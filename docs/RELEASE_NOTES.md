# Heavy Inventories 26.1.0.0-rc.1

Release candidate for Minecraft 26.1 on Fabric and NeoForge. Install the matching loader jar on both the client and server. Requires Java 25; see the [guide](GUIDE.md#installation) for the tested dependency versions.

## Included

- Server-authoritative inventory weight, equipment and nested vanilla container contents, and configurable carrying capacity.
- Datapack weight overrides, bundled material defaults, and recipe inference for derived items. Successful reloads update connected players automatically.
- Progressive walking slowdown or slowdown beginning at 90% capacity, with Strength and Bracing/Reinforced/Surefooted support.
- A weight ring behind vanilla XP text, optional numeric display, adjustable client offset, and compact item-weight tooltips.
- Operator configuration, weight reports, and conversion of legacy weight JSON files into reviewable datapacks.

## RC1 fixes and verification

- Configuration filesystem failures remain IO errors on both Linux and Windows, while malformed JSON is rejected separately. A failed replacement preserves the existing target and removes its temporary file.
- Runtime tests now reconnect the server packet listener to the replacement player after scripted respawns, matching vanilla's respawn handler. Crowded-HUD checks require the actual 20 armor points from diamond equipment, without overriding the armor attribute.
- Packaged-jar checks reject deleted classes left behind by incremental builds and verify both loaders' release versions.

The acceptance record and artifact hashes are in [RC1 verification](RC1_VERIFICATION.md). This document does not imply that a release has been published.

## Scope

Weights use believable material differences scaled for Minecraft gameplay; default capacity remains 1,000 pounds. This candidate needs gameplay and balance feedback from testers.

The supported baseline uses vanilla equipment/containers and the dependencies listed in the guide. Custom backpack APIs, Ender Chest storage, third-party movement/HUD mods, shaders, and custom resource packs are outside the verified scope. NeoForge's tested baseline is a beta version. Stamina, capacity training, and dropped-item density are not implemented.

Older Heavy Inventories network peers are unsupported. Legacy weight JSON files do not affect gameplay; the guide explains conversion and datapack installation.
