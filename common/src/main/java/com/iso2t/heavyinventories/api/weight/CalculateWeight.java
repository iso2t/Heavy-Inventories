package com.iso2t.heavyinventories.api.weight;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.server.ServerWeightState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CalculateWeight {

	private CalculateWeight () {
	}

	/**
	 * Main hand is already a hotbar slot. Equipment, cursor, and personal crafting inputs appear once.
	 */
	public static List<ItemStack> carriedStacks (Player player) {
		var stacks = new ArrayList<ItemStack>();
		var inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) stacks.add(inventory.getItem(slot));
		stacks.add(player.containerMenu.getCarried());
		var crafting = player.inventoryMenu.getCraftSlots();
		for (int slot = 0; slot < crafting.getContainerSize(); slot++) stacks.add(crafting.getItem(slot));
		// Crafting-result previews and external container slots are not owned inventory.
		return stacks;
	}

	public static float from (Player player) {
		return from(player, carriedStacks(player));
	}

	public static float from (Player player, List<ItemStack> stacks) {
		if (player.level().isClientSide()) return PlayerHolder.getOrCreate(player).getWeight();
		return StackWeight.total(stacks, ServerWeightState.of(player.level().getServer())::unitWeight).weight();
	}
}
