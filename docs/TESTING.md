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

The loader registers with Fabric's `DataResourceLoader` and NeoForge's `AddServerReloadListenersEvent`. Startup and actual server resource reloads were verified locally on both packaged loaders. Step 3 adds startup/explicit-rebuild gameplay adoption, invalid-candidate retention, and synchronization through the existing definition packets. Step 4 now adopts completed vanilla reloads automatically, as described below.

Run the dedicated datapack scenario against the already prepared disposable test servers:

```powershell
.\gradlew.bat :fabric:runServer -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PdatapackSmoke --console=plain *> build/fabric-datapacks.log
.\scripts\assert-runtime.ps1 -Log build/fabric-datapacks.log -Mode Datapack
```

Repeat with `neoforge`. The scenario creates unique fixture packs, reloads them with ordered overrides, deliberately supplies a malformed winning definition, repairs/removes overrides, restores the original pack selection, and deletes its fixture directories. It checks automatic gameplay adoption after the complete resource reload, propagation through recipe and tag changes, and failure retention. A failed or forcibly interrupted test may leave its named fixture packs behind; use disposable worlds and inspect them before reuse.

The expected invalid-input phase logs a weight error with the arrow resource and fixture pack name; that diagnostic alone is not a crash. The current scenario requires `DATAPACK LOADING PASSED`, `DATAPACK GAMEPLAY PASSED`, `DATAPACK RELOAD PASSED`, and a successful build using the assertion script. Local evidence: `build/datapack-step2-servers.log` (both loaders), `datapack-step2-tests.log`, `datapack-step2-harness.log`, and `datapack-step2-build.log`. The Step 2 common suite contained **56 tests**, including nine new parser/resource-manager tests and the two display-precision tests added after Step 8.

## Datapack gameplay resolution

Step 3 resolves a complete table for registered items from winning fixed definitions, supported recipes, and fallback. Inference markers contribute no fixed anchor. Startup runs after the world, recipes, and tags exist. Legacy namespace JSON files no longer affect gameplay. Dumps use the active gameplay table.

The dedicated scenario checks fresh-start arrow inference, zero, fixed/infer precedence, ingredient propagation after explicit rebuild, invalid-candidate retention, override removal, and complete restoration after disabling fixture packs. It uses existing disposable local servers and restores their pack selections.

The integrated client scenario additionally checks the startup arrow definition on both sides, single/64-arrow tooltip formatting, and a server inventory total (originally 3.4 pounds; 5.76 with the bundled Step 6 defaults). It reruns inventory/container, lifecycle, movement, config, admin, HUD, and Cloth checks. Admin coverage confirms malformed legacy files are ignored, failed configuration reloads preserve state, and exports match gameplay.

```powershell
.\gradlew.bat :fabric:runServer :neoforge:runServer -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PdatapackSmoke --offline --console=plain *> build/datapack-step3-servers.log
.\scripts\assert-runtime.ps1 -Log build/datapack-step3-servers.log -Mode Datapack
.\gradlew.bat :fabric:runClient :neoforge:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke '-PlifecycleClientWorld=New World' --offline --console=plain *> build/datapack-step3-clients.log
.\scripts\assert-runtime.ps1 -Log build/datapack-step3-clients.log -Mode Client
.\gradlew.bat build --offline --console=plain *> build/datapack-step3-build.log
```

Both packaged dedicated servers and integrated clients passed locally on 2026-09-24, including two occurrences each of the new datapack/server and client completion markers. The common suite contains **62 passing tests**, including six new resolution tests. The full build and both production-jar verification tasks passed. Logs above are local ignored evidence; no release was published.

Historical Step 3 boundary: resource reload staged definitions and required a second command. Step 4 below removes that requirement. Legacy conversion and export provenance are Step 5; curated material defaults are Step 6. Separate multiplayer reconnect checks were not repeated for Step 3; the earlier Step 8 record remains historical evidence. Startup with invalid datapack data has a fallback-only recovery branch, but that startup failure path was not separately exercised in these runtime runs.

## Automatic datapack reload

Step 4 adds a shared server mixin that chains weight adoption to the successful completion of `MinecraftServer.reloadResources`, on the server thread. It runs after Minecraft applies the new resource manager, recipes, and tags. Exceptional completion skips adoption. Invalid weight candidates retain the active table, packets, and revision, with diagnostics in the log and a message to connected operators.

A successful commit increments the revision once, then refreshes every connected player's holder and sends the complete definitions and updated totals. Holder revision checks invalidate inventory caches even when no stack changed. Vanilla reload uses the active server settings; the separate mod reload command still reads the configuration file and rebuilds from loaded resources.

The dedicated scenario checks explicit weights, infer markers, zero, priority, ingredient propagation, changed recipe output count, changed ingredient tag, invalid weight retention, and pack removal. A test-only mixin deliberately fails a reload listener after staging valid weights to prove that a failed Minecraft reload cannot publish them. This injector is excluded from production jars. Enchantment registry edits were unsuitable as a failure fixture because that registry is not reloaded by this path.

The client variant holds 64 arrows through eight checkpoints. At each checkpoint it waits for actual definition/player packets, checks the revision and inventory total, and verifies single-item and stack tooltips before allowing the server to proceed. Both loader runs restore the original pack selection and remove their unique fixtures on success; interrupted/failed runs can require fixture cleanup.

```powershell
.\gradlew.bat :fabric:runServer :neoforge:runServer -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PdatapackSmoke --offline --console=plain *> build/datapack-step4-servers.log
.\scripts\assert-runtime.ps1 -Log build/datapack-step4-servers.log -Mode Datapack
.\gradlew.bat :fabric:runClient :neoforge:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PdatapackSmoke '-PlifecycleClientWorld=New World' --offline --console=plain *> build/datapack-step4-clients.log
.\scripts\assert-runtime.ps1 -Log build/datapack-step4-clients.log -Mode DatapackClient
.\gradlew.bat build --offline --console=plain *> build/datapack-step4-build.log
```

Verified locally on 2026-09-24: both packaged dedicated-server runs and both packaged integrated-client runs passed, including two `CLIENT DATAPACK RELOAD PASSED` markers. The full build, all 62 common tests, and both production-jar verification tasks passed. Step 4 adds runtime regression coverage rather than new unit tests. Separate-process multiplayer/reconnect and the broader GUI/movement suite were not rerun for this step; their earlier acceptance records remain historical evidence.

## Conversion and provenance reports

Step 5 adds `/heavyinventories convert legacy <pack_name>`. The command validates legacy `weights/*.json` before publishing a completed ZIP in `weight-packs/`, refuses existing outputs, preserves inputs, and never installs/enables the generated pack. Tests cover strict parsing, duplicates, invalid ranges/underflow, archive path safety, fractions, explicit zero, nested paths, absent-mod IDs, skipped entries without weights, and output collisions. Limits are 1024 namespace files, 100,000 entries, 16 Mi characters per file, and 64 Mi characters total.

The resolver now records source classification during inference, rather than guessing from a resulting value. The active table retains immutable provenance alongside weights and packets. Reports include format/version, pounds, revision, full item IDs, source classification, and the winning definition's pack/resource/mode when present. A recipe result equal to the fallback is still labeled `recipe`. A rejected reload leaves report provenance unchanged.

The dedicated scenario now converts a private fixture into a ZIP, installs it as a test pack, verifies Minecraft accepts its metadata and applies its explicit weight, checks pack provenance, and removes it with the other fixtures. The integrated admin scenario invokes the actual converter and dump commands, verifies source-file preservation and no gameplay/pack-selection changes, rejects collisions and malformed input, checks report contents, and verifies non-operator denial. Temporary command outputs are removed.

```powershell
.\gradlew.bat :fabric:runServer :neoforge:runServer -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PdatapackSmoke --offline --console=plain *> build/datapack-step5-servers.log
.\scripts\assert-runtime.ps1 -Log build/datapack-step5-servers.log -Mode Datapack
.\gradlew.bat :fabric:runClient :neoforge:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke '-PlifecycleClientWorld=New World' --offline --console=plain *> build/datapack-step5-clients.log
.\scripts\assert-runtime.ps1 -Log build/datapack-step5-clients.log -Mode Client
.\gradlew.bat build --offline --console=plain *> build/datapack-step5-build.log
```

Verified locally on 2026-09-24: both dedicated conversion/reload runs and both integrated admin/gameplay runs passed. The server assertion mode additionally requires `DATAPACK CONVERSION PASSED`. All **69 common tests** passed (six converter tests and one provenance/report test added), along with the full build and both production-jar verification tasks. Language JSON was parsed strictly after a runtime check caught and prompted correction of a trailing comma. Separate-process multiplayer reconnect and the eight-checkpoint client reload variant were not repeated for Step 5; their earlier records remain historical evidence.

## Compatibility boundaries

- Tests use vanilla assets plus loader/Cloth resources. Custom resource packs, HUD replacements, shader/rendering mods, third-party movement changes, and unusual GUI/font combinations are not covered.
- Vanilla containers and standard item-content components are covered. Custom backpack APIs, external inventories, and Ender storage are outside the implemented scope.
- Shared physics affects client movement using server-derived state. This is not an anti-cheat guarantee against modified clients.
- Datapack gameplay resolution is active at startup and after successful resource reloads; bundled material defaults are included. Conditional custom enchantment effects, density, and stamina also remain unimplemented.
- Matching client/server Heavy Inventories builds are required. Broader Minecraft/loader version ranges in metadata do not imply that all versions in those ranges were tested.

## Bundled default weights

Step 6 uses gameplay-scaled material weights with unchanged 1,000-pound capacity. The build generates 549 per-item resources from `common/src/main/weight-defaults.json`; both production-jar checks compare every packaged definition against that catalog and check for stale entries.

The real Minecraft 26.1 table matches across Fabric and NeoForge: 549 explicit weights, 852 recipe results, and 105 fallbacks. All remaining fallbacks are spawn eggs or creative/admin-only items. The runtime harness rejects unexpected survival-item fallbacks and unknown bundled item IDs.

Checks cover stone/log/metal anchors; arrow, tool, armor, container, compression, and copper-lantern results; and the 132.41-pound example kit. An override removes bundled charcoal's fixed 0.5 weight with `{"infer":true}`, producing 2 from logs; removing the override restores 0.5. Existing checks also cover fixed replacement, explicit zero, recipe/tag propagation, rejection, restoration, and converted packs.

Both integrated clients passed all eight live reload checkpoints with synchronized single/stack tooltips and unchanged-inventory recalculation. The regular lifecycle, nested container/equipment, movement, configuration, export/conversion, HUD, and Cloth GUI checks also passed. All 69 common tests and both packaged-jar checks passed.

Local evidence:
- `build/datapack-step6-build-servers.log`: full build and both packaged dedicated-server scenarios.
- `build/datapack-step6-reload-clients.log`: both packaged integrated-client reload scenarios.
- `build/datapack-step6-clients.log`: both existing integrated-client regression suites.
- `<loader>/runs/server/bundled-defaults-report.json`: complete startup table and source classifications for audit.

Reproduce in the existing disposable test worlds (accept the EULA before a new dedicated-server setup):

```powershell
.\gradlew.bat build :fabric:runServer :neoforge:runServer -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PdatapackSmoke --offline --console=plain
.\gradlew.bat :fabric:runClient :neoforge:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PdatapackSmoke "-PlifecycleClientWorld=New World" --offline --console=plain
.\gradlew.bat :fabric:runClient :neoforge:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke "-PlifecycleClientWorld=New World" --offline --console=plain
```

Use `scripts/assert-runtime.ps1` with `Datapack`, `DatapackClient`, or `Client` for the corresponding log. The datapack modes require `BUNDLED DEFAULTS PASSED` as well as their existing markers. These runs validate calculation and delivery, not long-term gameplay balance or third-party modpack coverage. Separate-process multiplayer reconnect checks were not repeated in Step 6.

## Weight ring HUD

The shared renderer draws the supplied 16×16 frame over a matching interior mask. Fabric registers after INFO_BAR so the ring survives XP level zero; NeoForge registers below EXPERIENCE_LEVEL. Both wrap the existing XP layer with a scoped vertical translation. Production jars contain no new mixins for this feature; test-only injections observe the real vanilla XP transform.

The full build and **74 common tests** passed. New tests cover the mask against the actual PNG, fill quantization and capacity changes, migration of old/off settings, integer range validation, and persistence of HUD modes/offsets. Both jar checks now require the frame texture.

Both packaged clients passed **20 visual checkpoints**: weights 0/25/50/89/90/99/100/125%, XP levels 0/1/30/100/1000, offsets 0/7/8/12/64, ring/numbers/both/off modes, and a real nested-container calculation limit. The harness verifies server-synchronized weights, ring visibility, the actual transform seen by vanilla XP drawing, and restoration of vanilla positioning when the ring is hidden. Cloth tests save offset 12, reload it, reopen the screen, and then restore the disposable client's preferences.

Screenshots under `<loader>/runs/client/screenshots/ring-*.png` and `step7-settings.png` show the actual game renderer. The ring/XP layering and the settings screen were visually inspected on both loaders. Existing F1, GUI scales 1/2, screen hiding, respawn/dimension, equipment/container, Strength/movement, admin, and feedback checks also passed.

Evidence:
- `build/hud-ring-clients.log`: full build plus both integrated-client suites. `scripts/assert-runtime.ps1 -Mode Client` now requires `RING HUD PASSED`.
- `build/hud-ring-datapacks.log`: dedicated startup/reload and integrated live datapack reloads with the new HUD on both loaders.

Reproduce with the existing disposable worlds:

```powershell
.\gradlew.bat build :fabric:runClient :neoforge:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke "-PlifecycleClientWorld=New World" --offline --console=plain
.\gradlew.bat :fabric:runServer :neoforge:runServer :fabric:runClient :neoforge:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PdatapackSmoke "-PlifecycleClientWorld=New World" --offline --console=plain
```

### Final ring compatibility acceptance

The remaining HUD plan checks passed on both loaders. `RingCompatibilityScenario` adds five real-game checkpoints to the original 20: a server-sent locator waypoint, a spawned/tamed/saddled horse's jump bar, dismounting back to experience, daylight at 800×600, and night at 1600×900. The day/night cases equip diamond armor and explicitly seed the armor-display and maximum-health attributes, producing armor icons and three rows of hearts. This isolates the layout check from equipment modifier recalculation. World time, window dimensions, and attribute fixtures are restored afterward. Test-only observers verify the actual contextual-bar class, vanilla XP transform, and at most one ring draw per GUI extraction. Screenshots from both loaders were inspected for ring/XP ordering, bar clearance, and light/dark readability.

Separate-process Fabric and NeoForge clients also passed the authority/movement/reconnect suite with the ring active. Each captured an overloaded state before disconnect and the new server state after reconnect (45 weight / 512 capacity), verifying fresh definitions/bonuses, visible ring, offset 7, vanilla XP rendering, and no duplicate draws. Client display preferences and dedicated-server configuration are restored by the harness. The final build, both jar checks, and all **74 common tests** passed. This final acceptance pass required test-harness additions only; the production renderer did not need adjustment.

Final local evidence:

- `build/hud-ring-final-build.log`: clean full build and production-jar checks.
- `build/hud-ring-compat-clients.log`: both packaged integrated suites, with 25 ring checkpoints each. `Client` assertion mode now requires both `RING HUD PASSED` and `RING COMPATIBILITY PASSED`; use this newer log with the current script.
- `build/hud-ring-fabric-mp-server.log` and `build/hud-ring-fabric-mp-client.log`: separate-process Fabric acceptance.
- `build/hud-ring-neoforge-mp-server.log` and `build/hud-ring-neoforge-mp-client.log`: separate-process NeoForge acceptance.
- Each loader's `runs/client/screenshots/ring-{locator,mounted-jump,experience-return,day-small-crowded,night-large-crowded,multiplayer-overloaded,multiplayer-reconnected}.png` contains the added visual evidence.

To repeat multiplayer, launch the server and client commands below in separate terminals, promptly one after the other. Use the existing EULA-accepted disposable servers bound to localhost (Fabric 25575, NeoForge 25576); replace `fabric` with `neoforge` for the second run. Each process stops itself. The server restores its config and operator state; the fixture replaces the test player's inventory.

```powershell
.\gradlew.bat :fabric:runServer -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PauthorityMultiplayer --offline --console=plain
.\gradlew.bat :fabric:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PauthorityMultiplayer -PsmokeMultiplayer --offline --console=plain
```

Validate saved logs with `scripts/assert-runtime.ps1 -Mode MultiplayerServer` and `-Mode MultiplayerClient`; the latter now also requires `MULTIPLAYER RING PASSED`. The integrated-client command above runs all 25 checkpoints. Third-party HUD mods and custom resource packs remain unverified; a replacement ring texture must preserve the current 16×16 interior mask. These exclusions do not leave any agreed baseline HUD-plan steps outstanding.

## RC1 acceptance follow-up

The RC1 pass corrects two verification gaps found after the HUD plan. The scripted calls to `PlayerList.respawn` did not reassign `replacement.connection.player`, a step normally performed by vanilla's respawn packet handler. The listener therefore continued ticking the old entity; manually invoked weight updates hid this mistake. Both respawn paths now update the connection and reset its position, and weight assertions reject a stale connection owner. The crowded-HUD fixtures no longer assign the armor attribute: the real equipped diamond set must synchronize exactly 20 armor points. Both loader runs passed with this correction.

The previous Linux CI failure was `ServerSettingsTest.failedReplacementPreservesTargetAndCleansTemporaryFile`: Linux allowed opening a directory as a reader, then Gson wrapped the read failure as invalid JSON. File reading now occurs before JSON parsing, preserving `IOException` consistently. The tests separately cover a directory at the config path and a failed atomic replacement with temporary-file cleanup. All 75 common tests pass locally; Windows/Linux CI runs against the RC branch.

Use [RC1 verification](RC1_VERIFICATION.md) for the final candidate artifacts, hashes, reproduction commands, and acceptance evidence. Earlier log paths above are historical and may be removed by a clean build.
