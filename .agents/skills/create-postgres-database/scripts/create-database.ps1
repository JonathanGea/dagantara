#Requires -Version 5.1
param(
    [ValidateRange(1, 65535)]
    [int]$Port = 5433,
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9-]{0,47}$')]
    [string]$StateName = 'Dagantara',
    [string]$PgBin = (Join-Path $env:ProgramFiles 'PostgreSQL\16\bin')
)

$ErrorActionPreference = 'Stop'
$state = Join-Path $env:LOCALAPPDATA $StateName
$data = Join-Path $state 'postgresql-16'
$versionFile = Join-Path $data 'PG_VERSION'
$log = Join-Path $state 'postgresql.log'
$adminPath = Join-Path $state 'db-admin.credential.xml'
$appPath = Join-Path $state 'db-app.credential.xml'
$pgCtl = Join-Path $PgBin 'pg_ctl.exe'
$postgres = Join-Path $PgBin 'postgres.exe'
$initdb = Join-Path $PgBin 'initdb.exe'
$psql = Join-Path $PgBin 'psql.exe'

foreach ($binary in @($pgCtl, $postgres, $initdb, $psql)) {
    if (-not (Test-Path -LiteralPath $binary -PathType Leaf)) {
        throw "Binary PostgreSQL 16 tidak ditemukan di $binary. Gunakan -PgBin jika lokasinya berbeda."
    }
}
$pgVersion = & $postgres --version
if ($LASTEXITCODE -ne 0 -or $pgVersion -notmatch '\b16\.') {
    throw 'Skrip ini menggunakan binary PostgreSQL 16.'
}

function Get-DatabaseCredential([string]$Path, [string]$UserName, [bool]$AllowCreate) {
    if (Test-Path -LiteralPath $Path) {
        $credential = Import-Clixml -LiteralPath $Path
        if ($credential -isnot [pscredential] -or $credential.UserName -ne $UserName) {
            throw "Kredensial lokal tidak sesuai untuk akun $UserName."
        }
        return $credential
    }
    if (-not $AllowCreate) {
        throw "Kredensial $UserName tidak ditemukan. Pulihkan berkas $Path untuk memakai ulang instance."
    }

    $bytes = New-Object byte[] 32
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $random.GetBytes($bytes) } finally { $random.Dispose() }
    $password = [BitConverter]::ToString($bytes).Replace('-', '')
    $credential = [pscredential]::new($UserName, (ConvertTo-SecureString $password -AsPlainText -Force))
    $credential | Export-Clixml -LiteralPath $Path
    return $credential
}

function Invoke-LocalSql([pscredential]$Credential, [string]$Database, [string]$Sql) {
    $priorPassword = $env:PGPASSWORD
    try {
        $env:PGPASSWORD = $Credential.GetNetworkCredential().Password
        $result = $Sql | & $psql -X -qAt -w -v ON_ERROR_STOP=1 -h 127.0.0.1 -p $Port -U $Credential.UserName -d $Database 2>$null
        if ($LASTEXITCODE -ne 0) {
            throw "psql gagal pada database $Database sebagai $($Credential.UserName) di port $Port."
        }
        return $result
    } finally {
        $env:PGPASSWORD = $priorPassword
    }
}

New-Item -ItemType Directory -Path $state -Force | Out-Null
$lock = [IO.File]::Open((Join-Path $state 'bootstrap.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
try {
    $initialized = Test-Path -LiteralPath $versionFile -PathType Leaf
    $running = $false
    if ($initialized) {
        if ((Get-Content -LiteralPath $versionFile -Raw).Trim() -ne '16') {
            throw "Versi data PostgreSQL di $data bukan 16."
        }
        $configuredPort = & $postgres -D $data -C port
        if ($LASTEXITCODE -ne 0 -or $configuredPort -ne "$Port") {
            throw "Pilih port yang sesuai dengan konfigurasi instance di $data."
        }
        $listenAddresses = & $postgres -D $data -C listen_addresses
        if ($LASTEXITCODE -ne 0 -or $listenAddresses -ne '127.0.0.1') {
            throw "Konfigurasi instance di $data harus memakai 127.0.0.1."
        }
        & $pgCtl status -D $data | Out-Null
        $status = $LASTEXITCODE
        if ($status -notin @(0, 3)) { throw "Gagal membaca status PostgreSQL di $data." }
        $running = $status -eq 0
    }
    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    if ($running) {
        $postmasterPid = Get-Content -LiteralPath (Join-Path $data 'postmaster.pid') -TotalCount 1
        if (-not ($listener | Where-Object { $_.OwningProcess -eq $postmasterPid -and $_.LocalAddress -eq '127.0.0.1' })) {
            throw "Instance di $data tidak memiliki listener 127.0.0.1 pada port $Port."
        }
    } elseif ($listener) {
        throw "Port $Port sudah dipakai proses lain."
    }

    $admin = Get-DatabaseCredential $adminPath dagantara_admin (-not $initialized)
    $app = Get-DatabaseCredential $appPath dagantara_app (-not $initialized)
    if (-not $initialized) {
        $passwordFile = Join-Path $state ("initdb-" + [Guid]::NewGuid().ToString('N') + ".tmp")
        try {
            [IO.File]::WriteAllText($passwordFile, $admin.GetNetworkCredential().Password + [Environment]::NewLine, [Text.Encoding]::ASCII)
            & $initdb -D $data -U dagantara_admin --auth=scram-sha-256 --encoding=UTF8 --no-locale "--pwfile=$passwordFile" | Out-Host
            if ($LASTEXITCODE -ne 0) { throw 'initdb gagal.' }
        } finally {
            Remove-Item -LiteralPath $passwordFile -ErrorAction SilentlyContinue
        }
        Add-Content -LiteralPath (Join-Path $data 'postgresql.conf') -Encoding ascii -Value @('', "listen_addresses = '127.0.0.1'", "port = $Port")
    }
    if (-not $running) {
        # PostgreSQL can inherit the terminal pipe from pg_ctl on Windows.
        $arguments = @('start', '-D', ('"{0}"' -f $data), '-l', ('"{0}"' -f $log), '-w', '-t', '20')
        $launcher = Start-Process -FilePath $pgCtl -ArgumentList $arguments -WindowStyle Hidden -PassThru
        try {
            if (-not $launcher.WaitForExit(25000) -or $launcher.ExitCode -ne 0) {
                throw "Gagal menjalankan PostgreSQL. Periksa $log."
            }
        } finally {
            $launcher.Dispose()
        }
    }

    $priorAppPassword = $env:DAGANTARA_BOOTSTRAP_PASSWORD
    try {
        $env:DAGANTARA_BOOTSTRAP_PASSWORD = $app.GetNetworkCredential().Password
        Invoke-LocalSql $admin postgres @'
SET log_statement = 'none';
SET log_min_error_statement = 'panic';
\getenv app_password DAGANTARA_BOOTSTRAP_PASSWORD
SELECT format('CREATE ROLE dagantara_app LOGIN PASSWORD %L', :'app_password')
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'dagantara_app')
\gexec
SELECT 'CREATE DATABASE dagantara OWNER dagantara_app'
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_database WHERE datname = 'dagantara')
\gexec
'@ | Out-Null
    } finally {
        $env:DAGANTARA_BOOTSTRAP_PASSWORD = $priorAppPassword
    }

    $verified = Invoke-LocalSql $app dagantara @'
SELECT d.datdba = r.oid AND NOT (r.rolsuper OR r.rolcreatedb OR r.rolcreaterole OR r.rolreplication OR r.rolbypassrls)
FROM pg_catalog.pg_database d
JOIN pg_catalog.pg_roles r ON r.rolname = current_user
WHERE d.datname = current_database();
'@
    if ($verified -ne 't') { throw 'Akun aplikasi harus memiliki database dan memakai privilege aplikasi.' }

    [pscustomobject]@{
        Status         = 'UP'
        JdbcUrl        = "jdbc:postgresql://127.0.0.1:${Port}/dagantara"
        UserName       = $app.UserName
        CredentialPath = $appPath
        DataDirectory  = $data
    }
} finally {
    $lock.Dispose()
}
