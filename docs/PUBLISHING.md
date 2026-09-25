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

## 4. Prepare a release commit and changelog

These three values must match exactly; RC1 is an example, not a decision to publish that version:

| Location | Example |
| --- | --- |
| `gradle.properties` | `version=4.0.0-rc.1` |
| Changelog filename and first heading | `changelogs/4.0.0-rc.1.md` and `# 4.0.0-rc.1` |
| Git tag | `v4.0.0-rc.1` |

While developing, add player-facing changes to [Unreleased](../changelogs/UNRELEASED.md). Before release, curate them into the version file using the [template](../changelogs/TEMPLATE.md), remove empty sections/placeholders, update the [index](../CHANGELOG.md), and reset Unreleased for subsequent work. The version file supplies the release notes for all three platforms.

Commit and merge the release changes through your normal Git workflow. The release commit must contain the publishing workflow, scripts, version, and changelog. Keep it on `26.1`: `release/publishing.json` permits release commits reachable from that branch. No merge into `main` is required. Set the repository default branch to `26.1` in GitHub repository Settings so GitHub can expose the manual workflow from the current code. The workflow resolves its setup from the branch selected for the manual run, then builds the exact release tag commit. If a future maintenance branch should publish, add its exact name to `release_branches` in that configuration through review first. Finish build and gameplay verification before tagging. Tag the exact reviewed commit, not a later untested change.

Channel mapping: `-rc.N` and `-beta.N` publish as beta/prerelease; `-alpha.N` as alpha/prerelease; a version without a suffix is stable. The tag's leading `v` does not appear in `gradle.properties` or the changelog filename.

## 5. Create and push one release tag

Run these commands in the repository after checking out the intended release commit. They are PowerShell-compatible. Check that the working tree is clean and the displayed commit is the reviewed release:

```powershell
git status --short
git log -1 --oneline
git remote -v
```

The expected `origin` is `https://github.com/iso2t/Heavy-Inventories.git` (or its SSH equivalent). A tag identifies a commit; it does not include uncommitted edits.

For the example version, create an annotated local tag and inspect it:

```powershell
git tag -a v4.0.0-rc.1 -m "Heavy Inventories 4.0.0-rc.1" HEAD
git show --no-patch v4.0.0-rc.1
```

When ready, push only that tag:

```powershell
git push origin refs/tags/v4.0.0-rc.1
```

Substitute the chosen version everywhere. An ordinary branch push does not normally push this tag. Prefer the explicit single-tag command over `git push --tags`, which sends all local tags. Do not move or force-push a published tag; changed release contents need a new version. [Git's tagging guide](https://git-scm.com/book/en/v2/Git-Basics-Tagging)

**Pushing a tag does not publish.** Publication is manual-only: open Actions, select Publish release, choose Run workflow, select branch `26.1`, enter the existing tag, and choose `mode=publish`. You do not create the GitHub release yourself; the workflow creates it. Ordinary branch pushes still run Build and verify.

## 6. First rollout: dry-run before enabling publication

In **Actions → Publish release → Run workflow**, the inputs are:

| Input | Meaning |
| --- | --- |
| `tag` | Existing tag, for example `v4.0.0-rc.1` |
| `mode` | `dry-run` (default), `preflight` (API reads only), or `publish` |
| `recovery` | Leave `{}` unless reconciling an ambiguous upload as described below |

The ordinary branch/PR build validates release notes and tests the publisher without tokens. Tag pushes do not run the publisher. A release run builds on Linux and Windows, compiles the runtime harness, and packages the exact verified Linux jars. Only after both builds pass can publication begin. It does not launch Minecraft gameplay tests.

1. Keep `PUBLISH_ENABLED=false`. Set `26.1` as the default branch and ensure it contains the implementation and reviewed release commit.
2. Create and push the chosen release tag using step 5. This makes the tag available to the manual workflow; pushing it does not start publication.
3. Open the repository's Actions page, select the publishing workflow, and choose **Run workflow**. Select branch `26.1`, enter the existing release tag in the `tag` input, and set `mode=dry-run` and leave `recovery={}`.
4. Inspect the resulting two jars, changelog, checksums, manifest, and destination preview. Both Linux and Windows validation must pass. Dry-run validates the release without publishing credentials; it cannot prove the tokens work.
5. Run the same tag with `mode=preflight`. This reads GitHub, Modrinth, and CurseForge metadata with the configured credentials, without creating a draft or uploading files. This read-only check cannot prove upload-only permissions/scopes: CurseForge does not expose a documented project-permission or file lookup endpoint in its author API. Verify your CurseForge account can upload to the configured project. The live path repeats its available checks before writing.
6. Set `PUBLISH_ENABLED=true`. Manually run the same existing tag with `mode=publish`. Changing the variable or pushing a tag never starts publication; only the explicit manual run does.
7. Verify both loader entries on CurseForge and Modrinth, and the GitHub release containing both jars. Confirm dependency declarations and any pending moderation.

For later releases, prepare the version/changelog, push the reviewed commit to `26.1`, and push its new tag. Then manually run Publish release with that tag and `mode=publish`. Every manual run defaults to `dry-run`; live publication also requires `PUBLISH_ENABLED=true`. Existing tags still point to their original commits; use a new version/tag to include subsequent fixes.

## 7. If publication fails

Inspect the workflow summary before rerunning. A failure can leave a GitHub draft or an accepted upload on one platform. Use manual `mode=publish` for the same tag; the publisher must reuse the original artifacts and skip verified uploads. An ambiguous upload requires reconciliation before retrying. Do not delete the tag or create duplicate platform versions to restart the process.

| Symptom | Check |
| --- | --- |
| No publishing workflow listed | Push the workflow to `26.1` and make `26.1` the repository default branch; also check Actions is enabled |
| Publication disabled | `PUBLISH_ENABLED` is a repository variable with value `true` |
| Missing token | Exact secret spelling and repository Actions secret location |
| Unauthorized upload | Token expiry/scopes and account permission on the selected project |
| Wrong project / not found | Project ID rather than file/version ID; private project access |
| Version or changelog validation failed | Tag, properties, filename, heading, and reviewed commit agree |
| Some destinations already succeeded | Use recovery on the same tag; inspect ambiguous outcomes |

Replace expired tokens by editing the existing secrets under the same names. Setting `PUBLISH_ENABLED=false` prevents future live attempts under the publication gate; it does not undo uploads or reliably stop an already-running job. Cancel an active job separately if necessary, then reconcile any completed uploads.

## Recovery evidence

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
python scripts/release/release.py package --tag v4.0.0-rc.1 --bundle build/release/local-preview
python scripts/release/release.py preview --bundle build/release/local-preview
```

Choose an empty output directory for each package attempt. Local previews may include uncommitted work and are not the canonical Linux release bundle. Publishing happens through Actions from the reviewed tagged commit. The `release-bundle` artifact contains the jars, changelog, checksums, and manifest; `release-preview` contains destination payloads. Authenticated preflight and a real Actions run remain the final setup checks.
