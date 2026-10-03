package com.iso2t.heavyinventories.integration.client;

import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.client.*;
import com.iso2t.heavyinventories.integration.RegistrationScope;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

final class ClientRegistrar implements ClientPluginRegistration, HudRegistration, AutoCloseable {
	private final RegistrationScope                                         scope;
	private final ClientWeights                                             weights;
	private final Map<Identifier, ClientRegistrations.Owner>                owners        = new HashMap<>();
	private final Map<Identifier, ClientRegistrations.Decoration>           decorations   = new HashMap<>();
	private final Map<Identifier, Consumer<Optional<PlayerWeightSnapshot>>> playerChanged = new HashMap<>();

	ClientRegistrar (Identifier plugin, ClientWeights weights) {
		scope = new RegistrationScope(plugin);
		this.weights = weights;
	}

	@Override
	public ClientWeights weights () {
		scope.check();
		return weights;
	}

	@Override
	public HudRegistration hud () {
		scope.check();
		return this;
	}

	@Override
	public void onPlayerChanged (Identifier id, Consumer<Optional<PlayerWeightSnapshot>> listener) {
		scope.add(playerChanged, id, listener);
	}

	@Override
	public void owner (Identifier id, HudElement element, int priority, HudIntegration integration) {
		scope.run(() -> scope.add(owners, id, new ClientRegistrations.Owner(element, priority, integration)));
	}

	@Override
	public void decorate (Identifier id, HudElement element, Phase phase, Decoration decoration) {
		scope.run(() -> scope.add(decorations, id, new ClientRegistrations.Decoration(element, phase, decoration)));
	}

	ClientRegistrations finish () {
		scope.finish();
		return new ClientRegistrations(owners, decorations, playerChanged);
	}

	@Override
	public void close () {
		scope.close();
	}
}
