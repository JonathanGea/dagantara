$ErrorActionPreference = 'Stop'

$docsRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $docsRoot
$skillsRoot = Join-Path $repoRoot '.agents\skills'
$indexPath = Join-Path $docsRoot 'index.html'

function Set-OfflineBlock($html, $id, $content) {
    $pattern = '(?s)(<script id="' + [regex]::Escape($id) + '" type="[^"]+"(?: data-doc="[^"]+")?>).*?(</script>)'
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

$utf8 = [System.Text.UTF8Encoding]::new($false)
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

    # The viewer resolves skill links from their legacy Markdown paths.
    $embeddedContent = $content -replace '(?<=\]\()\.\./([\w-]+)/SKILL\.md(?=[#)])', '../../index.html?skill=$1'
    # Keep the embedded JSON stable across checkouts with different line endings.
    $skills += [pscustomobject]@{
        id = $directory.Name
        title = $title
        description = $description
        markdown = $embeddedContent -replace '\r?\n', "`r`n"
    }
}
$skills = @($skills | Sort-Object id)

$json = ConvertTo-Json -InputObject $skills -Depth 3

$indexHtml = [System.IO.File]::ReadAllText($indexPath, $utf8)
$offlineJson = $json.Replace('<', '\u003c').Replace('>', '\u003e').Replace('&', '\u0026')
$indexHtml = Set-OfflineBlock $indexHtml 'offline-skills' $offlineJson
$documentBlocks = [regex]::Matches($indexHtml, '<script id="(?<id>offline-[\w-]+)" type="text/(?:plain|markdown)" data-doc="(?<path>(?:[\w-]+/)*[\w-]+\.md)">')
foreach ($block in $documentBlocks) {
    $markdown = [System.IO.File]::ReadAllText((Join-Path $docsRoot $block.Groups['path'].Value), $utf8)
    $indexHtml = Set-OfflineBlock $indexHtml $block.Groups['id'].Value $markdown
}
[System.IO.File]::WriteAllText($indexPath, ($indexHtml -replace '\r?\n', [Environment]::NewLine), $utf8)

Write-Output "Embedded $($skills.Count) skills and Markdown fallback data in docs/index.html."
