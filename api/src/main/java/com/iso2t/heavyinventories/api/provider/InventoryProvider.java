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
	 * Runs each normal server update. Report globally unique slot IDs in the registration's namespace.
	 * Stop if the sink returns false; the shared limit is 4096 slots, including empty slots.
	 * Exceptions and rejected slots make carried weight incomplete. Never start a nested weight calculation.
	 */
	void collect (ServerPlayer player, SlotSink slots);

	@FunctionalInterface
	interface SlotSink {

		/**
		 * The sink copies the stack during this call. Do not retain the sink or call it from another thread.
		 * Never report vanilla slots or storage already counted through a carried container.
		 */
		boolean accept (Identifier slot, ItemStack stack);

	}

}
