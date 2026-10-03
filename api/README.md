# Heavy Inventories API

This module contains the integration contracts for Minecraft 26.3. Plugin discovery and registration work on both
loaders. Registrars supply weight queries, HUD hooks, gameplay providers, and lifecycle notifications.

The [developer wiki](../wiki/Developers.md) provides setup instructions and complete integration examples.
With the wiki checked out, `:api:compileWikiExamplesJava` compiles its Java examples against this API jar and Minecraft.

The module uses the root project's version and Java 25 toolchain. It compiles against Minecraft without depending on
HI's common implementation, a loader API, or EasyConfig. Both HI loader jars compile these same sources into their own
output; consumers must not bundle another copy.

## Entry points

`HeavyInventoriesPlugin` registers common integrations. `HeavyInventoriesClientPlugin` is a separate physical-client
entry point. Each has a namespaced ID and receives an initialization-only registrar. Query services returned by the
registrar may be retained; player, world, and render-context references must not be retained beyond their documented
lifetime.

NeoForge discovers public plugin classes through `@HIPlugin`. Fabric uses named `heavyinventories` /
`heavyinventories_client` entrypoints whose values are fully qualified class names with public no-argument constructors.
Method/field entrypoint definitions are not supported. The annotation alone does not register a
Fabric entrypoint. Client plugins use `@HIPlugin(HIPlugin.Side.CLIENT)`; their classes are never loaded on a dedicated
server. An annotation on a Fabric entrypoint must agree with that entrypoint's side.

Use `@HIPlugin(requires = "another_mod")` for an integration that also needs another mod. HI reads this metadata on
either loader before loading the class. Every listed mod must be installed. This does not check dependency versions;
declare those constraints in loader metadata. To make HI itself optional, keep HI API references out of the main mod
entrypoint, do not mark HI as required in loader metadata, and do not bundle the API in your jar.

Plugin IDs must use the owning mod's namespace. Registration IDs use that same namespace and are unique within each
category across all plugins. Plugins register in ID order. Duplicate plugin IDs and container ownership are rejected.
Registration runs once per game process, before gameplay, and finishes when `register` returns. Retaining a registrar
or registering from another thread is an error. Retain the returned query services instead; call them later on their
documented game thread, not during loading.

A gameplay registration failure stops startup and reports the responsible plugin. A client registration failure
disables that plugin. Failed registrations leave no partial hooks. Duplicate client plugin IDs disable all plugins
sharing that ID.

## Weight contracts

All weights and capacity bonuses are in pounds before display conversion or rounding. `WeightResult` distinguishes a
complete value, an unknown item, unavailable state, and an incomplete calculation. Only complete results contain a
numeric weight. A registered item using the 0.1-pound recipe fallback is still a complete result.

`ServerWeights` takes an explicit server or server player and belongs on that server's thread. `ClientWeights` reads the
current connection on the client thread. `PlayerWeightSnapshot` contains the last completed update; reading it must not
trigger another calculation. An empty snapshot means no synchronized/initialized player state. Incomplete contents have
no numeric total or invented ratio. Snapshot ticks use the local level's game time: calculation time on the server,
receipt time on the client. They are not a shared server/client clock. Client reads require matching player/table
revisions and become unavailable when the connection closes.

Client snapshots apply the current local game-mode and movement exemptions when read. Entering flight or Creative
mode does not require another weight packet to correct the reported walking multiplier or effect eligibility.
This changes only the presentation of the snapshot; its weight, capacity, revision, and receipt tick stay intact.

`invalidate` marks a server player's cached inventory for the next normal update. Neither invalidation nor a player
query performs another calculation. A newly created or removed player has no available snapshot. A retained snapshot
stays immutable across later inventory changes, reloads, and replacement players.

## Inventory and capacity providers

Register providers during `HeavyInventoriesPlugin.register`. Their IDs use your mod's namespace. HI invokes gameplay
providers on the server thread, and the client receives the resulting carried weight and effective capacity.

`registration.inventory(id, provider)` adds player-owned slots beyond vanilla inventory, equipment, cursor, and personal
crafting inputs. Call the supplied sink for each slot, using a stable namespaced slot ID such as `your_mod:belt/left`.
Report empty slots consistently and stop when the sink returns false. Slot IDs are unique across inventory providers,
not just within one callback. The sink copies each stack immediately; it cannot be retained or used on another thread.

Do not report vanilla slots, a crafting-result preview, external chest storage, or contents already counted through a
carried container. HI rejects duplicate slot IDs, foreign namespaces, and shared stack references, including aliases of
vanilla stacks. Providers still own the logical mapping: HI cannot recognize that two separately copied stacks refer
to the same external storage. Each storage location must have one owner.

HI compares additional slots during normal player updates. Counts, components, additions, removals, and slot IDs can
invalidate the cached total. For external contents that change without changing a stack's components, retain the
registrar's `ServerWeights` service and call `invalidate(player)` after the change. The next normal update recalculates;
an unchanged inventory also refreshes every 20 ticks. Invalidation never calls providers recursively.

`registration.container(id, itemIds, provider)` owns contents for the listed items. The provider replaces standard
container/bundle traversal for those items, so include all their contents in your callback. Unregistered items keep
Minecraft's normal container-component behavior. HI counts the empty shell and multiplies shell plus contents by the
outer stack count. Supply contents for one container, without multiplying them yourself or including the container.
Only one provider may claim an item; conflicting registrations stop startup.

Custom and standard contents share a depth limit of 16 and a traversal budget of 4,096 visits. Inventory providers can
report at most 4,096 slots in total, including empty slots. Null/duplicate/invalid slots, cycles, exhausted budgets,
overflow, or provider exceptions make the result incomplete rather than publishing a partial or zero weight.
Container sinks also expire when their callback returns. Never start another weight calculation inside a provider;
reading the last completed player snapshot and requesting later invalidation are allowed.

Container callbacks receive the actual level. `ServerWeights.stack(server, stack)` uses the overworld; use
`stack(serverLevel, stack)` when contents depend on dimension. Client queries and tooltips use the current client level.
Return `false` when contents cannot be supplied completely, including when the client lacks server-only data. This
produces an incomplete preview; the carried HUD total still comes from the server. HI does not synchronize a provider's
private inventory data. In singleplayer, the same registered container provider can run on both the client and server
threads. Use the supplied level, keep side-specific data separate, and do not retain world/player references.

`registration.capacity(id, provider)` adds pounds after HI's base, Strength, and enchantment bonuses. For example:

```java
registration.capacity(Identifier.fromNamespaceAndPath("your_mod", "chestplate_bonus"), player ->
    player.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE) ? 125 : 0);
```

Capacity providers run each server update. Return a finite, nonnegative value, or zero while inactive. Invalid or
failing
contributions add nothing; other valid contributions still apply. Contributions that would overflow the effective
capacity are rejected. Failure logs identify the registration and are limited to once per minute per provider/category.
All existing encumbrance thresholds use the resulting capacity. Bonuses and cached extra inventories belong to the
player entity and are rebuilt after respawn or reconnect; HI does not copy or persist them.

The [runtime provider fixture](../tests/src/main/java/com/iso2t/heavyinventories/test/plugin/FixtureProviders.java)
compiles against the API alone and exercises all three provider types. Datapacks remain responsible for fixed item
weights; providers do not replace that definition system.

## Lifecycle notifications

Register callbacks once, during plugin initialization. HI invokes listeners in registration-ID order. The common
callbacks run on the owning server thread:

- `onWeightsReady` receives the server and committed revision, including initial readiness and successful datapack
  or settings changes. Item queries already see the new table. Player updates follow this notification. A rejected
  reload leaves the existing state in place and emits no ready event.
- `onPlayerChanged` receives the player, the last delivered snapshot, and the newly committed snapshot. The previous
  value is empty on a new player entity, including after respawn or reconnect. Changes to values or revision trigger
  it; advancing the snapshot tick alone does not. Querying that player inside the callback returns the new snapshot.
- `onServerStopped` runs once after normal server shutdown. Queries are unavailable by then. Release any world or
  player references your integration owns.

For example, retain the query service and request a later inventory refresh after a definition change:

```java
var weights = registration.weights();
registration.onWeightsReady(Identifier.fromNamespaceAndPath("your_mod", "refresh"), (server, revision) ->
    server.getPlayerList().getPlayers().forEach(weights::invalidate));
```

Invalidation inside a listener schedules the next normal update. Queries read committed state without recalculating.
Starting another HI player update or committing another weight table during notification dispatch is rejected.
Callbacks cannot cancel or rewrite the completed update.

Client `onPlayerChanged` receives an `Optional<PlayerWeightSnapshot>` on the client thread. HI samples the current
synchronized state once per client tick, so several packets can produce one notification. Values, revisions, local
game-mode/flight exemptions, and replacement player entities can trigger it. Tick-only changes do not.
An empty value follows loss of readiness, including mismatched table/player revisions, or a disconnect. Repeated
unavailable state produces no event. Clear any connection-specific display values on empty, and use the next present
snapshot when ready. A present snapshot can still report an incomplete carried weight.

Registrations last for the game process; do not register again when a world opens. HI clears session notification
state on shutdown/disconnect. Immutable snapshots may be kept for comparison, but must not be presented as current
after their session ends. A listener's runtime or linkage failure does not stop the remaining listeners or undo
committed state. The listener remains registered; errors are logged at most once per minute per listener/category
within a session.

## HUD contracts

Client APIs are isolated under `api.client`. Register an owner through `registrar.hud().owner(...)` for
`HudElement.RING` or `HudElement.NUMBERS`. Each element has one owner. A layout callback runs at most once per element
per GUI frame; the resulting ring layout is also used by vanilla XP positioning.

For example, this owner moves the existing ring to the top-left and leaves the XP number in its vanilla position:

```java
registrar.hud().owner(Identifier.fromNamespaceAndPath("your_mod", "ring"), HudElement.RING, 0,
    new HudIntegration() {
        @Override
        public HudLayout layout(HudContext context, HudElement element, HudLayout original) {
            return new HudLayout(new HudBounds(12, 12, 16, 16), true, 0);
        }
    });
```

`HudContext` contains the shared immutable snapshot, GUI dimensions, selected display mode, and HI's original layouts.
The layout passed to `render` is the selected owner's result. Coordinates are GUI pixels. The default ring remains
16×16; changing its bounds does not scale the native artwork. The default numeric display starts at the layout's top
edge and aligns to its right edge.

Only the ring layout can set an XP offset; positive values move the number upward. Return a hidden layout with zero
offset to hide the ring and leave XP alone. Override `render` to draw a replacement, or call `drawing.drawDefault()`
once to use HI's artwork at the resolved position. Omitting that call suppresses built-in drawing but does not change
visibility or the XP offset. The direct `drawing.ring(...)` and `drawing.numbers(...)` helpers never dispatch hooks.
Use the supplied graphics object and helpers only during that callback, on its thread.

Players can choose owners independently with **Ring renderer** and **Weight readout renderer** in client settings:

- Blank selects the highest priority, with registration ID in ascending order breaking ties.
- A namespaced registration ID selects that exact owner.
- `heavyinventories:default` selects HI's original layout and drawing.
- An unavailable or disabled selection falls back to HI, without promoting another owner.

The TOML fields are `ringowner` and `numbersowner`. Selection affects owners; decorations still run around a visible
element. Register decorations with `decorate(...)` and `Phase.BEFORE` or `Phase.AFTER`. Each phase runs in ascending
registration-ID order, relative to that HI element. Both loaders expose the layer IDs `heavyinventories:weight_ring`
and `heavyinventories:weight`. These controls do not detect overlaps or arrange unrelated mods' HUD layers.

Overlay-off, F1, Creative/Spectator, open screens, and unavailable synchronized state suppress HI callbacks. The
selected Ring/Numbers/Both mode still applies. Calculation-limit text remains available in Ring mode when a plugin
replaces only the ring; the numeric owner is responsible for preserving it if that element is replaced too.

HI isolates the pose and scissor stacks around each render callback. Hooks cannot pop the caller's scissor. Runtime
or linkage failures are logged once and disable that owner or decoration until restart. A failed layout uses HI's
default immediately; a failed render restores the default on subsequent frames. Already-submitted drawing in the
failed frame cannot be undone, so HI does not draw another copy over it. Hooks must still restore any other graphics
state they change and must not retain graphics references.

## Verification

Run `:api:check` with the Gradle wrapper for result/layout validation, dependency-boundary checks, and compilation of
separate consumer sources against the API jar. Those consumer classes are not included in the API or loader artifacts.

The loader jar checks require every API source class and reject duplicate class entries. Dedicated-server runtime tests
also resolve common API signatures from the packaged mod. See `tests/README.md` for those opt-in runs.

`:tests:compileApiConsumerJava` compiles the runtime query fixture against the API jar and Minecraft alone. The fixture
checks nested stacks, calculation limits, provenance, snapshots, and thread ownership in actual loader runs.

Maven publication will be configured separately once the runtime integration is usable.
