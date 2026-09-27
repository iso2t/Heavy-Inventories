package com.iso2t.heavyinventories.api.weight;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.item.Item;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;

/**
 * Legacy per-item cache for API callers; gameplay uses server-owned weight definitions.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WeightCache {

	private static final ConcurrentHashMap<Item, Float> PER_ITEM = new ConcurrentHashMap<>();

	public static float get (Item item, DoubleSupplier compute) {
		return PER_ITEM.computeIfAbsent(item, k -> (float) compute.getAsDouble());
	}

	public static void put (Item item, float weight) {
		PER_ITEM.put(item, weight);
	}

	public static void invalidate (Item item) {
		PER_ITEM.remove(item);
	}

	public static void clearAll () {
		PER_ITEM.clear();
	}
}
