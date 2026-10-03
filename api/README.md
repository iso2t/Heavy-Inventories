# Heavy Inventories API

This module contains the integration contracts for Minecraft 26.3. It is currently under development: plugin discovery,
query adapters, provider execution, and HUD dispatch are not implemented yet. The interfaces are not a working
registration system until those implementation steps land.

The module uses the root project's version and Java 25 toolchain. It compiles against Minecraft without depending on
HI's common implementation, a loader API, or EasyConfig. Both HI loader jars compile these same sources into their own
output; consumers must not bundle another copy.

## Entry points

`HeavyInventoriesPlugin` registers common integrations. `HeavyInventoriesClientPlugin` is a separate physical-client
entry point. Each has a namespaced ID and receives an initialization-only registrar. Query services returned by the
registrar may be retained; player, world, and render-context references must not be retained beyond their documented
lifetime.

The planned discovery contract uses `@HIPlugin` on NeoForge and named `heavyinventories` / `heavyinventories_client`
entrypoints on Fabric. Client-only plugin classes use `@HIPlugin(HIPlugin.Side.CLIENT)` on NeoForge. Optional
integrations must keep API references out of their main mod entrypoint so their mod can start without HI.

## Weight contracts

All weights and capacity bonuses are in pounds before display conversion or rounding. `WeightResult` distinguishes a
complete value, an unknown item, unavailable state, and an incomplete calculation. Only complete results contain a
numeric weight. A registered item using the 0.1-pound recipe fallback is still a complete result.

`ServerWeights` takes an explicit server or server player and belongs on that server's thread. `ClientWeights` reads the
current connection on the client thread. `PlayerWeightSnapshot` contains the last completed update; reading it must not
trigger another calculation. An empty snapshot means no synchronized/initialized player state. Incomplete contents have
no numeric total or invented ratio.

Provider contracts cover additional owned inventory slots, exclusive custom-container contents, and additive nonnegative
capacity bonuses. Datapacks remain responsible for fixed item definitions. Notifications describe committed state and
run on its owning game thread.

## HUD contracts

Client APIs are isolated under `api.client`. A HUD integration can own a ring or numeric display layout and optionally
replace its drawing. Decorative callbacks run around the selected owner. Ownership is resolved by user preference,
descending priority, then stable ID.

`HudContext` is immutable frame data in GUI pixels. Only the ring's layout may move the XP number. Hidden layouts have
zero XP offset. `HudDrawing` provides default drawing and direct ring/text helpers without redispatching callbacks.
These contracts will be connected to the existing renderers in the HUD implementation step.

## Verification

Run `:api:check` with the Gradle wrapper for result/layout validation, dependency-boundary checks, and compilation of
separate consumer sources against the API jar. Those consumer classes are not included in the API or loader artifacts.

The loader jar checks require every API source class and reject duplicate class entries. Dedicated-server runtime tests
also resolve common API signatures from the packaged mod. See `tests/README.md` for those opt-in runs.

Maven publication will be configured separately once the runtime integration is usable.
