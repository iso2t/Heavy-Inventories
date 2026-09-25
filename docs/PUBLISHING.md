# Publishing

Publishing uses [Mod Publish Plugin](https://modmuss50.github.io/mod-publish-plugin/) in `build.gradle`. The manual workflow runs `build publishMods`, uploads Fabric and NeoForge to CurseForge and Modrinth, and creates a GitHub tag/release containing both jars.

## Repository setup

In GitHub **Settings → Secrets and variables → Actions**, configure **repository secrets**:

| Secret | Value |
| --- | --- |
| `CURSEFORGE_API_KEY` | CurseForge author upload token from [API Tokens](https://www.curseforge.com/account/api-tokens) |
| `MODRINTH_TOKEN` | Modrinth personal access token from [account settings](https://modrinth.com/settings/account) |

An existing `CURSEFORGE_TOKEN` secret also works; you do not need to rename it. If both names exist, `CURSEFORGE_API_KEY` takes precedence. Use an author upload token for CurseForge, not a developer/catalog API key. The Modrinth plugin recommends Create versions, Read versions, and Write versions scopes. Both accounts need upload permission on the target project.

On the **Variables** tab, configure **repository variables**:

| Variable | Heavy Inventories project |
| --- | --- |
| `CURSEFORGE_PROJECT_ID` | `245768` |
| `MODRINTH_PROJECT_ID` | `2xLhHOis` |

GitHub supplies `GITHUB_TOKEN` automatically. The workflow requests `contents: write` to create tags and releases. No environment secrets, extra GitHub token, or `PUBLISH_ENABLED` variable is needed. An old `PUBLISH_ENABLED` variable is ignored.

GitHub requires the manual workflow file on the repository's default branch to expose **Run workflow**. Keep the current publishing workflow there, and select `26.1` when running this release line. Ordinary branch and tag pushes only build; they never publish.

## Publish a version

1. Set `version` in `gradle.properties` and prepare `changelogs/<version>.md`. Commit and push through your normal development workflow.
2. Open **Actions → Publish release → Run workflow**, and select **26.1**.
3. Uncheck **Build and preview without uploading**, then run.

That one run builds, tests, verifies both jars, uploads them, and creates `v<version>` and its GitHub release. No manual tag creation, jar upload, preflight run, or recovery JSON is required. The GitHub tag targets the exact commit selected when the run started.

Leave the checkbox checked for a dry run. It builds and saves preview files in the `publishing-artifacts` Actions artifact without uploading to the release platforms or creating a tag.

## Versions and changelogs

Record changes in [Unreleased](../changelogs/UNRELEASED.md), then curate `changelogs/<version>.md` using the [template](../changelogs/TEMPLATE.md) and update the [index](../CHANGELOG.md). All three platforms use that file verbatim. A missing version-specific file stops publication. The workflow does not choose a new version or write release notes for you.

Versions containing `-alpha` publish as alpha; other suffixed versions, including `-rc` and `-beta`, publish as beta/prerelease. Versions without a suffix are stable. Modrinth appends `+fabric` or `+neoforge` to distinguish the two entries.

Use a new version for a new build. Earlier attempts may already have created `v4.0.0-rc.1` or a draft release; this workflow does not move tags or clean up previous attempts. Inspect those before retrying that version.

## Metadata and failures

Both loaders target exactly `minecraft_version` from `gradle.properties`. CurseForge declares both client and server support. Fabric requires Fabric API and Cloth Config; NeoForge lists Cloth Config as required, with release notes explaining that dedicated servers can omit it. These settings live together in `build.gradle`.

If a run fails during publishing, inspect the failing Gradle task and the destination dashboards before rerunning: an earlier upload may already have succeeded. This simple publisher does not implement cross-platform rollback or deduplication. Keep the original failure details when reporting an API rejection; a successful dry run does not verify credentials or live upload acceptance.

## Local preview

With JDK 25 installed, run in PowerShell:

```powershell
$env:PUBLISH_DRY_RUN = 'true'
$env:CURSEFORGE_PROJECT_ID = '245768'
$env:MODRINTH_PROJECT_ID = '2xLhHOis'
.\gradlew.bat build publishMods --stacktrace
```

No tokens are needed for a preview. Local invocations also default to dry-run when `PUBLISH_DRY_RUN` is absent. Normal `build` does not publish.
