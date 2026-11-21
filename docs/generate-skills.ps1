$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$skillsRoot = Join-Path $repoRoot '.agents\skills'
$manifestPath = Join-Path $PSScriptRoot 'skills.json'

$skills = @(
    Get-ChildItem -LiteralPath $skillsRoot -Directory | ForEach-Object {
        $skillPath = Join-Path $_.FullName 'SKILL.md'
        if (-not (Test-Path -LiteralPath $skillPath -PathType Leaf)) { return }

        $content = Get-Content -LiteralPath $skillPath -Raw -Encoding UTF8
        $titleMatch = [regex]::Match($content, '(?m)^#\s+(.+?)\s*$')
        $descriptionMatch = [regex]::Match($content, '(?m)^description:\s*(.+?)\s*$')
        $title = if ($titleMatch.Success) {
            $titleMatch.Groups[1].Value.Trim()
        } else {
            ($_.Name -replace '-', ' ')
        }
        $description = if ($descriptionMatch.Success) {
            $descriptionMatch.Groups[1].Value.Trim().Trim('"', "'")
        } else {
            ''
        }

        [pscustomobject]@{
            id = $_.Name
            title = $title
            description = $description
        }
    } | Sort-Object id
)

$json = ConvertTo-Json -InputObject $skills -Depth 3
[System.IO.File]::WriteAllText(
    $manifestPath,
    $json + [Environment]::NewLine,
    [System.Text.UTF8Encoding]::new($false)
)
Write-Output "Generated $manifestPath with $($skills.Count) skills."
