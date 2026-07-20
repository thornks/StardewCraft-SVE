# StardewCraft SVE

Stardew Valley Expanded content addon for StardewCraft on Minecraft 1.21.1 and NeoForge.

## Requirements

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.226 or later
- StardewCraft 0.5.1fix4
- GeckoLib 4.7.7
- JEI 19.27.0.340 and Jade 15.10.5 for their respective integrations

## Local dependencies

Third-party and StardewCraft JAR files are intentionally excluded from this repository. Before building, place these files at the paths expected by `build.gradle`:

```text
../stardewcraft-0.5.1fix4.jar
libs/geckolib-neoforge-1.21.1-4.7.7.jar
libs/jade-1.21.1-NeoForge-15.10.5.jar
libs/jei-1.21.1-neoforge-19.27.0.340.jar
```

## Build

On Windows:

```powershell
.\gradlew.bat animalRegressionTest --no-daemon
.\gradlew.bat bundleRegressionTest --no-daemon
.\gradlew.bat jar --no-daemon
```

The built mod is written to `build/libs/stardewcraftsve-0.1.0.jar`.

## Credits

Stardew Valley Expanded content and textures are credited to FlashShifter and the Stardew Valley Expanded project.
