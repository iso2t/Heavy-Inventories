package com.iso2t.heavyinventories.api.provider;

import net.minecraft.server.level.ServerPlayer;

/**
 * Server-thread contribution rebuilt from current conditions, added after HI's existing bonuses.
 */
@FunctionalInterface
public interface CapacityProvider {

	/**
	 * Finite, nonnegative pounds. Return zero while inactive. Exceptions, invalid values, and contributions
	 * that overflow effective capacity add nothing. Never start another weight calculation inside this callback.
	 */
	double bonus (ServerPlayer player);

}
