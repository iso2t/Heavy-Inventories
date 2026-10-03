package com.iso2t.heavyinventories.api.client;

import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Initialization-only client registration; services returned by weights() may be retained.
 */
public interface ClientPluginRegistration {
	ClientWeights weights ();

	HudRegistration hud ();

	/**
	 * Initial state, changed synchronized state, and empty on disconnect; called on the client thread.
	 */
	void onPlayerChanged (Identifier id, Consumer<Optional<PlayerWeightSnapshot>> listener);
}
