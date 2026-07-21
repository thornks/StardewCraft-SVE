param(
    [Parameter(Mandatory = $true)]
    [string]$SveArchive
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Normalize-ObjectId([string]$id) {
    $path = $id -replace '^FlashShifter\.StardewValleyExpandedCP_', ''
    $path = $path -creplace '([a-z0-9])([A-Z])', '$1_$2'
    return (($path -replace '[^A-Za-z0-9]+', '_').Trim('_').ToLowerInvariant())
}

function Parse-Fish([string]$id, [string]$value) {
    $parts = $value -split '/'
    if ($parts.Count -lt 14) { throw "Unexpected Data/Fish value for ${id}: $value" }
    $times = @([regex]::Matches($parts[5], '\d+') | ForEach-Object { [int]$_.Value })
    $timeRanges = [System.Collections.ArrayList]::new()
    for ($i = 0; $i -lt $times.Count; $i += 2) {
        [void]$timeRanges.Add(@($times[$i], $times[$i + 1]))
    }
    return [pscustomobject]@{
        Id = $id
        Difficulty = if ($parts[1] -eq 'null') { $null } else { [int]$parts[1] }
        Behavior = $parts[2].ToLowerInvariant()
        MinFishSize = [int]$parts[3]
        MaxFishSize = [int]$parts[4]
        TimeRanges = @($timeRanges)
        Seasons = @(($parts[6].Trim() -split '\s+') | ForEach-Object { $_.ToLowerInvariant() })
        Weather = if ($parts[7].ToLowerInvariant() -eq 'both') { 'any' } else { $parts[7].ToLowerInvariant() }
        MaxDepth = [int]$parts[9]
        SpawnRate = [double]$parts[10]
        DepthMultiplier = [double]$parts[11]
        MinFishingLevel = [int]$parts[12]
        TutorialFish = [bool]::Parse($parts[13])
    }
}

function Set-Property($object, [string]$name, $value) {
    $object | Add-Member -NotePropertyName $name -NotePropertyValue $value -Force
}

$archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $SveArchive).Path)
try {
    $entry = $archive.Entries | Where-Object { $_.FullName -like '*code/Other/Fish.json' } |
        Select-Object -First 1
    if ($null -eq $entry) { throw 'Missing SVE code/Other/Fish.json' }
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try { $fishText = $reader.ReadToEnd() } finally { $reader.Dispose() }
} finally {
    $archive.Dispose()
}

$definitions = @{}
foreach ($block in [regex]::Matches(
    $fishText,
    '(?s)"Target"\s*:\s*"Data/Fish"\s*,\s*"Entries"\s*:\s*\{(?<entries>.*?)\}\s*(?:,\s*"When"|\s*\})')) {
    foreach ($fish in [regex]::Matches(
        $block.Groups['entries'].Value,
        '"(?<id>FlashShifter\.StardewValleyExpandedCP_[^"]+)"\s*:\s*"(?<value>[^"]+)"')) {
        $id = Normalize-ObjectId $fish.Groups['id'].Value
        $definitions[$id] = Parse-Fish $id $fish.Groups['value'].Value
    }
}

$motionTypes = @{ mixed = 0; dart = 1; smooth = 2; sinker = 3; floater = 4 }
$locationPath = 'src/main/resources/data/stardewcraftsve/fishing/locations/sve_fish.json'
$locationData = Get-Content $locationPath -Raw | ConvertFrom-Json
foreach ($fish in @($locationData.fish | Where-Object { $null -ne $_.id })) {
    $id = [string]$fish.id
    if (-not $definitions.ContainsKey($id)) { throw "No original Data/Fish entry for $id" }
    $definition = $definitions[$id]
    Set-Property $fish 'chance' 1.0
    Set-Property $fish 'difficulty' $definition.Difficulty
    Set-Property $fish 'motionType' $motionTypes[$definition.Behavior]
    Set-Property $fish 'minFishSize' $definition.MinFishSize
    Set-Property $fish 'maxFishSize' $definition.MaxFishSize
    Set-Property $fish 'minFishingLevel' $definition.MinFishingLevel
    Set-Property $fish 'seasons' $definition.Seasons
    Set-Property $fish 'weather' $definition.Weather
    Set-Property $fish 'timeRanges' $definition.TimeRanges
    Set-Property $fish 'maxDepth' $definition.MaxDepth
    Set-Property $fish 'spawnRate' $definition.SpawnRate
    Set-Property $fish 'depthMultiplier' $definition.DepthMultiplier
    if ($definition.TutorialFish) {
        Set-Property $fish 'isTutorialFish' $true
    } elseif ($null -ne $fish.PSObject.Properties['isTutorialFish']) {
        $fish.PSObject.Properties.Remove('isTutorialFish')
    }
}
[System.IO.File]::WriteAllText(
    (Join-Path (Get-Location) $locationPath),
    (($locationData | ConvertTo-Json -Depth 20) + [Environment]::NewLine),
    [System.Text.UTF8Encoding]::new($false))

$itemPath = 'src/main/java/com/stardew/craft/sve/ModItems.java'
$itemSource = Get-Content $itemPath -Raw
$itemPattern = '(?s)(DeferredHolder<Item,\s*FishItem>\s+\w+\s*=\s*ITEMS\.register\("(?<id>[^"]+)"' +
    '.*?new FishItem\(.*?\},\s*)\d+,\s*"[^"]+"'
$updatedItems = [regex]::Replace($itemSource, $itemPattern, {
    param($match)
    $id = $match.Groups['id'].Value
    if (-not $definitions.ContainsKey($id)) { return $match.Value }
    $definition = $definitions[$id]
    return $match.Groups[1].Value + $definition.Difficulty + ', "' + $definition.Behavior + '"'
})
[System.IO.File]::WriteAllText(
    (Join-Path (Get-Location) $itemPath),
    $updatedItems,
    [System.Text.UTF8Encoding]::new($false))

$reference = [ordered]@{}
foreach ($id in ($definitions.Keys | Sort-Object)) {
    $definition = $definitions[$id]
    $reference[$id] = [ordered]@{
        difficulty = $definition.Difficulty
        behavior = $definition.Behavior
        minFishSize = $definition.MinFishSize
        maxFishSize = $definition.MaxFishSize
        minFishingLevel = $definition.MinFishingLevel
        seasons = $definition.Seasons
        weather = $definition.Weather
        timeRanges = $definition.TimeRanges
        maxDepth = $definition.MaxDepth
        spawnRate = $definition.SpawnRate
        depthMultiplier = $definition.DepthMultiplier
        isTutorialFish = $definition.TutorialFish
    }
}
$referencePath = 'src/test/resources/sve_reference/fish_metadata.json'
[System.IO.Directory]::CreateDirectory((Split-Path $referencePath -Parent)) | Out-Null
[System.IO.File]::WriteAllText(
    (Join-Path (Get-Location) $referencePath),
    (($reference | ConvertTo-Json -Depth 20) + [Environment]::NewLine),
    [System.Text.UTF8Encoding]::new($false))

Write-Output "Synchronized $($locationData.fish.Count) catch rules, FishItem behavior, and reference metadata."
