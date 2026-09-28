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

Additional modes:

| Option                    | Purpose                                                                                           |
|---------------------------|---------------------------------------------------------------------------------------------------|
| `-PdatapackSmoke`         | Exercise datapack loading, reloads, and conversion.                                               |
| `-PnewWorldHandoff`       | With client datapack tests, exercise new-world resource handoff.                                  |
| `-PallowTestWorldUpgrade` | Allow Minecraft to back up and upgrade an older disposable save.                                  |
| `-PwithoutCloth`          | With packaged NeoForge server tests, omit Cloth Config.                                           |
| `-PauthorityMultiplayer`  | Test separate server/client processes and reconnection.                                           |
| `-PsmokeMultiplayer`      | With packaged multiplayer client tests, connect to localhost:25575 (Fabric) or :25576 (NeoForge). |

For multiplayer tests, start the server with `-PauthorityMultiplayer`, then start the client
with both multiplayer options. Configure the matching server port and local test account
access beforehand. Both processes stop when their assertions finish.
