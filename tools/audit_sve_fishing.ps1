param(
    [Parameter(Mandatory = $true)]
    [string]$SveArchive,
    [switch]$ShowLocations
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Read-ZipText([System.IO.Compression.ZipArchive]$archive, [string]$pattern) {
    $entry = $archive.Entries | Where-Object { $_.FullName -like $pattern } | Select-Object -First 1
    if ($null -eq $entry) { throw "Missing archive entry matching: $pattern" }
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}

function Normalize-ObjectId([string]$id) {
    $path = $id -replace '^FlashShifter\.StardewValleyExpandedCP_', ''
    $path = $path -creplace '([a-z0-9])([A-Z])', '$1_$2'
    return (($path -replace '[^A-Za-z0-9]+', '_').Trim('_').ToLowerInvariant())
}

function ConvertFrom-Jsonc([string]$text) {
    $builder = [System.Text.StringBuilder]::new($text.Length)
    $inString = $false
    $escaped = $false
    $lineComment = $false
    $blockComment = $false
    for ($i = 0; $i -lt $text.Length; $i++) {
        $char = $text[$i]
        $next = if ($i + 1 -lt $text.Length) { $text[$i + 1] } else { [char]0 }
        if ($lineComment) {
            if ($char -eq "`r" -or $char -eq "`n") {
                $lineComment = $false
                [void]$builder.Append($char)
            }
            continue
        }
        if ($blockComment) {
            if ($char -eq '/' -and $i -gt 0 -and $text[$i - 1] -eq '*') { $blockComment = $false }
            continue
        }
        if ($inString) {
            [void]$builder.Append($char)
            if ($escaped) { $escaped = $false }
            elseif ($char -eq '\') { $escaped = $true }
            elseif ($char -eq '"') { $inString = $false }
            continue
        }
        if ($char -eq '"') {
            $inString = $true
            [void]$builder.Append($char)
        } elseif ($char -eq '/' -and $next -eq '/') {
            $lineComment = $true
            $i++
        } elseif ($char -eq '/' -and $next -eq '*') {
            $blockComment = $true
            $i++
        } else {
            [void]$builder.Append($char)
        }
    }
    $clean = $builder.ToString()
    do {
        $nextText = [regex]::Replace($clean, ',(?=\s*[}\]])', '')
        $changed = $nextText -ne $clean
        $clean = $nextText
    } while ($changed)
    return $clean | ConvertFrom-Json
}

function Parse-Fish([string]$id, [string]$value) {
    $parts = $value -split '/'
    if ($parts.Count -lt 14) { throw "Unexpected Data/Fish value for ${id}: $value" }
    $times = @([regex]::Matches($parts[5], '\d+') | ForEach-Object { [int]$_.Value })
    if ($times.Count % 2 -ne 0) { throw "Unpaired time values for ${id}: $($parts[5])" }
    $timeRanges = for ($i = 0; $i -lt $times.Count; $i += 2) { "$($times[$i])-$($times[$i + 1])" }
    $weather = if ($parts[7].ToLowerInvariant() -eq 'both') { 'any' } else { $parts[7].ToLowerInvariant() }
    $difficulty = if ($parts[1] -eq 'null') { $null } else { [int]$parts[1] }
    return [pscustomobject]@{
        Id = $id
        Difficulty = $difficulty
        Behavior = $parts[2].ToLowerInvariant()
        MinFishSize = [int]$parts[3]
        MaxFishSize = [int]$parts[4]
        TimeRanges = @($timeRanges)
        Seasons = @(($parts[6].Trim() -split '\s+') | ForEach-Object { $_.ToLowerInvariant() })
        Weather = $weather
        MaxDepth = [int]$parts[9]
        SpawnRate = [double]$parts[10]
        DepthMultiplier = [double]$parts[11]
        MinFishingLevel = [int]$parts[12]
        TutorialFish = [bool]::Parse($parts[13])
    }
}

function Canonical-List($values) {
    return (@($values) | ForEach-Object { [string]$_ } | Sort-Object) -join ','
}

function Add-Finding(
    [System.Collections.Generic.List[object]]$findings,
    [string]$scope,
    [string]$id,
    [string]$field,
    $current,
    $expected
) {
    $findings.Add([pscustomobject]@{
        Scope = $scope
        Id = $id
        Field = $field
        Current = if ($null -eq $current) { '<missing>' } else { [string]$current }
        Expected = [string]$expected
    })
}

$archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $SveArchive).Path)
try {
    $fishText = Read-ZipText $archive '*code/Other/Fish.json'
    $locationDocument = ConvertFrom-Jsonc (Read-ZipText $archive '*code/Locations/LocationsData.json')
} finally {
    $archive.Dispose()
}

$original = @{}
foreach ($block in [regex]::Matches(
    $fishText,
    '(?s)"Target"\s*:\s*"Data/Fish"\s*,\s*"Entries"\s*:\s*\{(?<entries>.*?)\}\s*(?:,\s*"When"|\s*\})')) {
    foreach ($entry in [regex]::Matches(
        $block.Groups['entries'].Value,
        '"(?<id>FlashShifter\.StardewValleyExpandedCP_[^"]+)"\s*:\s*"(?<value>[^"]+)"')) {
        $id = Normalize-ObjectId $entry.Groups['id'].Value
        $original[$id] = Parse-Fish $id $entry.Groups['value'].Value
    }
}

$originalLocations = @{}
function Add-OriginalLocation([string]$locationName, $fish) {
    $itemProperty = $fish.PSObject.Properties['ItemId']
    if ($null -eq $itemProperty) { return }
    $match = [regex]::Match(
        [string]$itemProperty.Value,
        'FlashShifter\.StardewValleyExpandedCP_(?<id>[A-Za-z0-9_]+)$')
    if (-not $match.Success) { return }
    $id = Normalize-ObjectId ('FlashShifter.StardewValleyExpandedCP_' + $match.Groups['id'].Value)
    if (-not $originalLocations.ContainsKey($id)) {
        $originalLocations[$id] = [System.Collections.Generic.List[string]]::new()
    }
    $originalLocations[$id].Add($locationName)
}
foreach ($change in @($locationDocument.Changes | Where-Object { $_.Target -eq 'Data/Locations' })) {
    $targetField = $change.PSObject.Properties['TargetField']
    if ($null -ne $targetField -and @($targetField.Value).Count -ge 2 -and
            @($targetField.Value)[1] -eq 'Fish') {
        $entriesProperty = $change.PSObject.Properties['Entries']
        if ($null -ne $entriesProperty) {
            foreach ($fish in $entriesProperty.Value.PSObject.Properties) {
                Add-OriginalLocation ([string]@($targetField.Value)[0]) $fish.Value
            }
        }
        continue
    }
    foreach ($location in $change.Entries.PSObject.Properties) {
        $fishProperty = $location.Value.PSObject.Properties['Fish']
        if ($null -eq $fishProperty) { continue }
        foreach ($fish in @($fishProperty.Value)) {
            Add-OriginalLocation $location.Name $fish
        }
    }
}

$currentDocument = Get-Content `
    'src/main/resources/data/stardewcraftsve/fishing/locations/sve_fish.json' -Raw | ConvertFrom-Json
$current = @{}
foreach ($fish in @($currentDocument.fish | Where-Object { $null -ne $_.id })) {
    $current[[string]$fish.id] = $fish
}

$motionTypes = @{ mixed = 0; dart = 1; smooth = 2; sinker = 3; floater = 4 }
$findings = [System.Collections.Generic.List[object]]::new()
$fields = @('Difficulty', 'MinFishSize', 'MaxFishSize', 'MinFishingLevel', 'Weather',
    'MaxDepth', 'SpawnRate', 'DepthMultiplier')
foreach ($id in ($current.Keys | Sort-Object)) {
    if (-not $original.ContainsKey($id)) {
        Add-Finding $findings 'location' $id 'entry' 'extra' 'present in original Data/Fish'
        continue
    }
    $expected = $original[$id]
    $actual = $current[$id]
    if ([double]$actual.chance -ne 1.0) {
        Add-Finding $findings 'location' $id 'chance' $actual.chance 1.0
    }
    foreach ($field in $fields) {
        $jsonField = $field.Substring(0, 1).ToLowerInvariant() + $field.Substring(1)
        $property = $actual.PSObject.Properties[$jsonField]
        $actualValue = if ($null -eq $property) { $null } else { $property.Value }
        $expectedValue = $expected.$field
        if ($null -eq $actualValue -or [string]$actualValue -ne [string]$expectedValue) {
            Add-Finding $findings 'location' $id $jsonField $actualValue $expectedValue
        }
    }
    $actualMotion = $actual.PSObject.Properties['motionType']
    $actualMotionValue = if ($null -eq $actualMotion) { $null } else { $actualMotion.Value }
    if ($null -eq $actualMotionValue -or [int]$actualMotionValue -ne $motionTypes[$expected.Behavior]) {
        Add-Finding $findings 'location' $id 'motionType' $actualMotionValue `
            "$($motionTypes[$expected.Behavior]) ($($expected.Behavior))"
    }
    $actualTimes = @($actual.timeRanges | ForEach-Object { "$(($_ | Select-Object -First 1))-$(($_ | Select-Object -Last 1))" })
    if ((Canonical-List $actualTimes) -ne (Canonical-List $expected.TimeRanges)) {
        Add-Finding $findings 'location' $id 'timeRanges' ($actualTimes -join ',') ($expected.TimeRanges -join ',')
    }
    if ((Canonical-List $actual.seasons) -ne (Canonical-List $expected.Seasons)) {
        Add-Finding $findings 'location' $id 'seasons' ($actual.seasons -join ',') ($expected.Seasons -join ',')
    }
}

$itemSource = Get-Content 'src/main/java/com/stardew/craft/sve/ModItems.java' -Raw
$itemPattern = '(?s)DeferredHolder<Item,\s*FishItem>\s+\w+\s*=\s*ITEMS\.register\("(?<id>[^"]+)"' +
    '.*?new FishItem\(.*?\},\s*(?<difficulty>\d+),\s*"(?<behavior>[^"]+)"'
foreach ($match in [regex]::Matches($itemSource, $itemPattern)) {
    $id = $match.Groups['id'].Value
    if (-not $original.ContainsKey($id)) { continue }
    $expected = $original[$id]
    $difficulty = [int]$match.Groups['difficulty'].Value
    $behavior = $match.Groups['behavior'].Value.ToLowerInvariant()
    if ($difficulty -ne $expected.Difficulty) {
        Add-Finding $findings 'item' $id 'difficulty' $difficulty $expected.Difficulty
    }
    if ($behavior -ne $expected.Behavior) {
        Add-Finding $findings 'item' $id 'behavior' $behavior $expected.Behavior
    }
}

Write-Output "Original Data/Fish entries: $($original.Count)"
Write-Output "Current catch rules: $($current.Count)"
Write-Output "Findings: $($findings.Count)"
$findings | Sort-Object Scope, Id, Field | Format-Table -AutoSize
if ($ShowLocations) {
    $locationRows = foreach ($id in ($current.Keys | Sort-Object)) {
        $fish = $current[$id]
        $biomes = $fish.PSObject.Properties['biomes']
        $biomeTags = $fish.PSObject.Properties['biomeTags']
        [pscustomobject]@{
            Id = $id
            OriginalLocations = (@($originalLocations[$id]) | Sort-Object -Unique) -join ', '
            CurrentTargets = (@($(if ($null -ne $biomes) { $biomes.Value })) +
                @($(if ($null -ne $biomeTags) { $biomeTags.Value }))) -join ', '
        }
    }
    foreach ($row in $locationRows) {
        Write-Output "$($row.Id)`toriginal=$($row.OriginalLocations)`tcurrent=$($row.CurrentTargets)"
    }
}
if ($findings.Count -gt 0) { exit 1 }
