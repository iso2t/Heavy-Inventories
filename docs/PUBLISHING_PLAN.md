# Publishing checklist

The custom Python publisher has been replaced by Gradle's Mod Publish Plugin. The [setup and release guide](PUBLISHING.md) is the current procedure.

- [x] Keep publication manual-only, with dry-run selected by default.
- [x] Read the version from `gradle.properties` and notes from `changelogs/<version>.md`.
- [x] Build/test both loaders before any upload.
- [x] Publish separate Fabric and NeoForge files to CurseForge and Modrinth.
- [x] Declare CurseForge client/server support and each loader's dependencies.
- [x] Create the GitHub version tag and release with both jars.
- [x] Accept the existing repository secret names.
- [x] Remove custom preflight, bundle, ledger, and recovery machinery.
- [ ] Verify the replacement workflow with a GitHub-hosted run.
- [ ] Confirm a live upload and resulting metadata on all three platforms.

Local verification is recorded in [PUBLISHING_VERIFICATION.md](PUBLISHING_VERIFICATION.md). The workflow's ability to publish does not authorize the assistant to commit, tag, push, or publish.
