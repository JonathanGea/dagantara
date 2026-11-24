$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$skillsRoot = Join-Path $repoRoot '.agents\skills'
$manifestPath = Join-Path $PSScriptRoot 'skills.json'
$publicSkillsPath = Join-Path $PSScriptRoot 'skills'
$indexPath = Join-Path $PSScriptRoot 'index.html'

function Set-OfflineBlock($html, $id, $content) {
    $pattern = '(?s)(<script id="' + [regex]::Escape($id) + '" type="[^"]+">).*?(</script>)'
    $block = [regex]::new($pattern)
    if (-not $block.IsMatch($html)) {
        throw "Could not find the $id data block in docs/index.html."
    }
    if ($content -match '</script\b') {
        throw "The $id data contains a closing script tag."
    }
    return $block.Replace($html, {
        param($match)
        $match.Groups[1].Value + [Environment]::NewLine +
            $content.TrimEnd() + [Environment]::NewLine + $match.Groups[2].Value
    }, 1)
}

New-Item -ItemType Directory -Path $publicSkillsPath -Force | Out-Null
$skills = @()
foreach ($directory in Get-ChildItem -LiteralPath $skillsRoot -Directory) {
    $skillPath = Join-Path $directory.FullName 'SKILL.md'
    if (-not (Test-Path -LiteralPath $skillPath -PathType Leaf)) { continue }

    $content = [System.IO.File]::ReadAllText($skillPath, [System.Text.Encoding]::UTF8)
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
        markdown = $content
    }

    Copy-Item -LiteralPath $skillPath -Destination (Join-Path $publicSkillsPath ($directory.Name + '.md')) -Force
}
$skills = @($skills | Sort-Object id)

$json = ConvertTo-Json -InputObject $skills -Depth 3
$utf8 = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText($manifestPath, $json + [Environment]::NewLine, $utf8)

$indexHtml = [System.IO.File]::ReadAllText($indexPath, $utf8)
$offlineJson = $json.Replace('<', '\u003c').Replace('>', '\u003e').Replace('&', '\u0026')
$indexHtml = Set-OfflineBlock $indexHtml 'offline-skills' $offlineJson
$readme = [System.IO.File]::ReadAllText((Join-Path $PSScriptRoot 'README.md'), $utf8)
$indexHtml = Set-OfflineBlock $indexHtml 'offline-readme' $readme
$skillGuide = [System.IO.File]::ReadAllText((Join-Path $PSScriptRoot 'skill-guide.md'), $utf8)
$indexHtml = Set-OfflineBlock $indexHtml 'offline-skill-guide' $skillGuide
foreach ($module in @('product-management', 'inventory-management', 'store-settings')) {
    $markdown = [System.IO.File]::ReadAllText((Join-Path $PSScriptRoot ($module + '.md')), $utf8)
    $indexHtml = Set-OfflineBlock $indexHtml ('offline-' + $module) $markdown
}
[System.IO.File]::WriteAllText($indexPath, $indexHtml, $utf8)

Write-Output "Generated manifest, $($skills.Count) public skill pages, and viewer fallback data."
