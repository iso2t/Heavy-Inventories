# Heavy Inventories guide

Installation, gameplay rules, configuration, and supported limits for the current codebase. For the project overview, see the [README](../README.md).

## Installation

Use the jar for your loader on both the client and server, with matching Heavy Inventories builds.

| Component     | Verified version                  |
|---------------|-----------------------------------|
| Minecraft     | 26.1                              |
| Java          | 25                                |
| Fabric Loader | 0.18.5                            |
| Fabric API    | 0.144.0+26.1                      |
| NeoForge      | 26.1.0.1-beta                     |
| Cloth Config  | 26.1.154, for the matching loader |

Fabric requires Fabric API and Cloth Config. NeoForge requires Cloth Config on clients; its dedicated server does not require the settings UI. These are external dependencies, not bundled copies. The version table records the tested baseline, not a claim that every newer loader/mod version works.

## Weight and capacity

Gameplay resolves **datapack weights → supported recipes → a 0.1-pound fallback** on server startup. Fixed datapack values are anchors: ingredient changes propagate through recipe chains, while explicit output weights stay fixed. Recipes account for output counts and use the lowest valid alternative, with bounded cycle handling. Both loader jars include curated material defaults. The system uses believable material differences with gameplay-scaled weights.

Carried weight includes:

- Main inventory, offhand, and equipped armor, once each at full weight.
- The cursor stack and the player's personal 2×2 crafting inputs.
- The item's own weight plus nested vanilla container and bundle contents.

External chest inventories, Ender Chest storage, and custom backpack/storage APIs are not counted. Modded containers using Minecraft's standard `CONTAINER` or `BUNDLE_CONTENTS` item components use the same calculation, but third-party integrations have not been verified.

Default base capacity is **1000 pounds**. Bonuses add percentages of that base:

| Source                  | Bonus                                        |
|-------------------------|----------------------------------------------|
| Strength                | +10% per effect level; Strength II adds +20% |
| Bracing, on chest armor | +10% per level, through level X (+100%)      |
| Reinforced, on leggings | +5% per level, through level V (+25%)        |

For example, base 1000 + Bracing X + Reinforced V + Strength II gives capacity **2450**. Effects and equipment are read again each server tick; removed equipment/effects leave no stored bonus.

Pounds are the stored unit. The local display preference can convert numbers to kilograms (`pounds × 0.45359237`) or show raw values without a suffix. This never changes gameplay, command inputs, or percentages.

Displays use up to two decimal places, without trailing zeros. Values below 0.01 use one meaningful digit: `0.053125` displays as `0.05`, while `0.0053125` displays as `0.005`. Positive values below 0.000001 display as `<0.000001`. Formatting happens after unit conversion; stack totals use the underlying weights before rounding.

## Encumbrance

The server compares carried weight with effective capacity:

| Load              | State                           |
|-------------------|---------------------------------|
| Below 90%         | Below the encumbrance threshold |
| 90% to below 100% | Encumbered                      |
| 100% and above    | Over encumbered                 |

The server chooses one walking mode:

- **Progressive**, the default: horizontal input scales by `sqrt(max(0, 1 - weight/capacity))`. At half capacity the multiplier is about 71%; at 90% it is about 32%.
- **Begin at 90% capacity**: full horizontal input through 90%, then a linear decrease to zero at 100%.

Surefooted on boots provides minimum walking multipliers:

| Level | Encumbered floor | Overloaded floor |
|-------|------------------|------------------|
| I     | 25%              | 5%               |
| II    | 25%              | 10%              |
| III   | 30%              | 15%              |
| IV    | 40%              | 20%              |

Both encumbered states prevent ground jumping. Encumbered/overloaded horizontal swimming input is 75%/50%, sinking gravity is multiplied by 1.5/3, and fall damage by 1.5/3. Surefooted changes walking floors only.

Creative and spectator players are exempt. Ability flight, gliding, and riding bypass movement penalties. Weight still displays when appropriate. The mod scales normalized horizontal input, preserving vanilla momentum, knockback, vertical input, and movement modifiers; these are not absolute speed limits or an anti-cheat system.

The default weight HUD is a 16×16 ring behind the XP level. Its center fills upward using current weight and effective capacity: green below 90%, yellow from 90% to below 100%, and red at 100% or more. It remains visible at XP level zero. Choose Ring, Numbers, or Ring and numbers in client settings; the numeric display shows weight, capacity, percentage, and status at the bottom right. Disable the GUI overlay to hide both. A calculation-limit warning remains visible even in ring-only mode. Ground-jump denial appears briefly in the action bar, throttled to once per 40 client ticks. Item tooltips include current stack weight; hold Shift for maximum-stack weight.

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

Client display preferences and numeric text colors are available through `/heavyinventories config client` and stored in `config/heavyinventories-client.json`.

**Ring vertical offset** is a client-only whole number from 0 to 64, default **7**. Higher values move the ring and vanilla XP number upward together in GUI pixels; the XP bar stays in place. Zero retains vanilla text placement and may overlap the bar; 12 gives long XP numbers more space. Hiding the ring restores vanilla XP text positioning. The settings screen provides a reset to 7.

The saved fields are `hudMode` (`"ring"`, `"numbers"`, or `"both"`), `ringVerticalOffset`, and the existing `enableGuiOverlay` master toggle. Older settings default to ring mode with offset 7 and preserve an explicit overlay-off preference. These settings never change server gameplay or other players' HUDs.

### Item weights in datapacks

Item weights are world-scoped server data; clients receive the resolved table. Both mod jars include gameplay-scaled material defaults, with ordinary crafted weights inferred from recipes. For example, stone weighs 4, an iron ingot 2, and an arrow 0.09 pounds. See [the default weight scale and exceptions](DEFAULT_WEIGHTS_PROPOSAL.md). In an enabled datapack with valid `pack.mcmeta` metadata for your Minecraft version, add:

```text
data/minecraft/heavyinventories/weights/feather.json
```

```json
{ "weight": 0.02 }
```

This is an example override, not a bundled balance value. The resource namespace and path identify the item. A block uses its inventory item's ID. Each weight describes one empty item in pounds; counts and container contents are added separately.

The highest-priority pack replaces the whole resource. Use `{"infer": true}` to remove a lower pack's fixed value and allow recipe inference, then fallback. Deleting an override reveals the lower-priority resource again. Explicit zero remains fixed.

Definitions must contain exactly one supported field. Duplicate/unknown fields, malformed JSON, numeric strings, negative/non-finite values, values above 1,000,000,000, and positive values too small for float storage are rejected. Valid definitions for unregistered items are ignored with a warning.

Run Minecraft's `/reload` to apply datapack edits. After Minecraft finishes loading recipes and tags, Heavy Inventories automatically rebuilds the complete weight table and updates connected players' inventory totals and tooltips. No reconnect or second command is needed. Vanilla reload retains the active server settings; use `/heavyinventories reload` to reread the server configuration file and rebuild from already loaded datapacks/recipes. The mod command does not reread edited pack files.

Invalid weight candidates retain the previous complete table and revision, and notify operators with the cause. A failed Minecraft resource reload does not apply new weights. This protects Heavy Inventories' state; it does not roll back unrelated data accepted by Minecraft. Invalid weight data at first startup produces a complete fallback-only table and an error log. Invalid server settings at startup use default settings independently.

Legacy `weights/*.json` files are preserved but **no longer read for gameplay**. To convert them, run `/heavyinventories convert legacy <pack_name>` as an operator (or from the server console). This reads only that legacy directory and writes `weight-packs/<pack_name>.zip`; it does not read dump reports, install the ZIP, enable a pack, or change gameplay.

All inputs must pass validation before a ZIP is published. Existing output files are never replaced. Use a name of 1–64 lowercase letters, digits, underscores, or hyphens. Duplicate entries/fields, malformed numbers, unsafe paths, and invalid weights fail conversion. Entries without a weight are counted and skipped; density and other legacy metadata are not converted. Valid IDs for absent mods are preserved as optional definitions.

Review the generated ZIP, then copy it into the intended world's `datapacks` folder. Run `/reload` to discover it, use `/datapack list available` to check its status, and enable it with `/datapack enable "file/<pack_name>.zip" last` if needed. Normal datapack priority applies. The generated metadata targets the server's current Minecraft data-pack version. Keep ordinary derived items unspecified so future ingredient changes can propagate.

### Commands

| Command | Behavior |
| --- | --- |
| `/heavyinventories reload` | Operator: reload settings and rebuild weights from currently loaded datapacks/recipes |
| `/heavyinventories reload weight` | Alias for the full reload |
| `/heavyinventories reload players` | Operator: refresh player totals on the next tick |
| `/heavyinventories dump <namespace>` | Operator: export active weights and their sources to a unique report in `weight-exports/` |
| `/heavyinventories convert legacy <pack_name>` | Operator: convert legacy weight files into a reviewable ZIP in `weight-packs/` |
| `/heavyinventories config client` | Open local display preferences |
| `/heavyinventories config server` | View server settings; editing requires permission |
| `/heavyinventories config common` | Reserved screen; there are currently no common settings |

**Dump does not change gameplay.** Reports identify their format, version, stored unit (`lb`), and active weight revision. The `items` object uses full registry IDs:

```json
{
  "minecraft:arrow": {
    "weight": 0.09,
    "source": "recipe"
  }
}
```

Each item's `source` is `explicit`, `recipe`, or `fallback`. A fixed datapack value of 0.1 is still explicit; it is not classified by comparing the number with the fallback. Winning definitions also include `definition.pack`, `definition.resource`, and `definition.mode` (`weight` or `infer`). Recipe-derived values can depend on fallback ingredients. Development-only session replacements are labeled `session`.

Reports describe the active snapshot even after a failed reload; they are neither legacy converter input nor installable datapacks. Copy only deliberate fixed weights into per-item resources: making every inferred result explicit would freeze recipe chains.

## Limits and planned work

- Datapack gameplay resolution is active at startup and after successful resource reloads on both loaders. Bundled material defaults, legacy conversion, and provenance reports are available. See [the datapack checks](TESTING.md#datapack-gameplay-resolution).
- Recipe inference supports vanilla shaped/shapeless crafting, smelting, blasting, smoking, campfire cooking, and stonecutting with static outputs. Custom/dynamic recipes, component-dependent results, and crafting remainders are excluded; explicit datapack values cover exceptions.
- This version does not implement a stamina system, carrying-capacity training, or a supported dropped-item density mechanic.
- Custom backpack/Ender storage, third-party movement mods, and resource-pack compatibility have not been verified.
- Nested-content work is bounded to depth 16 and 4096 visited entries. Exceeding a limit marks the load over capacity and displays a calculation-limit message.
- Enchantment bonuses use the three supplied enchantment identities and their equipment slots. Legacy effect codecs still decode old data but no longer execute tick effects; datapacks reusing them for other enchantments or conditional effects need redesign.
- Clients require matching updated packet formats; this version does not support older Heavy Inventories network peers.
- NeoForge is pinned to a beta baseline. See the recorded checks and limits in [testing documentation](TESTING.md).
