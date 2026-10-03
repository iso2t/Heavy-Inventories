package com.iso2t.heavyinventories.api.provider;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Owns contents for registered items, replacing their standard container/bundle traversal.
 * HI counts the shell and multiplies shell plus contents by the outer count itself.
 */
@FunctionalInterface
public interface ContainerContentsProvider {
	/**
	 * Runs on the level's game thread. Return false if contents cannot be completely supplied.
	 * Client previews may lack server-only contents and must return false in that case.
	 * Never report the container itself, mutate stacks, or recursively call the weight service.
	 */
	boolean collect (Level level, ItemStack container, ContentsSink contents);

	@FunctionalInterface
	interface ContentsSink {
		/**
		 * Stop supplying contents when false is returned. Custom and standard contents share recursion/work limits.
		 * The sink and stack references are callback-scoped and confined to the callback thread.
		 */
		boolean accept (ItemStack stack);
	}
}
