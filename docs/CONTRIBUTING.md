# Contributing to Heavy Inventories

Use JDK 25 and the included Gradle 9.5.0 wrapper:

```sh
# Linux/macOS
bash ./gradlew clean build

# Windows PowerShell
.\gradlew.bat clean build
```

The build runs common regression tests and checks metadata, mixins, services, enchantment resources, bundled weight definitions, and test-harness exclusion in both loader jars. Distributable jars are under `fabric/build/libs/` and `neoforge/build/libs/`; do not install sources, javadoc, or lifecycle-test jars.

The GitHub Actions workflow builds/tests on Linux and Windows and uploads reports and mod artifacts. Branch and PR builds do not publish releases or start Minecraft. The separate publishing workflow is manual-only and uses an existing release tag. The [testing guide](TESTING.md) describes opt-in local runtime checks and their evidence.

Source and issue tracking: [iso2t/Heavy-Inventories](https://github.com/iso2t/Heavy-Inventories). Contributions and translations are welcome. Licensed under [MIT](../LICENSE.md).

## Changelogs

Record user-visible changes in [Unreleased](../changelogs/UNRELEASED.md) as work lands. Before releasing, curate a `changelogs/<version>.md` file using the exact version from `gradle.properties`, and add it to the [changelog index](../CHANGELOG.md). Remove empty headings and template placeholders. Include compatibility or upgrade steps when they affect players or modpack authors.

The version-specific file will supply the same release body to CurseForge, Modrinth, and GitHub Releases. Keep build logs and internal test details in testing documentation, and keep the README as the project introduction. Published changelogs should describe the original build; add later changes to the next version.

The [publishing plan](PUBLISHING_PLAN.md) defines the manual release trigger and the implementation checklist. The [setup guide](PUBLISHING.md) covers tokens, repository secrets and variables, changelogs, and tag commands. Publishing remains gated by repository configuration and an explicit manual publish run.
