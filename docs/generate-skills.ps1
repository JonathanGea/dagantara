$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$skillsRoot = Join-Path $repoRoot '.agents\skills'
$manifestPath = Join-Path $PSScriptRoot 'skills.json'
$publicSkillsPath = Join-Path $PSScriptRoot 'skills'
$indexPath = Join-Path $PSScriptRoot 'index.html'

New-Item -ItemType Directory -Path $publicSkillsPath -Force | Out-Null
$skills = @()
foreach ($directory in Get-ChildItem -LiteralPath $skillsRoot -Directory) {
    $skillPath = Join-Path $directory.FullName 'SKILL.md'
    if (-not (Test-Path -LiteralPath $skillPath -PathType Leaf)) { continue }

    $content = Get-Content -LiteralPath $skillPath -Raw -Encoding UTF8
    $titleMatch = [regex]::Match($content, '(?m)^#\s+(.+?)\s*$')
    $descriptionMatch = [regex]::Match($content, '(?m)^description:\s*(.+?)\s*$')
    $title = if ($titleMatch.Success) {
        $titleMatch.Groups[1].Value.Trim()
    } else {
        ($directory.Name -replace '-', ' ')
    }
    $description = if ($descriptionMatch.Success) {
        $descriptionMatch.Groups[1].Value.Trim().Trim('"', "'")
    } else {
        ''
    }

    $skills += [pscustomobject]@{
        id = $directory.Name
        title = $title
        description = $description
    }

    Copy-Item -LiteralPath $skillPath -Destination (Join-Path $publicSkillsPath ($directory.Name + '.md')) -Force
}
$skills = @($skills | Sort-Object id)

$json = ConvertTo-Json -InputObject $skills -Depth 3
$utf8 = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText($manifestPath, $json + [Environment]::NewLine, $utf8)

$indexHtml = [System.IO.File]::ReadAllText($indexPath, $utf8)
$offlineJson = $json.Replace('<', '\u003c').Replace('>', '\u003e').Replace('&', '\u0026')
$offlinePattern = '(?s)(<script id="offline-skills" type="application/json">).*?(</script>)'
$offlineRegex = [regex]::new($offlinePattern)
if (-not $offlineRegex.IsMatch($indexHtml)) {
    throw 'Could not find the offline-skills data block in docs/index.html.'
}
$replacement = '$1' + [Environment]::NewLine + $offlineJson + [Environment]::NewLine + '$2'
$indexHtml = $offlineRegex.Replace($indexHtml, $replacement, 1)
[System.IO.File]::WriteAllText($indexPath, $indexHtml, $utf8)

Write-Output "Generated manifest and $($skills.Count) public skill pages."
