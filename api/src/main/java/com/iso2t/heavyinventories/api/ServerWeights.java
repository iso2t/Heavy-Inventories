package com.iso2t.heavyinventories.api;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Server-thread queries. Implementations are supplied by HI through plugin registration.
 */
public interface ServerWeights {

	/**
	 * Base weight of one empty item. A registered recipe fallback is a complete result.
	 */
	WeightResult item (MinecraftServer server, Identifier item);

	/**
	 * Weight of this count and all supported nested contents, using the overworld as container context.
	 * Does not mutate the stack. Use the level overload for dimension-dependent contents.
	 */
	WeightResult stack (MinecraftServer server, ItemStack stack);

	/**
	 * Weight using the supplied level for custom container contents.
	 */
	default WeightResult stack (ServerLevel level, ItemStack stack) {
		return stack(level.getServer(), stack);
	}

	/**
	 * Last completed update; never triggers recursive player calculation.
	 */
	Optional<PlayerWeightSnapshot> player (ServerPlayer player);

	/**
	 * Empty before definitions are ready or when the item is unknown.
	 */
	Optional<WeightSource> source (MinecraftServer server, Identifier item);

	/**
	 * Mark for the next normal server update. Does not immediately read inventories.
	 */
	void invalidate (ServerPlayer player);

}
