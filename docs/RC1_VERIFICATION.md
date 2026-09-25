# RC1 verification

Candidate: **26.1.0.0-rc.1**, Minecraft 26.1 / Java 25. Local acceptance date: 2026-09-25. Baseline dependency versions are listed in the [guide](GUIDE.md#installation).

## Acceptance gates

- [x] Clean build, 75 common tests (zero failures/errors/skips), and both production-jar verification tasks.
- [x] Deleted `MaterialType.class` and all lifecycle-test classes absent from both distributable jars; metadata reports the RC1 version.
- [x] Both packaged integrated clients: gameplay, inventory/equipment/container calculations, permissions/configuration, movement, respawn/dimension changes, and all 25 HUD checkpoints.
- [x] Real diamond equipment supplies 20 armor points after scripted respawns on both loaders; no armor attribute override. The fixture asserts that the connection owns the replacement player.
- [x] Both separate dedicated-server/client pairs: server authority, live settings edits, movement bonuses, disconnect/reconnect, fresh weight definitions, and ring/XP rendering without duplicate draws.
- [x] Fabric dedicated datapack loading/conversion and integrated live-reload checks.
- [x] NeoForge dedicated datapack loading/conversion and integrated live-reload checks.

Hosted CI is recorded by GitHub for the candidate commit: [RC1 branch build runs](https://github.com/iso2t/Heavy-Inventories/actions?query=branch%3Acodex%2Frc1). Both Windows and Linux must pass before accepting the candidate. CI performs a clean build, common tests, jar checks, and runtime-harness compilation; game launches are covered by the local runs above. These checks do not publish a release.

## Local artifacts

Only these two jars are distributable. The `common`, sources, javadoc, and lifecycle-test jars are not installable releases.

| Loader | File | SHA-256 |
| --- | --- | --- |
| Fabric | `heavyinventories-fabric-26.1-26.1.0.0-rc.1.jar` | `3ca114ec7065cf7fd23d92e746508da74b6a23b720f11a32401c0fe941734463` |
| NeoForge | `heavyinventories-neoforge-26.1-26.1.0.0-rc.1.jar` | `c3ca42c3467bbe7b88ea621622f06f9be17872bb0f3c33fe2b31bf6d462c8dcf` |

The local handoff directory is `build/rc1/dist/`, with both jars, `SHA256SUMS.txt`, and release notes. These hashes identify the locally tested jars; independently built CI artifacts may differ by JDK or platform.

## Reproduction

Use disposable local test worlds and the already accepted localhost test servers. The runtime harness replaces the test player's inventory; never target a normal survival save. Server settings, operator status, and temporary datapack fixtures are restored by their scenarios.

```powershell
.\gradlew.bat clean build :fabric:lifecycleTestClasses :neoforge:lifecycleTestClasses -I gradle/lifecycle-smoke.gradle --offline --console=plain
.\gradlew.bat :fabric:runClient :neoforge:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke "-PlifecycleClientWorld=New World" --offline --console=plain
.\gradlew.bat :fabric:runServer :fabric:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PdatapackSmoke "-PlifecycleClientWorld=New World" --offline --console=plain
.\gradlew.bat :neoforge:runServer :neoforge:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PdatapackSmoke "-PlifecycleClientWorld=New World" --offline --console=plain
```

For separate-process multiplayer, promptly start the client in a second terminal after starting the server. Repeat with `neoforge` in place of `fabric`:

```powershell
.\gradlew.bat :fabric:runServer -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PauthorityMultiplayer --offline --console=plain
.\gradlew.bat :fabric:runClient -I gradle/lifecycle-smoke.gradle -PpackagedSmoke -PauthorityMultiplayer -PsmokeMultiplayer --offline --console=plain
```

Evidence is stored locally under `build/rc1/`: `clean-build.log`, `clients.log`, `{fabric,neoforge}-datapacks.log`, and `{fabric,neoforge}-mp-{server,client}.log`. Use `scripts/assert-runtime.ps1` with `Client`, `Datapack`, `DatapackClient`, `MultiplayerServer`, or `MultiplayerClient` as appropriate. Screenshots remain under each loader's `runs/client/screenshots/` directory. Clean builds remove build logs; preserve evidence outside `build/` before repeating one.

## Remaining scope limits

No confirmed gameplay blocker remains from these baseline checks. Longer survival/modpack balance testing is the purpose of the candidate. Third-party storage/movement/HUD integrations, shaders, and custom resource packs remain unverified. See the [release notes](RELEASE_NOTES.md#scope) for the supported scope.
