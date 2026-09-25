# Publishing setup and release guide

Status: setup instructions for the agreed publishing design. Only the build workflow exists today; the publishing workflow and scripts still need to be implemented. You can create the platform listings and repository secrets/variables now. The dry-run and publication procedures below become available after implementation. Adding secrets alone does not publish anything.

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

The proposed publisher creates new versions and reads existing ones; it does not need project creation, deletion, payout, or account-management permissions. Token scopes do not grant project membership: the account must also have upload permission. Set an expiration you can maintain, and replace the GitHub secret when rotating the token.

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

IDs are configuration, so they belong in variables. Set `PUBLISH_ENABLED` to the exact lowercase value `true` only at the live-release step. This switch is part of our proposed workflow, not a built-in GitHub setting. [GitHub variable setup](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-variables)

Do not create a `GITHUB_TOKEN` secret or obtain an extra GitHub personal access token for publishing. Actions supplies `GITHUB_TOKEN`; the publishing job will request `contents: write` in its YAML. Build jobs retain read-only permissions. You do not need to change every workflow's default permission to write. [GitHub workflow authentication](https://docs.github.com/en/actions/tutorials/authenticate-with-github_token)

Your local Git authentication for pushing is separate from these publishing tokens. Continue using your existing GitHub sign-in/credential manager or SSH setup for `origin`.

## 4. Prepare a release commit and changelog

These three values must match exactly; RC1 is an example, not a decision to publish that version:

| Location | Example |
| --- | --- |
| `gradle.properties` | `version=26.1.0.0-rc.1` |
| Changelog filename and first heading | `changelogs/26.1.0.0-rc.1.md` and `# 26.1.0.0-rc.1` |
| Git tag | `v26.1.0.0-rc.1` |

While developing, add player-facing changes to [Unreleased](../changelogs/UNRELEASED.md). Before release, curate them into the version file using the [template](../changelogs/TEMPLATE.md), remove empty sections/placeholders, update the [index](../CHANGELOG.md), and reset Unreleased for subsequent work. The version file supplies the release notes for all three platforms.

Commit and merge the release changes through your normal Git workflow. The release commit must contain the publishing workflow, scripts, version, and changelog; the default branch must also contain the workflow for manual runs. Finish build and gameplay verification before tagging. Tag the exact reviewed commit, not a later untested change.

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
git tag -a v26.1.0.0-rc.1 -m "Heavy Inventories 26.1.0.0-rc.1" HEAD
git show --no-patch v26.1.0.0-rc.1
```

When ready, push only that tag:

```powershell
git push origin refs/tags/v26.1.0.0-rc.1
```

Substitute the chosen version everywhere. An ordinary branch push does not normally push this tag. Prefer the explicit single-tag command over `git push --tags`, which sends all local tags. Do not move or force-push a published tag; changed release contents need a new version. [Git's tagging guide](https://git-scm.com/book/en/v2/Git-Basics-Tagging)

Once the publisher is implemented and enabled, **the tag push is the publication trigger**. Creating the local tag alone does not publish. You do not also need to create a GitHub release manually; the workflow creates it.

## 6. First rollout: dry-run before enabling publication

This procedure is for the future publishing workflow. Its input names and UI labels must be finalized during implementation.

1. Keep `PUBLISH_ENABLED=false`. Ensure the implementation is on the default branch and in the reviewed release commit.
2. Create and push the chosen release tag using step 5. Live publication must remain blocked by the switch.
3. Open the repository's Actions page, select the publishing workflow, and choose **Run workflow**. Use its default-branch workflow, select the existing release tag as the release input, and keep dry-run selected.
4. Inspect the resulting two jars, changelog, checksums, manifest, and destination preview. Both Linux and Windows validation must pass. Dry-run validates the release without publishing credentials; it cannot prove the tokens work.
5. Complete the publisher's authenticated preflight for project access, credentials, game/loader metadata, and dependencies. This preflight must finish before any external write in a live attempt.
6. Set `PUBLISH_ENABLED=true`. Manually run the same existing tag with explicit live publication selected. Changing the variable does not replay the earlier tag event, and pushing an unchanged tag does not create another event.
7. Verify both loader entries on CurseForge and Modrinth, and the GitHub release containing both jars. Confirm dependency declarations and any pending moderation.

For later releases, leave publishing enabled, prepare the new release commit/changelog, and push its new tag. The workflow then validates, builds, and publishes automatically. Manual runs continue to default to dry-run.

## 7. If publication fails

Inspect the workflow summary before rerunning. A failure can leave a GitHub draft or an accepted upload on one platform. With recovery implemented, use manual live mode for the same tag; the publisher must reuse the original artifacts and skip verified uploads. An ambiguous upload requires reconciliation before retrying. Do not delete the tag or create duplicate platform versions to restart the process.

| Symptom | Check |
| --- | --- |
| No publishing workflow listed | It is not implemented/on the default branch yet, or Actions is disabled |
| Publication disabled | `PUBLISH_ENABLED` is a repository variable with value `true` |
| Missing token | Exact secret spelling and repository Actions secret location |
| Unauthorized upload | Token expiry/scopes and account permission on the selected project |
| Wrong project / not found | Project ID rather than file/version ID; private project access |
| Version or changelog validation failed | Tag, properties, filename, heading, and reviewed commit agree |
| Some destinations already succeeded | Use recovery on the same tag; inspect ambiguous outcomes |

Replace expired tokens by editing the existing secrets under the same names. Setting `PUBLISH_ENABLED=false` prevents future live attempts under the planned gate; it does not undo uploads or reliably stop an already-running job. Cancel an active job separately if necessary, then reconcile any completed uploads.
