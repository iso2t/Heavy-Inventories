package com.iso2t.heavyinventories.api.client;

import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.WeightResult;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Client-thread access to the active connection. Never supplies guessed values before synchronization.
 */
public interface ClientWeights {

	WeightResult item (Identifier item);

	/**
	 * May be incomplete if a custom container's contents are not synchronized.
	 */
	WeightResult stack (ItemStack stack);

	Optional<PlayerWeightSnapshot> player ();

	/**
	 * Format finite, nonnegative pounds using the local player's units and HI's rounding rules.
	 */
	String format (double pounds);

}
