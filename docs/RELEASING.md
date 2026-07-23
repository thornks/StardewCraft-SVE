# Reproducible Releases

The release pipeline builds StardewCraftSVE from pinned inputs and refuses to publish a JAR unless a second clean build is byte-for-byte identical.

## Pinned Inputs

The authoritative versions are stored in `gradle.properties`:

- `mod_version` controls the archive name and the version embedded in `neoforge.mods.toml`;
- `stardewcraft_source_commit` pins the exact Starfield Pastoral source used as the compile dependency;
- NeoForge, JEI, GeckoLib, and Jade use fixed versions;
- Gradle dependency verification records SHA-256 values for resolved Maven artifacts;
- the Gradle wrapper pins both Gradle 8.9 and its distribution SHA-256;
- CI and release jobs use Temurin Java `21.0.8+9` on `ubuntu-24.04`.
- GitHub Actions are pinned to immutable commit SHAs rather than moving major-version tags.

Archive entry order, file timestamps, and file permissions are normalized by `build.gradle`. Repository text files are checked out with LF line endings on every operating system through `.gitattributes`.

## Local Verification

With the official `stardewcraft-0.5.2.jar` in the parent directory:

```powershell
.\gradlew.bat clean releaseBundle --no-daemon
```

The default local JAR is checked against the pinned SHA-256 before compilation. A StardewCraft JAR built from the pinned source commit can instead be supplied explicitly:

```powershell
.\gradlew.bat clean releaseBundle --no-daemon `
  -Pstardewcraft_jar=C:\path\to\stardewcraft-0.5.2.jar
```

Release files are written to `build/release`:

- `stardewcraftsve-<version>.jar`;
- `SHA256SUMS`;
- `BUILD-INFO.properties`.

On Linux or Git Bash, `tools/prepare_stardewcraft.sh` clones and builds the pinned StardewCraft source automatically.

## Publishing

1. Update `mod_version` in `gradle.properties`.
2. Commit and push the release changes to `master`.
3. Wait for the `CI` workflow to pass.
4. Create and push the matching annotated tag, for example:

```bash
git tag -a v0.2.0 -m "StardewCraftSVE 0.2.0"
git push origin v0.2.0
```

The `Release` workflow then:

1. rejects a tag that does not match `mod_version`;
2. builds StardewCraft from the pinned source commit;
3. runs the complete SVE regression suite;
4. creates the release bundle;
5. performs a second clean build and compares both JARs byte for byte;
6. verifies `SHA256SUMS`;
7. uploads the bundle, creates a GitHub artifact attestation, and publishes the GitHub Release.

Never replace an existing release asset in place. Increment `mod_version` and create a new tag so every published version remains traceable to one immutable source revision.
