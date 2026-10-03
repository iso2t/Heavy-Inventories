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
	 * Reports ready/changed state in registration-ID order on the client thread, sampled once per tick.
	 * Tick-only changes are ignored; multiple packets may produce one notification. Replacement player
	 * entities emit fresh state even if the values match. Empty follows loss of readiness or disconnect;
	 * repeated unavailable state emits nothing. Snapshots may contain an incomplete carried weight.
	 * Runtime and linkage failures are isolated and logged at most once per minute per listener/session.
	 * Registrations survive disconnects; clear connection-specific state when receiving empty.
	 */
	void onPlayerChanged (Identifier id, Consumer<Optional<PlayerWeightSnapshot>> listener);
}
