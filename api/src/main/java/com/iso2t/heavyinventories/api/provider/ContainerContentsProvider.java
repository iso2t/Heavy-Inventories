package com.iso2t.heavyinventories.api.provider;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Supplies one container item's contents. HI counts the shell, count, and nested children itself.
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
		 * Stop supplying contents when false is returned. Stack references are callback-scoped.
		 */
		boolean accept (ItemStack stack);
	}
}
