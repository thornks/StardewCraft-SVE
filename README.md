# StardewCraftSVE

StardewCraftSVE is a non-commercial fan addon that adapts selected content and gameplay from Stardew Valley Expanded for [StarfieldPastoral (StardewCraft)](https://github.com/ChangQingElysium/Starfield-Pastoral) on NeoForge.

This is an independent compatibility project. It is not an official Stardew Valley Expanded project and is not affiliated with FlashShifter or the Stardew Valley Expanded team.

## Current Status

The addon is in active development. Its current focus is integrating non-map SVE content with StardewCraft's public data and gameplay systems while keeping behavior close to Stardew Valley Expanded.

It does not currently recreate the SVE world map or the complete SVE NPC, dialogue, event, and story experience.

## Platform

- Project name: StardewCraftSVE
- Minecraft 1.21.1
- NeoForge 21.1.226 or later
- Java 21
- Addon version: 0.1.0
- Mod id: `stardewcraftsve`
- Required StardewCraft version: 0.5.1fix4

## Implemented or In-Progress Systems

- SVE items and shared StardewCraft metadata, quality, collection, museum, shop, and acquisition integration;
- crops, seasonal forage, artifact spots, cooking content, and community center bundles;
- fish, location and season rules, fish roe, fish ponds, and JEI fishing information;
- artisan machines including the butter churner and yarn spooler;
- pear, nectarine, and persimmon fruit trees, plus fir and birch trees and their growth stages;
- goose and camel farm animals, animal products, purchasing, incubation, and management integration;
- StardewCraft collection-page and mail content integration;
- JEI and Jade compatibility for supported SVE content.

## Requirements

- [StarfieldPastoral (StardewCraft)](https://github.com/ChangQingElysium/Starfield-Pastoral) 0.5.1fix4
- GeckoLib 4.7.7
- JEI 19.27.0.340 for JEI integration
- Jade 15.10.5 for Jade integration

## Repository Scope

This repository contains the addon project itself:

- Java source and gameplay resources under `src`;
- Gradle build files and wrapper files;
- essential repository metadata.

Third-party dependency JARs, extracted reference material, decompiled sources, scratch assets, logs, and temporary development files are not intended for redistribution through this repository.

## Development

Before building, provide the local dependencies expected by `build.gradle`:

```text
../stardewcraft-0.5.1fix4.jar
libs/geckolib-neoforge-1.21.1-4.7.7.jar
libs/jade-1.21.1-NeoForge-15.10.5.jar
libs/jei-1.21.1-neoforge-19.27.0.340.jar
```

Common validation commands:

```powershell
.\gradlew.bat check --no-daemon
.\gradlew.bat build --no-daemon
```

The built addon is written to `build/libs/stardewcraftsve-0.1.0.jar`.

## Credits and Asset Notice

- Stardew Valley Expanded was created by FlashShifter and the Stardew Valley Expanded team.
- StarfieldPastoral (StardewCraft) was created by ChangQingElysium and its contributors.

This repository is maintained as a non-commercial fan project. Stardew Valley, Stardew Valley Expanded, and other third-party assets remain the property of their respective rightsholders and are not relicensed by this repository.
