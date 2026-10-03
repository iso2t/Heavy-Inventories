package com.iso2t.heavyinventories.api.provider;

import net.minecraft.server.level.ServerPlayer;

/**
 * Server-thread contribution rebuilt from current conditions, added after HI's existing bonuses.
 */
@FunctionalInterface
public interface CapacityProvider {
	/**
	 * Finite, nonnegative pounds. Return zero while the contribution is inactive.
	 */
	double bonus (ServerPlayer player);
}
