param(
    [Parameter(Mandatory = $true)]
    [string]$SveArchive,
    [switch]$IncludeDescriptions
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Read-ZipText([System.IO.Compression.ZipArchive]$archive, [string]$path) {
    $entry = $archive.Entries | Where-Object { $_.FullName -eq $path }
    if ($null -eq $entry) {
        throw "Missing archive entry: $path"
    }
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try {
        return $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
    }
}

function ConvertFrom-Jsonc([string]$text) {
    $clean = [regex]::Replace($text, '/\*[\s\S]*?\*/', '')
    $clean = [regex]::Replace($clean, '(?m)^\s*//.*$', '')
    do {
        $next = [regex]::Replace($clean, ',(?=\s*[}\]])', '')
        $changed = $next -ne $clean
        $clean = $next
    } while ($changed)
    return $clean | ConvertFrom-Json
}

function Normalize-ObjectId([string]$id) {
    $path = $id -replace '^FlashShifter\.StardewValleyExpandedCP_', ''
    $path = $path -creplace '([a-z0-9])([A-Z])', '$1_$2'
    return (($path -replace '[^A-Za-z0-9]+', '_').Trim('_').ToLowerInvariant())
}

function Read-CatalogList([string]$source, [string]$name) {
    $match = [regex]::Match(
        $source,
        '(?s)' + [regex]::Escape($name) + '\s*=\s*List\.of\((?<body>.*?)\);')
    if (-not $match.Success) {
        throw "Missing collection catalog list: $name"
    }
    return @([regex]::Matches($match.Groups['body'].Value, '"([^"]+)"') |
        ForEach-Object { $_.Groups[1].Value })
}

function Add-Finding(
    [System.Collections.Generic.List[object]]$findings,
    [string]$kind,
    [string]$id,
    [string]$detail
) {
    $findings.Add([pscustomobject]@{ Kind = $kind; Id = $id; Detail = $detail })
}

$archivePath = (Resolve-Path -LiteralPath $SveArchive).Path
$archive = [System.IO.Compression.ZipFile]::OpenRead($archivePath)
try {
    $objectsDocument = ConvertFrom-Jsonc (Read-ZipText $archive `
        'Stardew Valley Expanded/[CP] Stardew Valley Expanded/code/Items/Objects.json')
    $i18nText = Read-ZipText $archive `
        'Stardew Valley Expanded/[CP] Stardew Valley Expanded/i18n/default.json'
} finally {
    $archive.Dispose()
}

$objects = @{}
foreach ($change in @($objectsDocument.Changes | Where-Object { $_.Target -eq 'Data/Objects' })) {
    foreach ($property in $change.Entries.PSObject.Properties) {
        $objects[(Normalize-ObjectId $property.Name)] = $property.Value
    }
}
$i18n = @{}
foreach ($match in [regex]::Matches(
    $i18nText,
    '(?m)^\s*"(?<key>object\.[^"]+\.description)"\s*:\s*"(?<value>(?:\\.|[^"\\])*)"\s*,?')) {
    $encoded = '"' + $match.Groups['value'].Value + '"'
    $i18n[$match.Groups['key'].Value] = [string]($encoded | ConvertFrom-Json)
}

$itemSource = Get-Content 'src/main/java/com/stardew/craft/sve/ModItems.java' -Raw
$catalogSource = Get-Content `
    'src/main/java/com/stardew/craft/sve/collection/SveCollectionCatalog.java' -Raw
$currentLanguage = Get-Content `
    'src/main/resources/assets/stardewcraftsve/lang/en_us.json' -Raw | ConvertFrom-Json
$findings = [System.Collections.Generic.List[object]]::new()

$qualityPattern = '(?s)public static final DeferredHolder<Item,\s*StardewQualityItem>\s+\w+' +
    '\s*=\s*ITEMS\.register\("(?<id>[^"]+)",\s*\(\)\s*->\s*new StardewQualityItem\(' +
    '\s*"(?<type>[^"]+)",\s*(?<price>-?\d+),\s*(?<edibility>-?\d+)'
$simplePattern = '(?s)public static final DeferredHolder<Item,\s*SimpleStardewItem>\s+\w+' +
    '\s*=\s*ITEMS\.register\("(?<id>[^"]+)",\s*\(\)\s*->\s*new SimpleStardewItem\(' +
    '\s*"(?<type>[^"]+)",\s*(?<price>-?\d+)'

foreach ($match in [regex]::Matches($itemSource, $qualityPattern)) {
    $id = $match.Groups['id'].Value
    if (-not $objects.ContainsKey($id)) { continue }
    $original = $objects[$id]
    $price = [int]$match.Groups['price'].Value
    $edibility = [int]$match.Groups['edibility'].Value
    if ($price -ne [int]$original.Price) {
        Add-Finding $findings 'price' $id "$price != $($original.Price)"
    }
    if ($edibility -ne [int]$original.Edibility) {
        Add-Finding $findings 'edibility' $id "$edibility != $($original.Edibility)"
    }
}
foreach ($match in [regex]::Matches($itemSource, $simplePattern)) {
    $id = $match.Groups['id'].Value
    if (-not $objects.ContainsKey($id)) { continue }
    $original = $objects[$id]
    $price = [int]$match.Groups['price'].Value
    if ($price -ne [int]$original.Price) {
        Add-Finding $findings 'price' $id "$price != $($original.Price)"
    }
    if ([int]$original.Edibility -ne -300) {
        Add-Finding $findings 'edibility' $id "plain item != $($original.Edibility)"
    }
}

$fishPattern = '(?s)public static final DeferredHolder<Item,\s*FishItem>\s+\w+' +
    '\s*=\s*ITEMS\.register\("(?<id>[^"]+)",\s*\(\)\s*->\s*new FishItem\(' +
    '\s*new int\[\]\s*\{(?<prices>[^}]+)\},\s*new int\[\]\s*\{(?<energy>[^}]+)\}'
foreach ($match in [regex]::Matches($itemSource, $fishPattern)) {
    $id = $match.Groups['id'].Value
    if (-not $objects.ContainsKey($id)) { continue }
    $original = $objects[$id]
    $prices = @($match.Groups['prices'].Value -split ',' | ForEach-Object { [int]$_.Trim() })
    $energy = @($match.Groups['energy'].Value -split ',' | ForEach-Object { [int]$_.Trim() })
    $expectedEnergy = [int][math]::Ceiling(([int]$original.Edibility) * 2.5)
    if ($prices[0] -ne [int]$original.Price) {
        Add-Finding $findings 'fish-price' $id "$($prices[0]) != $($original.Price)"
    }
    if ($energy[0] -ne $expectedEnergy) {
        Add-Finding $findings 'fish-energy' $id "$($energy[0]) != $expectedEnergy"
    }
}

if ($IncludeDescriptions) {
    foreach ($id in ($objects.Keys | Sort-Object)) {
        $description = [string]$objects[$id].Description
        if ($description -notmatch '\{\{i18n:([^}]+)\}\}') { continue }
        $originalDescription = $i18n[$matches[1]]
        if ($null -eq $originalDescription) { continue }
        $currentKey = "item.stardewcraftsve.$id.desc"
        $currentProperty = $currentLanguage.PSObject.Properties[$currentKey]
        if ($null -eq $currentProperty) { continue }
        if ([string]$currentProperty.Value -ne $originalDescription) {
            Add-Finding $findings 'description' $id `
                "current='$($currentProperty.Value)' original='$originalDescription'"
        }
    }
}

$collectionRules = @(
    [pscustomobject]@{
        Name = 'FISH'
        Expected = @($objects.Keys | Where-Object {
            $objects[$_].Type -eq 'Fish' -and
            $objects[$_].ExcludeFromFishingCollection -eq $false
        })
    },
    [pscustomobject]@{
        Name = 'ARTIFACTS'
        Expected = @($objects.Keys | Where-Object { $objects[$_].Type -eq 'Arch' })
    },
    [pscustomobject]@{
        Name = 'MINERALS'
        Expected = @($objects.Keys | Where-Object { $objects[$_].Type -eq 'Minerals' })
    }
)
foreach ($rule in $collectionRules) {
    $current = Read-CatalogList $catalogSource $rule.Name
    foreach ($id in $rule.Expected) {
        if ($id -notin $current) {
            Add-Finding $findings "collection-$($rule.Name.ToLowerInvariant())" $id 'missing'
        }
    }
    foreach ($id in $current) {
        if ($id -notin $rule.Expected) {
            Add-Finding $findings "collection-$($rule.Name.ToLowerInvariant())" $id 'extra'
        }
    }
}

$shippingCategories = @(-5, -17, -18, -23, -26, -27, -28, -75, -79, -80, -81)
$expectedShipping = @($objects.Keys | Where-Object {
    $objects[$_].ExcludeFromShippingCollection -eq $false -and
    [int]$objects[$_].Category -in $shippingCategories
})
$currentShipping = Read-CatalogList $catalogSource 'SHIPPING'
foreach ($id in $expectedShipping) {
    if ($id -notin $currentShipping) {
        Add-Finding $findings 'collection-shipping' $id 'missing'
    }
}
foreach ($id in $currentShipping) {
    if ($id -notin $expectedShipping) {
        Add-Finding $findings 'collection-shipping' $id 'extra'
    }
}

Write-Output "SVE objects: $($objects.Count)"
Write-Output "Findings: $($findings.Count)"
$findings | Sort-Object Kind, Id | Format-Table -AutoSize
if ($findings.Count -gt 0) { exit 1 }
