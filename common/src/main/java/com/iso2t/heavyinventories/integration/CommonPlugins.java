package com.iso2t.heavyinventories.integration;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.ServerWeights;
import com.iso2t.heavyinventories.api.plugin.HeavyInventoriesPlugin;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.iso2t.heavyinventories.integration.RegistrationScope.combine;

public final class CommonPlugins {

	public static final CommonPlugins       INSTANCE      = new CommonPlugins(new ServerWeightAccess());
	private final       ServerWeights       weights;
	private volatile    CommonRegistrations registrations = CommonRegistrations.EMPTY;
	private volatile    GameplayProviders   providers     = new GameplayProviders(CommonRegistrations.EMPTY);
	private             boolean             started;

	public CommonPlugins (ServerWeights weights) {
		this.weights = Objects.requireNonNull(weights);
	}

	public CommonRegistrations registrations () {
		return registrations;
	}

	public GameplayProviders providers () {
		return providers;
	}

	public synchronized void initialize (List<PluginCandidate<HeavyInventoriesPlugin>> candidates) {
		if (started) throw new IllegalStateException("Common plugins already initialized");
		started = true;
		var next = CommonRegistrations.EMPTY;
		for (var loaded : PluginLoader.load(candidates, HeavyInventoriesPlugin::id, false)) {
			try (var registrar = new CommonRegistrar(loaded.id(), weights)) {
				loaded.plugin().register(registrar);
				next = merge(next, registrar.finish());
			} catch (RuntimeException | LinkageError e) {
				throw new IllegalStateException("Failed HI registration " + loaded.id() + " (" + loaded.description() + ")", e);
			}
		}
		registrations = next;
		providers = new GameplayProviders(next);
		HeavyInventories.LOGGER.info("Registered HI common plugins");
	}

	private static CommonRegistrations merge (CommonRegistrations current, CommonRegistrations added) {
		var containers = combine(current.containers(), added.containers());
		var owners = new HashMap<Identifier, Identifier>();
		containers.forEach((id, container) -> container.items().forEach(item -> {
			var previous = owners.putIfAbsent(item, id);
			if (previous != null) throw new IllegalArgumentException("Container item " + item + " is claimed by " + previous + " and " + id);
		}));
		return new CommonRegistrations(combine(current.inventories(), added.inventories()), containers, combine(current.capacities(), added.capacities()), combine(current.weightsReady(), added.weightsReady()), combine(current.playerChanged(), added.playerChanged()), combine(current.serverStopped(), added.serverStopped()));
	}

}
