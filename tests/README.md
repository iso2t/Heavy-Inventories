# Runtime tests

This module contains the test mod used by both Fabric and NeoForge. Its sources live in
`src/main/java`, so a normal Gradle import recognizes them in the IDE. Reload the Gradle
project after pulling this change.

`./gradlew build` compiles the harness and runs the JUnit tests in `common`. It does not
launch Minecraft. To compile only the harness, use `./gradlew :tests:classes`.

The test jar is built under `tests/build/libs`. It is not included in either release jar
or the publishing workflow.

## Run locally

Use disposable worlds: these tests replace inventories, respawn players, change dimensions,
and temporarily edit settings and permissions. Dedicated servers require a separately
accepted Minecraft EULA. The harness uses the existing `<loader>/runs/client` and
`<loader>/runs/server` directories.

```powershell
.\gradlew.bat :fabric:runServer -PruntimeTests -PpackagedSmoke
.\gradlew.bat :fabric:runClient -PruntimeTests -PpackagedSmoke '-PlifecycleClientWorld=New World'
```

Replace `fabric` with `neoforge` for the other loader. On Linux/macOS, use `./gradlew`.
Without `-PruntimeTests`, the normal client and server tasks do not load the harness.
`-PpackagedSmoke` tests the built production jar under the development launcher.

The gameplay suite includes elytra flight checks using Minecraft's glide update and rocket tick methods. It compares
fixed-pitch, 100-block descents from an initial horizontal speed of one block per tick, checks loaded deployment and
boosts, and exercises config edits, exemptions, and Slow Falling. With the default 15% maximum lift reduction, a level
glide measured approximately 961 blocks at zero load, 846 at 500 pounds, and 735 at 1,000 pounds on both loaders.
These controlled measurements exclude terrain collisions and steering changes; they are not a promised travel distance.

Additional modes:

| Option                    | Purpose                                                                                           |
|---------------------------|---------------------------------------------------------------------------------------------------|
| `-PdatapackSmoke`         | Exercise datapack loading, reloads, and conversion.                                               |
| `-PnewWorldHandoff`       | With client datapack tests, exercise new-world resource handoff.                                  |
| `-PallowTestWorldUpgrade` | Allow Minecraft to back up and upgrade an older disposable save.                                  |
| `-PauthorityMultiplayer`  | Test separate server/client processes and reconnection.                                           |
| `-PsmokeMultiplayer`      | With packaged multiplayer client tests, connect to localhost:25575 (Fabric) or :25576 (NeoForge). |

For multiplayer tests, start the server with `-PauthorityMultiplayer`, then start the client
with both multiplayer options. Configure the matching server port and local test account
access beforehand. Both processes stop when their assertions finish.

## Check a saved log

Save the runtime output, then check it with Gradle:

```powershell
.\gradlew.bat :fabric:runServer -PruntimeTests -PpackagedSmoke *> build/fabric-server.log
.\gradlew.bat :tests:verifyRuntimeLog '-PruntimeLog=build/fabric-server.log' -PruntimeMode=Server
```

On Linux/macOS, use `./gradlew` and redirect with `> build/fabric-server.log 2>&1`.
Log paths are relative to the repository root; absolute
paths also work. UTF-8 and UTF-16 logs are supported.

Modes are `Server`, `Client`, `MultiplayerServer`, `MultiplayerClient`, `Datapack`, and
`DatapackClient`. The task fails on runtime errors, missing completion markers, or an
unfinished Gradle run. Use a separate log for each loader and run. This task reads an
existing log; it does not launch Minecraft or require `-PruntimeTests`.

Soaring checks cover elytra-only application, all four levels, the mitigation cap, equipment removal,
and synchronized glide/rocket behavior in singleplayer and separate-process multiplayer.
