# Bundled default weights

Status: implemented; gameplay-scaled weights with believable material relationships.

Heavy Inventories uses realistic relationships without treating each block as a literal cubic meter of material. Mining loads affect travel, while a normal adventuring kit leaves room for loot. Stored weights are expressed in pounds; these are balance values, not physical mass estimates.

The base capacity remains **1,000**. Movement modes, Strength/enchantment bonuses, display conversion, and the 0.1 fallback are unchanged.

## Where defaults live

[The authoring catalog](../common/src/main/weight-defaults.json) lists exact item IDs, weights, and reasons for exceptions. The build generates 549 ordinary per-item datapack resources under `data/<namespace>/heavyinventories/weights/<path>.json` and embeds them in both loader jars. They work on a fresh installation without a separate pack download.

The catalog is a build input, not another runtime configuration layer. Server/world datapacks override these resources through normal pack priority. Ordinary crafted items remain unspecified, so supported recipe changes and ingredient overrides propagate.

## Material scale

| Material | Pounds per item |
| --- | ---: |
| Logs, stripped logs, Nether stems | 2 |
| Bamboo | 0.125 |
| Stone and cobblestone | 4 |
| Deepslate and cobbled deepslate | 5 |
| Obsidian | 6 |
| Dirt, sand, gravel, clay blocks | 3 |
| Raw iron / iron ingot | 2 |
| Raw copper / copper ingot | 2 |
| Raw gold / gold ingot | 4 |
| Diamond | 1.5 |
| Emerald | 1 |
| Netherite ingot | 4 |
| Coal / charcoal | 0.5 |
| Flint | 0.1 |
| Feather | 0.01 |
| String | 0.05 |
| Leather | 1 |
| White wool | 0.2 |
| Sugar cane | 0.1 |
| Dye | 0.02 |
| Raw meat and fish | 0.5 |

The catalog also covers natural blocks, ores, plants, other food, mob drops, and noncraftable equipment. Group labels are for authoring convenience; the runtime schema still targets individual items.

## Deliberate exceptions

- **Processing and cycles:** charcoal has a processing-yield allowance. Bone meal and dried kelp anchor their compression cycles; bone blocks and dried kelp blocks remain inferred. Dyes share a small pigment weight.
- **Containers:** a bucket weighs 6; water/milk add 2, lava adds 4, and powdered snow adds 1. Fish buckets include a content allowance. Dyed bundles and shulker boxes match the empty uncolored container because component-preserving dye recipes are excluded from inference.
- **Contents:** bundles and shulker boxes still add their nested contents. Color never creates a carrying discount.
- **Netherite equipment:** fixed at the reviewed diamond-equipment baseline plus 4. Smithing is excluded from the supported recipe set.
- **Other equipment:** chainmail armor is 3 / 8 / 6 / 3 for helmet/chestplate/leggings/boots; elytra is 4 and trident is 6. The mace derives its 12 from a heavy core of 11 and breeze rod of 1.
- **World transformations:** solid concrete matches the default powder baseline; oxidation preserves the default copper-component mass.
- **Excluded/component-dependent recipes:** honey blocks account for returned glass bottles. Filled maps, written/enchanted books, potions, golden foods, tipped arrows, and firework stars receive deliberate fixed baselines. These values do not dynamically account for potion effects, enchantments, or firework payload components.

These exceptions are fixed data, not new runtime formulas. A pack changing diamond, copper, container, or other ingredient weights should also review related fixed exceptions. Recipe-derived values choose the cheapest supported ingredient/recipe route, not the particular recipe a player used.

## Verified recipe results

These results were checked against the actual loaded Minecraft 26.1 recipes and tags on both loaders.

| Derived item | Pounds |
| --- | ---: |
| Ordinary plank | 0.5 |
| Stick | 0.25 |
| Arrow | 0.09 |
| 64 arrows | 5.76 |
| Iron pickaxe | 6.5 |
| Iron sword | 4.25 |
| Iron chestplate | 16 |
| Full iron armor | 48 |
| Shield | 5 |
| Bow | 0.9 |
| Torch | 0.1875 |
| Furnace | 32 |
| Crafting table | 2 |
| Empty bucket | 6 |
| Empty bundle | 1.05 |
| Empty shulker box | 8 |
| Iron block | 18 |
| Bone block | 0.75 |
| Dried kelp block | 0.45 |

An arrow is now `(0.1 flint + 0.01 feather + 0.25 stick) / 4 = 0.09`. Its earlier 0.053125 value came from fallback-based ingredients; the display formatter is unchanged.

## Example load

The example kit contains full iron armor, an iron pickaxe and sword, a shield, bow, 64 arrows, 32 cooked beef, 64 torches, a crafting table, and a furnace.

| Carried load | Pounds | Base capacity used |
| --- | ---: | ---: |
| Kit | 132.41 | 13.241% |
| Kit + 64 cobblestone | 388.41 | 38.841% |
| Kit + 128 cobblestone | 644.41 | 64.441% |
| Kit + 192 cobblestone | 900.41 | 90.041% |
| Kit + 256 cobblestone | 1156.41 | 115.641% |

Three stacks plus this kit reach encumbrance. Four overload an unmodified player. Strength and armor enchantments increase capacity normally. Progressive walking slowdown begins below 90%; the alternative walking mode begins at 90%.

## Server overrides

In an enabled datapack, `data/minecraft/heavyinventories/weights/iron_ingot.json` containing `{"weight":3}` changes iron's anchor and its inferred descendants. A file containing `{"infer":true}` instead removes the lower pack's fixed anchor and allows recipes, then fallback. Removing the file reveals the bundled value again.

Run Minecraft's `/reload` to apply edits. See [pack creation and validation](../README.md#item-weights-in-datapacks) for metadata, installation, priority, and failure behavior. Clients receive the server's resolved table automatically.

## Coverage and balance limits

The stock 26.1 table contains 1,506 item entries: **549 explicit, 852 recipe-derived, and 105 fallback**. Remaining fallbacks are spawn eggs and creative/admin-only items such as command blocks, barriers, and unobtainable spawner items. Modded items may still use fallback until recipes or another datapack cover them.

The runtime harness checks material/recipe examples, the example kit, survival-item fallback coverage, user overrides, an `infer` override of bundled charcoal, removal/restoration, and synchronized client values. Build checks compare every packaged definition with the source catalog and reject stale resources.

Automated checks establish consistent calculation and delivery. These numbers are an initial gameplay balance and still need ordinary playtesting.
