# Heavy Inventories

Heavy Inventories adds carrying weight and encumbrance to Minecraft for RPG-style inventory decisions. Heavier loads reduce mobility; Strength and enchanted armor increase what you can carry.

This checkout targets **Minecraft 26.1 and Java 25**, with separate **Fabric** and **NeoForge** builds. Legacy Forge is not supported by this version.

## Installation

Use the jar for your loader on both the client and server, with matching Heavy Inventories builds.

| Component | Verified version |
| --- | --- |
| Minecraft | 26.1 |
| Java | 25 |
| Fabric Loader | 0.18.5 |
| Fabric API | 0.144.0+26.1 |
| NeoForge | 26.1.0.1-beta |
| Cloth Config | 26.1.154, for the matching loader |

Fabric requires Fabric API and Cloth Config. NeoForge requires Cloth Config on clients; its dedicated server does not require the settings UI. These are external dependencies, not bundled copies. The version table records the tested baseline, not a claim that every newer loader/mod version works.

## Weight and capacity

Gameplay resolves **datapack weights → supported recipes → a 0.1-pound fallback** on server startup. Fixed datapack values are anchors: ingredient changes propagate through recipe chains, while explicit output weights stay fixed. Recipes account for output counts and use the lowest valid alternative, with bounded cycle handling. Curated material defaults are still planned; these estimates are not yet a realistic material-weight model.

Carried weight includes:

- Main inventory, offhand, and equipped armor, once each at full weight.
- The cursor stack and the player's personal 2×2 crafting inputs.
- The item's own weight plus nested vanilla container and bundle contents.

External chest inventories, Ender Chest storage, and custom backpack/storage APIs are not counted. Modded containers using Minecraft's standard `CONTAINER` or `BUNDLE_CONTENTS` item components use the same calculation, but third-party integrations have not been verified.

Default base capacity is **1000 pounds**. Bonuses add percentages of that base:

| Source | Bonus |
| --- | --- |
| Strength | +10% per effect level; Strength II adds +20% |
| Bracing, on chest armor | +10% per level, through level X (+100%) |
| Reinforced, on leggings | +5% per level, through level V (+25%) |

For example, base 1000 + Bracing X + Reinforced V + Strength II gives capacity **2450**. Effects and equipment are read again each server tick; removed equipment/effects leave no stored bonus.

Pounds are the stored unit. The local display preference can convert numbers to kilograms (`pounds × 0.45359237`) or show raw values without a suffix. This never changes gameplay, command inputs, or percentages.

Displays use up to two decimal places, without trailing zeros. Values below 0.01 use one meaningful digit: `0.053125` displays as `0.05`, while `0.0053125` displays as `0.005`. Positive values below 0.000001 display as `<0.000001`. Formatting happens after unit conversion; stack totals use the underlying weights before rounding.

## Encumbrance

The server compares carried weight with effective capacity:

| Load | State |
| --- | --- |
| Below 90% | Below the encumbrance threshold |
| 90% to below 100% | Encumbered |
| 100% and above | Over encumbered |

The server chooses one walking mode:

- **Progressive**, the default: horizontal input scales by `sqrt(max(0, 1 - weight/capacity))`. At half capacity the multiplier is about 71%; at 90% it is about 32%.
- **Begin at 90% capacity**: full horizontal input through 90%, then a linear decrease to zero at 100%.

Surefooted on boots provides minimum walking multipliers:

| Level | Encumbered floor | Overloaded floor |
| --- | --- | --- |
| I | 25% | 5% |
| II | 25% | 10% |
| III | 30% | 15% |
| IV | 40% | 20% |

Both encumbered states prevent ground jumping. Encumbered/overloaded horizontal swimming input is 75%/50%, sinking gravity is multiplied by 1.5/3, and fall damage by 1.5/3. Surefooted changes walking floors only.

Creative and spectator players are exempt. Ability flight, gliding, and riding bypass movement penalties. Weight still displays when appropriate. The mod scales normalized horizontal input, preserving vanilla momentum, knockback, vertical input, and movement modifiers; these are not absolute speed limits or an anti-cheat system.

The optional bottom-right HUD shows weight, effective capacity, percentage, and encumbrance status. Colors change at the actual 90% and 100% thresholds. Ground-jump denial appears briefly in the action bar, throttled to once per 40 client ticks. Item tooltips include current stack weight; hold Shift for maximum-stack weight.

## Server configuration

Paths are relative to the server's game directory, or the Minecraft instance directory in singleplayer. Singleplayer files are shared by worlds launched from that instance.

Create or edit `config/heavyinventories-server.json`:

```json
{
  "startingWeight": 1000.0,
  "walkingMode": "progressive"
}
```

Use `"at_ninety_percent"` for the alternative walking mode. Older files without `walkingMode` default to progressive. Capacity must be finite, greater than zero, and no greater than 1,000,000,000.

Operators can also open `/heavyinventories config server` and edit both values. Saving sends a validated request to the server; accepted changes are written before they apply. Non-operators can view the server screen. Singleplayer requires command permission for server edits.

Client display preferences and colors are available through `/heavyinventories config client` and stored in `config/heavyinventories-client.json`.

### Item weights in datapacks

Item weights are world-scoped server data; clients receive the resolved table. In an enabled datapack with valid `pack.mcmeta` metadata for your Minecraft version, add:

```text
data/minecraft/heavyinventories/weights/feather.json
```

```json
{ "weight": 0.02 }
```

This is an example override, not a bundled balance value. The resource namespace and path identify the item. A block uses its inventory item's ID. Each weight describes one empty item in pounds; counts and container contents are added separately.

The highest-priority pack replaces the whole resource. Use `{"infer": true}` to remove a lower pack's fixed value and allow recipe inference, then fallback. Deleting an override reveals the lower-priority resource again. Explicit zero remains fixed.

Definitions must contain exactly one supported field. Duplicate/unknown fields, malformed JSON, numeric strings, negative/non-finite values, values above 1,000,000,000, and positive values too small for float storage are rejected. Valid definitions for unregistered items are ignored with a warning.

**Current development-stage reload behavior:** restart the world/server to apply pack edits, or run Minecraft's `/reload`, wait for completion, then run `/heavyinventories reload` to rebuild gameplay weights from the newly loaded resources. Automatic adoption after vanilla reload is the next implementation step. The mod command alone reloads server settings and rebuilds from already loaded datapacks/recipes; it does not reread edited pack files.

Invalid weight candidates retain the active table during explicit rebuilds. Invalid weight data at first startup produces a complete fallback-only table and an error log. Invalid server settings at startup use default settings independently.

Legacy `weights/*.json` files are preserved but **no longer read for gameplay**. Their presence produces a migration warning. A one-time conversion tool is planned; for now, move selected entries into ordinary per-item datapack resources.

### Commands

| Command | Behavior |
| --- | --- |
| `/heavyinventories reload` | Operator: reload settings and rebuild weights from currently loaded datapacks/recipes |
| `/heavyinventories reload weight` | Alias for the full reload |
| `/heavyinventories reload players` | Operator: refresh player totals on the next tick |
| `/heavyinventories dump <namespace>` | Operator: export active gameplay weights to a unique file in `weight-exports/` |
| `/heavyinventories config client` | Open local display preferences |
| `/heavyinventories config server` | View server settings; editing requires permission |
| `/heavyinventories config common` | Reserved screen; there are currently no common settings |

**Dump does not change gameplay.** It is a review file, not an installable datapack. Copy only deliberate fixed values into per-item resources: making every inferred result explicit would prevent ingredient changes from propagating. Export provenance and legacy conversion are planned in the command/migration step.

## Limits and planned work

- Datapack gameplay resolution is active at startup and explicit rebuilds on both loaders. Automatic vanilla-reload adoption, legacy conversion, export provenance, and bundled material defaults remain planned. See [the datapack checks](docs/TESTING.md#datapack-gameplay-resolution).
- Recipe inference supports vanilla shaped/shapeless crafting, smelting, blasting, smoking, campfire cooking, and stonecutting with static outputs. Custom/dynamic recipes, component-dependent results, and crafting remainders are excluded; explicit datapack values cover exceptions.
- This version does not implement a stamina system, carrying-capacity training, or a supported dropped-item density mechanic.
- Custom backpack/Ender storage, third-party movement mods, and resource-pack compatibility have not been verified.
- Nested-content work is bounded to depth 16 and 4096 visited entries. Exceeding a limit marks the load over capacity and displays a calculation-limit message.
- Enchantment bonuses use the three supplied enchantment identities and their equipment slots. Legacy effect codecs still decode old data but no longer execute tick effects; datapacks reusing them for other enchantments or conditional effects need redesign.
- Clients require matching updated packet formats; this version does not support older Heavy Inventories network peers.
- NeoForge is pinned to a beta baseline. See the recorded checks and limits in [testing documentation](docs/TESTING.md).

## Building and contributing

Use JDK 25 and the included Gradle 9.5.0 wrapper:

```sh
# Linux/macOS
bash ./gradlew clean build

# Windows PowerShell
.\gradlew.bat clean build
```

The build runs common regression tests and checks metadata, mixins, services, enchantment resources, and test-harness exclusion in both loader jars. Distributable jars are under `fabric/build/libs/` and `neoforge/build/libs/`; do not install sources, javadoc, or lifecycle-test jars.

The GitHub Actions workflow builds/tests on Linux and Windows and uploads reports and mod artifacts. It does not publish releases or start Minecraft. [docs/TESTING.md](docs/TESTING.md) describes opt-in local runtime checks and their evidence.

Source and issue tracking: [SuperScary/Heavy-Inventories](https://github.com/SuperScary/Heavy-Inventories). Contributions and translations are welcome. Licensed under [MIT](LICENSE.md).
