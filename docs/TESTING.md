# Verification and compatibility

The supported verification baseline is Minecraft 26.1 / Java 25, Fabric Loader 0.18.5 + Fabric API 0.144.0+26.1, NeoForge 26.1.0.1-beta, and matching Cloth Config 26.1.154. Third-party storage/movement integrations and arbitrary resource packs remain unverified.

## Automated builds

```powershell
.\gradlew.bat clean build --console=plain
.\gradlew.bat :fabric:lifecycleTestClasses :neoforge:lifecycleTestClasses -I gradle/lifecycle-smoke.gradle --console=plain
```

On Linux/macOS, substitute `bash ./gradlew`. Add `--offline` only when all dependencies are already cached.

`build` runs the common JUnit suite and both `verifyModJar` tasks. Artifact checks verify loader identity, declared entrypoints/mixins/services, generated enchantments, license inclusion, and exclusion of runtime-test classes. Runtime harness compilation catches API drift without launching Minecraft.

[The GitHub Actions workflow](../.github/workflows/build.yml) runs these checks on Windows and Linux with Java 25, read-only repository permissions, bounded jobs, and commit-pinned actions. Reports and distributable jars are uploaded separately. Workflow execution in GitHub requires committing/pushing the files; local validation is not evidence of a hosted CI run. No release publishing or Minecraft EULA acceptance occurs in CI.

The workflow follows the official [Java setup](https://github.com/actions/setup-java), [Gradle setup](https://github.com/gradle/actions), and [artifact upload](https://github.com/actions/upload-artifact) documentation. Update action pins deliberately when upgrading CI.

## Local Minecraft acceptance tests

Use **disposable worlds only**. Tests replace player inventories, perform respawns/dimension travel, temporarily edit config/override files and permissions, and place/restore test blocks. They are excluded from production jars.

The tracked `gradle/lifecycle-smoke.gradle` is sufficient; `-PpackagedSmoke` loads the distributable mod jar in Gradle's game launch. The locally ignored `gradle/packaged-smoke.gradle` is not required. These are packaged-jar tests under a development launcher, not standalone production-installer tests.

### Prepare local test servers

Each loader uses `<loader>/runs/server/`. Minecraft requires the person running the server to accept its EULA before a world can load; the test harness does not accept it for you. The current local test servers were authorized separately.

Use these properties for isolated local testing:

```properties
server-ip=127.0.0.1
online-mode=false
enforce-secure-profile=false
level-name=step2-smoke
pause-when-empty-seconds=0
```

Set `server-port=25575` for Fabric and `server-port=25576` for NeoForge. Offline mode is used for local development identities; do not expose these test servers to a network. Create a disposable singleplayer save under each `<loader>/runs/client/saves/` using the normal development client, such as `New World`.

### Dedicated server

```powershell
.\gradlew.bat :fabric:runServer -I gradle/lifecycle-smoke.gradle -PpackagedSmoke --console=plain *> build/fabric-server.log
.\scripts\assert-runtime.ps1 -Log build/fabric-server.log -Mode Server
```

Substitute `neoforge` for the other loader. The server constructs real player entities, checks state ownership/calculation/movement, and stops itself. For NeoForge's server-side optional UI dependency check, additionally pass `-PwithoutCloth`.

### Integrated client/server

```powershell
.\gradlew.bat :fabric:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke '-PlifecycleClientWorld=New World' --console=plain *> build/fabric-client.log
.\scripts\assert-runtime.ps1 -Log build/fabric-client.log -Mode Client
```

Run the equivalent NeoForge command against its own disposable save. The client tests real entity replacement, inventory synchronization, configuration packets, operator commands, enchantments/potions, fluids, settings controls, and HUD visibility. It captures HUD/settings screenshots and stops after completion.

### Separate multiplayer processes and reconnection

Start the server in one terminal, then promptly start the client in another:

```powershell
.\gradlew.bat :fabric:runServer -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PauthorityMultiplayer --console=plain *> build/fabric-mp-server.log
```

```powershell
.\gradlew.bat :fabric:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PauthorityMultiplayer -PsmokeMultiplayer --console=plain *> build/fabric-mp-client.log
```

The server checks non-operator/invalid/stale edits, grants permission for a valid edit, verifies persisted state, and exercises movement. The client disconnects and reconnects to verify fresh entity state and replacement of cached definitions. Both stop after their assertions; temporary server config/operator state is restored during normal shutdown, including shutdown after an assertion failure.

Validate both logs:

```powershell
.\scripts\assert-runtime.ps1 -Log build/fabric-mp-server.log -Mode MultiplayerServer
.\scripts\assert-runtime.ps1 -Log build/fabric-mp-client.log -Mode MultiplayerClient
```

Do not treat Gradle's exit code alone as proof: a Minecraft server can log an assertion/crash and still let its launch task exit successfully. The assertion script requires explicit completion markers and rejects known failure signatures. A forcibly killed process may bypass cleanup; inspect disposable config/permission files before reuse.

## Current acceptance record

Verified locally on **Windows, 2026-09-24**, using the versions above and disposable local worlds.

| Check | Fabric | NeoForge |
| --- | --- | --- |
| Clean build and production-jar verification | Passed | Passed |
| Dedicated-server lifecycle, inventory, containers, recipes, movement | Passed | Passed |
| Integrated world: respawn, dimension travel, reload, permission-checked config, commands | Passed | Passed |
| Equipment replacement/removal, Strength expiry, Speed/Slowness, mounted movement, deep lava, Slow Falling | Passed | Passed |
| Separate multiplayer authority, disconnect/reconnect, fresh definitions and player state | Passed | Passed |
| Cloth screens, color persistence, HUD hiding, GUI scales 1/2, jump feedback | Passed | Passed |
| Dedicated server without Cloth Config | Required dependency; not applicable | Passed; absent from the loaded-mod list |

The common suite has **45 passing tests, zero failures/errors/skips**, including US/German decimal display and unit conversion. Both runtime harnesses compile after a clean build. HUD/settings screenshots were visually reviewed for both loaders; locale coverage is automated formatting coverage, not a review of every translated UI.

Local evidence is in ignored `build/step8-*.log` files (also copied to `runs/step8-verification/`). The combined `step8-clients.log` contains one complete integrated run for each loader. Separate `step8-<loader>-network-{server,client}.log` files contain reconnect results. `step8-fabric-server.log` and `step8-neoforge-no-cloth.log` record dedicated runs. Screenshots retain the harness names `<loader>/runs/client/screenshots/step7-hud.png` and `step7-settings.png`; they were regenerated during this verification.

The workflow YAML was parsed locally; triggers, Windows/Linux matrix, and commit pins were checked. The equivalent Windows build and harness-compilation commands passed. **Hosted GitHub Actions execution and Linux execution remain pending the first push.** No commit, push, or release was performed.

Remaining non-fatal warnings include missing Javadoc/deprecated APIs, Gradle features slated for removal in Gradle 10, Fabric development/mixin compatibility notices, and offline profile/Realms authentication warnings. Runtime assertions produced no Heavy Inventories injection or classloading failure. The temporary mounted-entity scenario can emit an unknown-passenger warning because the test boat is created and discarded within one server tick.

## Datapack loading stage

Step 2 introduced the datapack loader on both loaders. Step 3 now supplies gameplay weights as described below. It reads one winning resource per item from `data/<item_namespace>/heavyinventories/weights/<item_path>.json`. The supported forms are `{"weight": 0.053125}` and `{"infer": true}`. Pack priority replaces a whole resource; an inference marker can replace a lower pack's fixed value, and zero is an explicit weight.

Parsing rejects unknown/duplicate fields, malformed JSON, invalid numeric types/ranges, and positive values that would become zero in float storage. Definitions are limited to 4096 characters each and 100,000 winning resources per load. Errors include the resource and pack; valid resources for unregistered items are skipped with warnings. An invalid candidate exposes no partial definitions. Each resource-manager generation owns its immutable candidate, keeping separate reloads/worlds isolated.

The loader registers with Fabric's `DataResourceLoader` and NeoForge's `AddServerReloadListenersEvent`. Startup and actual server resource reloads were verified locally on both packaged loaders. Step 3 adds startup/explicit-rebuild gameplay adoption, invalid-candidate retention, and synchronization through the existing definition packets. Automatic adoption after vanilla reload remains Step 4.

Run the dedicated datapack scenario against the already prepared disposable test servers:

```powershell
.\gradlew.bat :fabric:runServer -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PdatapackSmoke --console=plain *> build/fabric-datapacks.log
.\scripts\assert-runtime.ps1 -Log build/fabric-datapacks.log -Mode Datapack
```

Repeat with `neoforge`. The scenario creates unique fixture packs, reloads them with ordered overrides, deliberately supplies a malformed winning definition, repairs/removes overrides, restores the original pack selection, and deletes its fixture directories. It checks that resource loading alone stages the candidate, then explicitly rebuilds gameplay weights and checks propagation and failure retention. A failed or forcibly interrupted test may leave its named fixture packs behind; use disposable worlds and inspect them before reuse.

The expected invalid-input phase logs a weight error with the arrow resource and fixture pack name; that diagnostic alone is not a crash. Require both `DATAPACK LOADING PASSED` and `DATAPACK GAMEPLAY PASSED` and a successful build using the assertion script. Local evidence: `build/datapack-step2-servers.log` (both loaders), `datapack-step2-tests.log`, `datapack-step2-harness.log`, and `datapack-step2-build.log`. The Step 2 common suite contained **56 tests**, including nine new parser/resource-manager tests and the two display-precision tests added after Step 8.

## Datapack gameplay resolution

Step 3 resolves a complete table for registered items from winning fixed definitions, supported recipes, and fallback. Inference markers contribute no fixed anchor. Startup runs after the world, recipes, and tags exist. Legacy namespace JSON files no longer affect gameplay. Dumps use the active gameplay table.

The dedicated scenario checks fresh-start arrow inference, zero, fixed/infer precedence, ingredient propagation after explicit rebuild, invalid-candidate retention, override removal, and complete restoration after disabling fixture packs. It uses existing disposable local servers and restores their pack selections.

The integrated client scenario additionally checks the startup arrow definition on both sides, single/64-arrow tooltip formatting, and a 3.4-pound server inventory total. It reruns inventory/container, lifecycle, movement, config, admin, HUD, and Cloth checks. Admin coverage confirms malformed legacy files are ignored, failed configuration reloads preserve state, exports match gameplay.

```powershell
.\gradlew.bat :fabric:runServer :neoforge:runServer -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PdatapackSmoke --offline --console=plain *> build/datapack-step3-servers.log
.\scripts\assert-runtime.ps1 -Log build/datapack-step3-servers.log -Mode Datapack
.\gradlew.bat :fabric:runClient :neoforge:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke '-PlifecycleClientWorld=New World' --offline --console=plain *> build/datapack-step3-clients.log
.\scripts\assert-runtime.ps1 -Log build/datapack-step3-clients.log -Mode Client
.\gradlew.bat build --offline --console=plain *> build/datapack-step3-build.log
```

Both packaged dedicated servers and integrated clients passed locally on 2026-09-24, including two occurrences each of the new datapack/server and client completion markers. The common suite contains **62 passing tests**, including six new resolution tests. The full build and both production-jar verification tasks passed. Logs above are local ignored evidence; no release was published.

Current boundary: Minecraft resource reload stages definitions; restart or explicitly rebuild with `/heavyinventories reload` after `/reload` finishes to adopt edited pack data. Automatic vanilla-reload adoption is Step 4. Legacy conversion and export provenance are Step 5; curated material defaults are Step 6. Separate multiplayer reconnect checks were not repeated for Step 3; the earlier Step 8 record remains historical evidence. Startup with invalid datapack data has a fallback-only recovery branch, but that startup failure path was not separately exercised in these runtime runs.

## Compatibility boundaries

- Tests use vanilla assets plus loader/Cloth resources. Custom resource packs, HUD replacements, shader/rendering mods, third-party movement changes, and unusual GUI/font combinations are not covered.
- Vanilla containers and standard item-content components are covered. Custom backpack APIs, external inventories, and Ender storage are outside the implemented scope.
- Shared physics affects client movement using server-derived state. This is not an anti-cheat guarantee against modified clients.
- Datapack gameplay resolution is active at startup/explicit rebuild; automatic vanilla-reload adoption and bundled material defaults remain pending. Conditional custom enchantment effects, density, and stamina also remain unimplemented.
- Matching client/server Heavy Inventories builds are required. Broader Minecraft/loader version ranges in metadata do not imply that all versions in those ranges were tested.
