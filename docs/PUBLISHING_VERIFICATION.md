# Publishing verification

Verified locally on 2026-09-25 using JDK 25, Gradle 9.5, and Mod Publish Plugin 2.2.0. These results apply to the replacement Gradle publisher; the previous Python publisher and its mocked-API tests have been removed.

- `gradlew.bat build publishMods` with `PUBLISH_DRY_RUN=true` passed. All 75 common tests passed, and both loader jars passed `verifyModJar`.
- All five plugin publishing tasks completed in dry-run mode: Fabric/NeoForge on CurseForge and Modrinth, plus one GitHub release with both jars.
- An additional local configuration check confirmed the exact Minecraft version, beta channel for `4.0.0-rc.1`, both CurseForge environments, correct per-loader dependencies, matching changelog text, exact jar names, and GitHub tag/commit settings. It uses only ignored build-directory files and is not another publishing subsystem.
- The preview artifacts contain the actual loader jars. No source, javadoc, common, or test-harness jar is selected for publication.
- Actionlint 1.7.12 passed for all three workflows. `git diff --check` passed.

Existing Java/Gradle warnings remain. No gameplay code changed, so Minecraft runtime scenarios were not repeated. No secrets were accessed, commits/tags created, branches pushed, or platform uploads performed during this verification.

## Remaining live checks

- [ ] Run the updated manual workflow on GitHub.
- [ ] Confirm tokens permit uploads and all platforms accept the metadata.
- [ ] Inspect the resulting files, dependencies, release notes, and CurseForge moderation status.

The dry run validates configuration and artifacts; it does not prove live API acceptance. Earlier failed publication attempts may have left a tag, draft, or uploaded file. Inspect those before reusing a version. See [Publishing](PUBLISHING.md) for the current procedure.
