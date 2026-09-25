# Publishing setup and release guide

Status: the publishing workflow and scripts are implemented locally. They take effect only after a maintainer commits and pushes them to GitHub. No release has been published or repository settings verified by this implementation. Adding secrets alone does not publish anything.

The [publishing plan](PUBLISHING_PLAN.md) tracks implementation. Commands below are for the maintainer to run deliberately; this guide does not authorize the assistant to commit, tag, push, or publish.

## 1. Prepare the platform projects

Create or use one Heavy Inventories mod project on each platform, with permission to upload releases. Both loader builds belong to the same project on each service.

- CurseForge: record the numeric project ID from the project's author overview URL. Use the project ID, not an individual file ID. The [upload API documentation](https://support.curseforge.com/support/solutions/articles/9000197321) describes where to find it.
- Modrinth: record the permanent project ID. For a public listing, open `https://api.modrinth.com/v2/project/YOUR_PROJECT_SLUG` in a browser and copy the `id` field, checking the title and owner correspond to your project. For a private draft, use its signed-in project settings to obtain the ID. The [project API](https://docs.modrinth.com/api/operations/getproject/) accepts either a slug or ID; store the permanent ID.

New listings can need moderation and an initial file before becoming public. A draft project with a valid ID can be prepared first; an upload being accepted does not mean the listing is public yet. Complete any platform review requirements during the first rollout.

## 2. Create the two publishing tokens

### CurseForge

Sign in with the account that owns the project or has upload permission. Open [CurseForge API Tokens](https://www.curseforge.com/account/api-tokens), create a token for Heavy Inventories publishing, and copy it directly into the GitHub secret described below. If that page is unavailable, follow the API Tokens link in the official [author upload API instructions](https://support.curseforge.com/support/solutions/articles/9000197321), which currently points to the legacy author portal.

Use an author upload token. A CurseForge developer/catalog API key is a different credential. Name the GitHub secret exactly `CURSEFORGE_TOKEN`.

### Modrinth

Sign in with an account allowed to upload versions to the project. Open your [Modrinth account settings](https://modrinth.com/settings/account), then Personal access tokens. Create a token named something like `Heavy Inventories GitHub publishing` with these scopes:

- Create versions (`VERSION_CREATE`).
- Read projects (`PROJECT_READ`), including the draft project during initial setup.
- Read versions (`VERSION_READ`), for validation and recovery.

The publisher creates new versions and reads existing ones; it does not need project creation, deletion, payout, or account-management permissions. Token scopes do not grant project membership: the account must also have upload permission. Set an expiration you can maintain, and replace the GitHub secret when rotating the token.

Name the GitHub secret exactly `MODRINTH_TOKEN`. Modrinth documents [personal access tokens](https://docs.modrinth.com/api/#authentication) and defines these [scope names in its source](https://github.com/modrinth/code/blob/main/apps/labrinth/src/models/v3/pats.rs).

## 3. Add repository secrets and variables

Open [Heavy-Inventories Actions secrets](https://github.com/iso2t/Heavy-Inventories/settings/secrets/actions). The navigation is **Settings → Secrets and variables → Actions**. On the Secrets tab, choose **New repository secret** for each token:

| Exact secret name | Value |
| --- | --- |
| `CURSEFORGE_TOKEN` | The CurseForge author upload token |
| `MODRINTH_TOKEN` | The Modrinth personal access token |

Paste the token itself, without surrounding quotes. Store these as repository Actions secrets, not Dependabot secrets or plain variables. Keep token values out of project files and chat. [GitHub secret setup](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)

Switch to the **Variables** tab and choose **New repository variable**, or open [Actions variables](https://github.com/iso2t/Heavy-Inventories/settings/variables/actions):

| Exact variable name | Initial value |
| --- | --- |
| `CURSEFORGE_PROJECT_ID` | Your real numeric CurseForge project ID |
| `MODRINTH_PROJECT_ID` | Your real permanent Modrinth project ID |
| `PUBLISH_ENABLED` | `false` |

IDs are configuration, so they belong in variables. Set `PUBLISH_ENABLED` to the exact lowercase value `true` only at the live-release step. This switch is part of our workflow, not a built-in GitHub setting. [GitHub variable setup](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-variables)

Do not create a `GITHUB_TOKEN` secret or obtain an extra GitHub personal access token for publishing. Actions supplies `GITHUB_TOKEN`; the publishing job requests `contents: write` in its YAML. Build jobs retain read-only permissions. You do not need to change every workflow's default permission to write. [GitHub workflow authentication](https://docs.github.com/en/actions/tutorials/authenticate-with-github_token)

Your local Git authentication for pushing is separate from these publishing tokens. Continue using your existing GitHub sign-in/credential manager or SSH setup for `origin`.

## 4. Publish from the branch

Normal releases take one manual Actions run. You do not create/push a tag, upload jars, or create a GitHub release yourself.

1. Keep `version` in `gradle.properties` and `changelogs/<version>.md` up to date, then commit/push the release code to `26.1` through your normal development workflow.
2. Open **Actions → Publish release → Run workflow** and select **26.1** in the branch selector.
3. Select **publish** in `mode`, leave `recovery` as `{}`, and run it. Repository variable `PUBLISH_ENABLED` must be `true`.

The workflow reads the version and notes from the selected commit, builds/tests Fabric and NeoForge, creates `v<version>` at that exact commit, uploads both jars to CurseForge and Modrinth, and finishes the GitHub release. It does not change source files or advance your branch. A branch push or tag push alone never publishes.

For a preview, choose `dry-run` (the default). It builds and produces artifacts without creating tags/releases or uploading to the platforms. `preflight` is an optional API-access check and also creates nothing. Neither is a required extra run before publishing.

Make `26.1` the repository default branch so GitHub exposes this manual workflow from the current code. Releases are allowed from `26.1` in `release/publishing.json`; no merge into old `main` is needed. Each run freezes the selected branch's commit, so a later branch push does not change a running release.

| Workflow input | Meaning |
| --- | --- |
| `mode` | `dry-run` (default), `preflight`, or `publish` |
| `recovery` | Leave `{}` except when reconciling an ambiguous upload below |

## 5. Versions and notes

Use a new version for each new release. For example, `version=4.0.0-rc.2` uses `changelogs/4.0.0-rc.2.md`, headed `# 4.0.0-rc.2`, and the workflow creates `v4.0.0-rc.2`. The release notes are the same on all three platforms.

Record changes in [Unreleased](../changelogs/UNRELEASED.md) during development, then curate the version file using the [template](../changelogs/TEMPLATE.md), update the [index](../CHANGELOG.md), and reset Unreleased. The workflow does not invent player-facing notes or choose your next version.

`-rc.N` and `-beta.N` publish as beta/prerelease, `-alpha.N` as alpha/prerelease, and a version without a suffix is stable. Versions and changelog filenames omit the leading `v` used for tags.

A matching tag is reused. A tag pointing to a different commit is never overwritten. The old manually created `v4.0.0-rc.1` points to earlier code; use a new version for the corrected release. There are no manual tag commands in the normal release process.

## 6. First rollout

Push the updated workflow/scripts to `26.1` and ensure the repository secrets/variables are configured. Optionally run `dry-run` to review the generated jars/notes, or `preflight` to check API access. Set `PUBLISH_ENABLED=true` when ready, then run `mode=publish` once.

CurseForge preflight checks API access only. Uploads send Minecraft/loader names directly through `gameVersionNames`; the upload endpoint validates them. Read-only API checks do not prove upload-only permissions. Check the resulting files and CurseForge moderation status after publication.

## 7. If publication fails

Inspect the workflow summary before rerunning. A failure can leave a GitHub draft or an accepted upload on one platform. Use **Re-run all jobs** on the original Actions run to retain its exact commit; the publisher must reuse the original artifacts and skip verified uploads. An ambiguous upload requires reconciliation before retrying. Do not delete the tag or create duplicate platform versions to restart the process.

| Symptom | Check |
| --- | --- |
| No publishing workflow listed | Push the workflow to `26.1` and make `26.1` the repository default branch; also check Actions is enabled |
| Publication disabled | `PUBLISH_ENABLED` is a repository variable with value `true` |
| Missing token | Exact secret spelling and repository Actions secret location |
| Unauthorized upload | Token expiry/scopes and account permission on the selected project |
| Wrong project / not found | Project ID rather than file/version ID; private project access |
| Version or changelog validation failed | Properties, filename, and heading agree |
| Tag points to a different commit | Use a new version and corresponding changelog; existing tags are not moved |
| Some destinations already succeeded | Rerun the original Actions run; inspect ambiguous outcomes |

Replace expired tokens by editing the existing secrets under the same names. Setting `PUBLISH_ENABLED=false` prevents future live attempts under the publication gate; it does not undo uploads or reliably stop an already-running job. Cancel an active job separately if necessary, then reconcile any completed uploads.

## Recovery evidence

For a partial failure, retry the original Actions run before advancing the release commit. If recovery evidence is needed, use Run workflow and select the generated release tag in the branch/tag selector to retain the original commit, then supply `recovery`.

The GitHub draft holds the canonical jars, manifest, and numbered `publish-state-*.json` ledger assets. Each upload intent is saved before the request and its receipt afterward. Never delete these assets to make a retry proceed. Once complete, the same release is a no-op. A new build with different contents must use a new version.

Modrinth uploads are reconciled automatically when the matching version and SHA-512 hash are visible. CurseForge lacks a documented author API for looking up a possibly successful upload, so a lost response stops the workflow. Inspect the author dashboard and the latest ledger before using `recovery`.

If the CurseForge file exists, verify its project, loader, game version, dependencies, release type, and notes in the dashboard. Obtain its direct ForgeCDN download URL. Supply an entry like this, replacing the example project/file IDs and URL with the verified values:

```json
{
  "curseforge:fabric": {
    "project_id": "123456",
    "project_verified": true,
    "id": 1234567,
    "download_url": "https://mediafilez.forgecdn.net/files/1234/567/heavyinventories-fabric-26.1-4.0.0-rc.1.jar"
  }
}
```

The script validates the CDN host, file ID/path, filename, and downloaded SHA-512 against the original jar. `project_verified` is your explicit attestation of the project and metadata check; it is not an automated permission check. If the file is still in moderation and cannot be downloaded, wait rather than uploading a duplicate.

If an intent was recorded but you have verified that the upload never happened (including private/pending files), authorize that missing attempt explicitly:

```json
{
  "curseforge:neoforge": {
    "project_id": "123456",
    "project_verified": true,
    "verified_absent": true
  }
}
```

The same absence form works for `modrinth:fabric` or `modrinth:neoforge`, using the Modrinth project ID. Allow time for a delayed request to settle and inspect the dashboard before attesting absence. Recovery entries cannot override a confirmed receipt or a mismatching remote version. Keep `recovery={}` for ordinary runs.

If draft staging failed before the manifest/ledger existed, no platform upload was started. Matching partial assets can be resumed; byte conflicts require inspecting the draft instead of silently overwriting it.

## Dependency policy and local verification

The platform metadata marks Fabric API and Cloth Config required for Fabric, and Cloth Config required for NeoForge. Required is the conservative listing choice because platform relations cannot express a client-only requirement; release notes explain that NeoForge dedicated servers can omit Cloth Config. Dependency IDs and trusted branches live in `release/publishing.json`.

Local validation uses Python 3.13 and JDK 25:

```powershell
python -m unittest discover -s scripts/release/tests -v
python scripts/release/release.py validate
.\gradlew.bat clean build releaseArtifacts --no-daemon --max-workers=2 --console=plain
python scripts/release/release.py package --bundle build/release/local-preview
python scripts/release/release.py preview --bundle build/release/local-preview
```

Choose an empty output directory for each package attempt. Local previews may include uncommitted work and are not the canonical Linux release bundle. Publishing happens through Actions from the selected commit, which the workflow tags automatically. The `release-bundle` artifact contains the jars, changelog, checksums, and manifest; `release-preview` contains destination payloads. Authenticated preflight and a real Actions run remain the final setup checks.
