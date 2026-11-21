param(
    [ValidateRange(1, 65535)]
    [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..\..'))
$backend = Join-Path $repo 'backend'
$pomPath = Join-Path $backend 'pom.xml'
$wrapper = Join-Path $backend 'mvnw.cmd'

if (-not (Test-Path -LiteralPath $pomPath) -or -not (Test-Path -LiteralPath $wrapper)) {
    throw "Backend atau Maven Wrapper tidak ditemukan di $backend"
}

[xml]$pom = Get-Content -LiteralPath $pomPath -Raw
$requiredJava = [int]$pom.project.properties.'java.version'
$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    $javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
}
if (-not $javaHome) {
    throw 'JAVA_HOME harus menunjuk ke instalasi JDK yang sesuai dengan backend/pom.xml.'
}
$java = Join-Path $javaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $java)) {
    throw 'JAVA_HOME harus menunjuk ke instalasi JDK yang sesuai dengan backend/pom.xml.'
}

$javaVersion = @(& $java --version)[0]
if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch '^(?:openjdk|java) (\d+)') {
    throw "Tidak dapat membaca versi Java dari $java"
}
if ([int]$Matches[1] -ne $requiredJava) {
    throw "Backend memerlukan JDK $requiredJava; JAVA_HOME memakai $javaVersion"
}

function Get-PortOwner {
    $listener = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
        Where-Object { $_.OwningProcess -gt 0 } | Select-Object -First 1
    if ($listener) { return [int]$listener.OwningProcess }
    return $null
}

function Test-BackendHealth {
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/actuator/health" -TimeoutSec 2
        return $health.status -eq 'UP'
    } catch {
        return $false
    }
}

$healthy = Test-BackendHealth
$owner = Get-PortOwner
if ($healthy -and $owner) {
    $running = Get-CimInstance Win32_Process -Filter "ProcessId = $owner"
    $commandLine = [string]$running.CommandLine
    $fromJar = $commandLine -match [regex]::Escape($backend)
    $fromMaven = $commandLine -match 'com\.dagantara\.backend\.DagantaraApplication'
    if ($fromMaven) {
        $parent = Get-CimInstance Win32_Process -Filter "ProcessId = $($running.ParentProcessId)"
        $fromMaven = [string]$parent.CommandLine -match [regex]::Escape($backend)
    }
    if ($fromJar -or $fromMaven) {
        Write-Output "Backend sudah berjalan: PID $owner, http://localhost:$Port/actuator/health = UP"
        exit 0
    }
}
if ($healthy -or $owner) {
    throw "Port $Port sudah dipakai$(if ($owner) { " PID $owner" }). Backend tidak dijalankan agar proses lain tidak terganggu."
}

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"
Push-Location $backend
try {
    & $wrapper -B -ntp package
    if ($LASTEXITCODE -ne 0) { throw 'Build backend gagal.' }
} finally {
    Pop-Location
}

$jar = Join-Path $backend "target\$($pom.project.artifactId)-$($pom.project.version).jar"
if (-not (Test-Path -LiteralPath $jar)) { throw "JAR hasil build tidak ditemukan: $jar" }
$stdout = Join-Path $backend 'target\run-backend.stdout.log'
$stderr = Join-Path $backend 'target\run-backend.stderr.log'
$process = Start-Process -FilePath $java -ArgumentList "-jar `"$jar`" --server.port=$Port" -WorkingDirectory $backend -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WindowStyle Hidden -PassThru

for ($attempt = 0; $attempt -lt 60; $attempt++) {
    Start-Sleep -Milliseconds 500
    $process.Refresh()
    if ($process.HasExited) {
        throw "Backend berhenti saat startup. Periksa $stdout dan $stderr"
    }
    if ((Test-BackendHealth) -and (Get-PortOwner) -eq $process.Id) {
        Write-Output "Backend berjalan: PID $($process.Id), Java $requiredJava, http://localhost:$Port/actuator/health = UP"
        exit 0
    }
}

Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
throw "Backend tidak sehat dalam 30 detik. Periksa $stdout dan $stderr"
