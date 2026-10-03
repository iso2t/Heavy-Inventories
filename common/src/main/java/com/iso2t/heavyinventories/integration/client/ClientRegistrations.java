package com.iso2t.heavyinventories.integration.client;

import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.client.HudElement;
import com.iso2t.heavyinventories.api.client.HudIntegration;
import com.iso2t.heavyinventories.api.client.HudRegistration;
import com.iso2t.heavyinventories.integration.RegistrationScope;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public record ClientRegistrations(Map<Identifier, Owner> owners, Map<Identifier, Decoration> decorations, Map<Identifier, Consumer<Optional<PlayerWeightSnapshot>>> playerChanged) {
	public static final ClientRegistrations EMPTY = new ClientRegistrations(Map.of(), Map.of(), Map.of());

	public ClientRegistrations {
		owners = RegistrationScope.freeze(owners);
		decorations = RegistrationScope.freeze(decorations);
		playerChanged = RegistrationScope.freeze(playerChanged);
	}

	public record Owner(HudElement element, int priority, HudIntegration integration) {
		public Owner {
			Objects.requireNonNull(element);
			Objects.requireNonNull(integration);
		}
	}

	public record Decoration(HudElement element, HudRegistration.Phase phase, HudRegistration.Decoration renderer) {
		public Decoration {
			Objects.requireNonNull(element);
			Objects.requireNonNull(phase);
			Objects.requireNonNull(renderer);
		}
	}
}
