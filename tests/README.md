# Tests

This module contains the test mod used by both Fabric and NeoForge. Its sources live in
`src/main/java`, so a normal Gradle import recognizes them in the IDE. Reload the Gradle
project after pulling this change.

`./gradlew build` compiles the harness and runs the JUnit tests in `common` and `tests`. It does not
launch Minecraft. To compile only the harness, use `./gradlew :tests:classes`.

With the wiki submodule checked out, `./gradlew :tests:compileWikiExamplesJava` also compiles the complete Java examples
from `wiki/API-*.md` against the API jar and Minecraft alone. This task is opt-in so normal builds don't require a wiki
checkout. Generated examples remain under `tests/build` and aren't packaged in the API or loader jars.

The test jar is built under `tests/build/libs`. It is not included in either release jar
or the publishing workflow.

API contract tests live in `src/test/java`. Run them with `:tests:test`. The API module contains only production
contracts;
consumer fixtures and wiki-example compilation belong to this module.

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

The normal server and client suites include API plugin fixtures: discovery, registration lifetime, separate client
initialization, and skipping a missing optional dependency. Singleplayer checks that starting an integrated server
does not register the common plugin again. JUnit covers duplicate IDs, rollback, and invalid registration calls.

The query fixture also compiles against the API jar and Minecraft alone, without HI implementation classes.
Server checks cover startup availability, provenance, nested stacks, calculation limits, immutable snapshots, and
deferred invalidation. Client checks include partial definition tables, revision mismatches, display units, and
game-mode/flight exemptions. Datapack and multiplayer modes exercise retained API services across reloads and
reconnects.

To launch the same plugin classes without HI or its API, run `:fabric:runServer -PapiPluginAbsent` and
`:neoforge:runServer -PapiPluginAbsent`. This builds a separate optional-consumer jar and removes HI from the launch
classpath. The fixture checks that the API is absent and shuts the server down after its first tick.

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

Modes are `Server`, `Client`, `PluginAbsent`, `MultiplayerServer`, `MultiplayerClient`, `Datapack`, and
`DatapackClient`. The task fails on runtime errors, missing completion markers, or an
unfinished Gradle run. Use a separate log for each loader and run. This task reads an
existing log; it does not launch Minecraft or require `-PruntimeTests`.

Soaring checks cover elytra-only application, all four levels, the mitigation cap, equipment removal,
and synchronized glide/rocket behavior in singleplayer and separate-process multiplayer.

## HUD consumer checks

The client lifecycle run also executes `FixtureHud`, compiled against the API jar and Minecraft alone. It exercises
owner selection, independent layouts, decorations, replacement and helper drawing, XP positioning, hidden HUD modes,
GUI scales, incomplete-weight feedback, and graphics-state recovery after callback failures. Expected callback
failures are logged once; the suite then verifies HI's default is restored. Look for `API HUD PASSED` on each loader.
The move, replacement, and calculation-limit cases save `api-hud-*.png` screenshots in the test client's screenshots
folder. The fixture is excluded from production jars.

## Gameplay provider checks

`FixtureProviders` is compiled against the API jar and Minecraft, without HI implementation imports. Dedicated and
singleplayer suites exercise extra slots, in-place mutations, duplicate/vanilla ownership rejection, provider failures,
custom contents inside vanilla containers, cycles, limits, unavailable data, explicit invalidation, fallback refresh,
capacity equipment, and replacement players. Singleplayer also checks synchronized totals, capacity removal, custom
client previews, and incomplete tooltips for server-only contents. Look for `API PROVIDERS SERVER PASSED` and
`API PROVIDERS CLIENT PASSED`.

The separate-process multiplayer suite adds a second reconnect with active extra-slot and capacity providers. The
replacement entity must receive newly evaluated values, without the old entity's contributions. Look for
`API PROVIDERS RECONNECT SERVER PASSED` and `API PROVIDERS RECONNECT CLIENT PASSED`.

## Lifecycle notification checks

The API-only `FixtureNotifications` and `FixtureClientNotifications` consumers check committed query state, first
snapshots, unchanged-tick suppression, immutable previous values, replacement players, deferred invalidation, and
failure isolation. Expected failing listeners log errors; healthy listeners must still run. Normal server/client
suites report `API NOTIFICATIONS SERVER PASSED`, `API NOTIFICATIONS CLIENT PASSED`, and
`API NOTIFICATIONS STOPPED PASSED` as applicable.

Datapack client runs require one ready callback per committed revision, none for rejected reloads, and coherent client
snapshots at all eight checkpoints (`API NOTIFICATIONS RELOAD PASSED`). Separate-process multiplayer checks initial
readiness and two disconnect/reconnect cycles (`API NOTIFICATIONS RECONNECT PASSED`). A transition to unavailable
client state reports `API NOTIFICATIONS CLIENT UNAVAILABLE PASSED`.
