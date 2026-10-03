package com.iso2t.heavyinventories.api.provider;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Additional player-owned slots, excluding vanilla inventory and storage counted inside carried items.
 */
@FunctionalInterface
public interface InventoryProvider {
	/**
	 * Runs on the server thread. Report stable slot IDs and stop if the sink returns false.
	 */
	void collect (ServerPlayer player, SlotSink slots);

	@FunctionalInterface
	interface SlotSink {
		/**
		 * The sink reads the stack during this call; neither party may mutate or retain the other's data.
		 */
		boolean accept (Identifier slot, ItemStack stack);
	}
}
