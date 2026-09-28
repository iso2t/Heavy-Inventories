# Contributing to Heavy Inventories

Use JDK 25 and the included Gradle 9.7.1 wrapper:

```sh
# Linux/macOS
bash ./gradlew clean build

# Windows PowerShell
.\gradlew.bat clean build
```

The build runs common regression tests and checks metadata, mixins, services, enchantment resources, bundled weight
definitions, and test-harness exclusion in both loader jars. Distributable jars are under `fabric/build/libs/` and
`neoforge/build/libs/`; do not install sources, javadoc, or the separate test-harness jar.

For weight-loading changes, also run the new-world handoff regression against an existing **disposable** `New World`
save in each loader's `runs/client/saves/` folder:

```powershell
.\gradlew.bat :neoforge:runClient :fabric:runClient -PruntimeTests -PdatapackSmoke -PnewWorldHandoff -PpackagedSmoke '-PlifecycleClientWorld=New World' --console=plain
```

The test routes the save through Minecraft's actual new-world resource handoff, replacing its resource manager while
keeping loaded recipes. It then checks bundled weights, settings reload, client tooltips/totals, datapack overrides, and
failed-reload retention. Both clients must report `NEW WORLD WEIGHTS PASSED` and `CLIENT DATAPACK RELOAD PASSED`.
Ordinary existing-world startup does not exercise this handoff. Test only disposable saves: the harness changes
inventory and creates/removes fixture datapacks. When upgrading those saves from an older Minecraft release, add
`-PallowTestWorldUpgrade` to let the harness choose Minecraft's backup-and-upgrade option and continue after conversion.
To check a different NeoForge version, run only `:neoforge:runClient` and add a quoted override for the desired 26.3
release using
`'-Pneoforge_version=<version>'`.

Shared Gradle convention plugins live in the included `build-logic` build, following
the [Minecraft 26.3 MultiLoader template](https://github.com/jaredlll08/MultiLoader-Template/tree/bc8c34e73b8f69ce1896e6b7b0d34a0ab94446a7).
It replaces the former `buildSrc` directory; keep Heavy Inventories' resource generation and packaged-jar checks in
those conventions. The port uses the template's Fabric Mixin 0.17.3+mixin.0.8.7 and MixinExtras 0.5.3 dependencies and
Gradle 9.7.1 wrapper.

NeoForge uses 26.3.0.3-beta instead of the template's 26.3.0.1-beta to meet Cloth Config 26.3.159's minimum requirement.

The 26.3 candidate passed 100 common tests, both packaged-jar checks, both loaders' dedicated/singleplayer and
separate-process
multiplayer suites, new-world
weight handoff and datapack reload/conversion checks, and a NeoForge dedicated run without Cloth Config on Windows/JDK
25.
The bundled catalog contains 579 definitions and resolves all vanilla survival items without unexpected fallbacks.
HUD screenshots were checked on both loaders. A publishing dry run selected the 4.263.0.0-rc.1 jars and changelog for
all five
upload targets. Elytra checks cover glide range, rocket thrust, configuration edits, Soaring levels/equipment, and
client synchronization.
Linux runtime and third-party integrations have not been verified for this port. Historical changelogs retain their
original version numbers.

The GitHub Actions workflow is configured to build/test on Linux and Windows and upload reports and mod artifacts; local
checks do not establish a hosted CI result. Branch and PR builds do not publish releases or start Minecraft. The
separate publishing workflow is manual-only, reads the selected branch commit, and creates its version tag
automatically. The [runtime test module](../tests/README.md) documents opt-in local test runs; its sources are included
in a normal Gradle import.

Source and issue tracking: [iso2t/Heavy-Inventories](https://github.com/iso2t/Heavy-Inventories). Contributions and
translations are welcome. Licensed under [MIT](../LICENSE.md).

## Versioning

Starting with Minecraft 26.2, use `4.<Minecraft line>.<feature>.<patch>`. The `4` identifies the Heavy Inventories era,
and `262` identifies Minecraft 26.2.x. The first release is `4.262.0.0`; a bug fix becomes `4.262.0.1`, and a feature
update becomes `4.262.1.0`. Reset patch to zero when incrementing feature, and reset both counters when moving to a new
Minecraft line. The 26.1.2 branch retains its existing `4.0.1` numbering.

Stable releases use four dot-separated numeric components without leading zeros. Release candidates append
`-rc.<number>`,
such as `4.263.0.0-rc.1`; these publish as Beta on CurseForge and Modrinth and as prereleases on GitHub.
Keep the exact supported Minecraft version
in `minecraft_version`, dependency metadata, and release notes. Four components are supported
by [Fabric's version parser](https://docs.fabricmc.net/develop/loader/fabric-mod-json)
and [NeoForge's Maven versioning](https://docs.neoforged.net/docs/gettingstarted/versioning/); comparisons are numeric
by component, not plain text sorting.

Both loaders publish the same version; loader labels and filenames distinguish the downloads. The publishing
workflow still creates `v<version>` tags, uses optional `changelogs/<version>.md` notes, and runs only when manually
triggered. Select
the `26.3` branch for this release candidate (`4.263.0.0-rc.1`). Nothing is uploaded by a normal build.

## Changelogs

Record user-visible changes in [Unreleased](../changelogs/UNRELEASED.md) as work lands. When adding release notes,
curate a
`changelogs/<version>.md` file using the exact version from `gradle.properties`, and add it to
the [changelog index](../CHANGELOG.md). Remove empty headings and template placeholders. Include compatibility or
upgrade steps when they affect players or modpack authors.

Release notes are optional. When present and nonblank, the version-specific file supplies the same release body to
CurseForge, Modrinth, and GitHub Releases. Missing or blank files use
`Heavy Inventories <version> for Minecraft <minecraft_version>.` instead. Keep build
logs and internal test details in testing documentation, and keep the README as the project introduction. Published
changelogs should describe the original build; add later changes to the next version.

The [publishing plan](PUBLISHING_PLAN.md) defines the manual release trigger and the implementation checklist.
The [setup guide](PUBLISHING.md) covers tokens, repository secrets and variables, changelogs, and the manual Publish
action. The manual workflow defaults to dry-run; uncheck its preview checkbox to publish.
