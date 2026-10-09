package com.iso2t.heavyinventories.integration.client;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.client.ClientWeights;
import com.iso2t.heavyinventories.api.client.HeavyInventoriesClientPlugin;
import com.iso2t.heavyinventories.integration.PluginCandidate;
import com.iso2t.heavyinventories.integration.PluginLoader;
import com.iso2t.heavyinventories.integration.RegistrationScope;

import java.util.List;
import java.util.Objects;

public final class ClientPlugins {

	public static final ClientPlugins       INSTANCE      = new ClientPlugins(new ClientWeightAccess());
	private final       ClientWeights       weights;
	private volatile    ClientRegistrations registrations = ClientRegistrations.EMPTY;
	private             boolean             started;

	public ClientPlugins (ClientWeights weights) {
		this.weights = Objects.requireNonNull(weights);
	}

	public ClientRegistrations registrations () {
		return registrations;
	}

	public synchronized void initialize (List<PluginCandidate<HeavyInventoriesClientPlugin>> candidates) {
		if (started) throw new IllegalStateException("Client plugins already initialized");
		started = true;
		var next = ClientRegistrations.EMPTY;
		for (var loaded : PluginLoader.load(candidates, HeavyInventoriesClientPlugin::id, true)) {
			try (var registrar = new ClientRegistrar(loaded.id(), weights)) {
				loaded.plugin().register(registrar);
				var added = registrar.finish();
				next = new ClientRegistrations(RegistrationScope.combine(next.owners(), added.owners()), RegistrationScope.combine(next.decorations(), added.decorations()), RegistrationScope.combine(next.playerChanged(), added.playerChanged()));
			} catch (RuntimeException | LinkageError e) {
				HeavyInventories.LOGGER.error("Disabled HI client plugin {} ({}) without applying its registrations", loaded.id(), loaded.description(), e);
			}
		}
		registrations = next;
		HeavyInventories.LOGGER.info("Registered HI client plugins");
	}

}
