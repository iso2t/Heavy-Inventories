# Publishing implementation verification

Verified locally on 2026-09-25 for mod version `4.0.0-rc.1`, Minecraft `26.1`, and Java `25`. These checks cover the local working tree. No commit, tag, push, repository setting change, or platform upload was performed.

## Passing checks

- `python -m unittest discover -s scripts/release/tests -v`: 32 tests passed. Parameterized cases include failures before/after all nine ledger writes, all five draft bundle asset uploads, and lost responses for each of four platform uploads. Tests use fake APIs; their successful-publication paths do not publish anything.
- `python scripts/release/release.py validate`: accepted the current version/changelog and rejected invalid fixture versions, tags, notes, paths, and destination IDs.
- `gradlew.bat clean build releaseArtifacts --no-daemon --max-workers=2 --console=plain`: passed; all 75 common tests passed, both loader jars passed packaged-resource checks, and Gradle reported the exact jar paths.
- Both `lifecycleTestClasses` tasks compiled with `gradle/lifecycle-smoke.gradle`.
- actionlint `1.7.12`: passed for the build, reusable build, and publishing workflows. The downloaded tool archive was checked against the upstream SHA-256 checksum. Python/Gradle tests remain part of CI; actionlint was a local implementation check.
- Packaging and preview succeeded for the actual local Fabric/NeoForge jars. Preview used no publishing credentials or network requests and produced four loader/platform payloads.

The clean build retains existing JavaDoc and Gradle deprecation warnings; there were no build or test failures. No gameplay code changed in this publishing implementation, and Minecraft runtime scenarios were not rerun.

## Behavior verified with fake APIs

- Preflight failure and an existing Modrinth conflict stop before GitHub/CurseForge writes.
- Publication is blocked while `PUBLISH_ENABLED` is not exactly `true`.
- Recovery uses original GitHub draft bytes even when a rebuild produces different jar bytes.
- Corrupt artifacts and conflicting manifests are rejected.
- Matching completed releases are no-ops, including a lost GitHub finalization response.
- An uncertain upload is never blindly repeated. Modrinth matching hashes can reconcile it; CurseForge requires maintainer evidence. Explicit verified-absence recovery is available after inspecting either platform.
- HTTP writes are not retried automatically. Transient reads have bounded retries; HTTP failures omit response bodies and tokens.
- Prereleases and older Minecraft release lines do not displace latest stable under the configured policy.

## Remaining rollout checks

- [ ] Maintainer commits/pushes these changes to `26.1` (the configured release branch) and selects `26.1` as the GitHub default branch for manual workflow discovery.
- [ ] Run the real GitHub-hosted Linux/Windows dry-run for the existing release tag.
- [ ] Verify repository secret/variable names and actual project IDs, then run `mode=preflight`.
- [ ] Verify account upload permission on both platforms. Read-only APIs do not prove upload scopes; CurseForge's author API cannot verify project-level upload permission or perform file lookup.
- [ ] Inspect the real preview artifacts and notes, enable publication, and deliberately run the first live release.
- [ ] Verify platform entries, loader/game/dependency metadata, artifact hashes, and CurseForge moderation/public availability.

Project/dependency IDs for Fabric API and Cloth Config were read from public platform metadata. The implementation uses two repository secrets plus the automatic GitHub token, as documented in the [setup guide](PUBLISHING.md). Tokens and destination project settings were not read from the user's account.

## Branch and manual-trigger correction

Publishing now has only `workflow_dispatch`; branch and tag pushes cannot publish. Resolution uses the selected workflow commit instead of checking out the repository default branch, and release ancestry is checked against `origin/26.1`. Local branch `26.1` now tracks `origin/26.1` rather than the deleted `origin/releases/26.1-rc1`. Repository default-branch settings remain unchanged remotely.

After these corrections, all 37 release-tool tests and actionlint passed locally. This includes accepting a release on `26.1` without requiring ancestry on `main`.
