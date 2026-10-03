package com.iso2t.heavyinventories.integration;

import com.iso2t.heavyinventories.api.ServerWeights;
import com.iso2t.heavyinventories.api.plugin.PluginRegistration;
import com.iso2t.heavyinventories.api.provider.CapacityProvider;
import com.iso2t.heavyinventories.api.provider.ContainerContentsProvider;
import com.iso2t.heavyinventories.api.provider.InventoryProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class CommonRegistrar implements PluginRegistration, AutoCloseable {
	private final RegistrationScope                                  scope;
	private final ServerWeights                                      weights;
	private final Map<Identifier, InventoryProvider>                 inventories   = new HashMap<>();
	private final Map<Identifier, CommonRegistrations.Container>     containers    = new HashMap<>();
	private final Map<Identifier, CapacityProvider>                  capacities    = new HashMap<>();
	private final Map<Identifier, BiConsumer<MinecraftServer, Long>> weightsReady  = new HashMap<>();
	private final Map<Identifier, PlayerWeightListener>              playerChanged = new HashMap<>();
	private final Map<Identifier, Consumer<MinecraftServer>>         serverStopped = new HashMap<>();

	CommonRegistrar (Identifier plugin, ServerWeights weights) {
		scope = new RegistrationScope(plugin);
		this.weights = weights;
	}

	@Override
	public ServerWeights weights () {
		scope.check();
		return weights;
	}

	@Override
	public void inventory (Identifier id, InventoryProvider provider) {
		scope.add(inventories, id, provider);
	}

	@Override
	public void capacity (Identifier id, CapacityProvider provider) {
		scope.add(capacities, id, provider);
	}

	@Override
	public void onWeightsReady (Identifier id, BiConsumer<MinecraftServer, Long> listener) {
		scope.add(weightsReady, id, listener);
	}

	@Override
	public void onPlayerChanged (Identifier id, PlayerWeightListener listener) {
		scope.add(playerChanged, id, listener);
	}

	@Override
	public void onServerStopped (Identifier id, Consumer<MinecraftServer> listener) {
		scope.add(serverStopped, id, listener);
	}

	@Override
	public void container (Identifier id, Set<Identifier> items, ContainerContentsProvider provider) {
		scope.run(() -> scope.add(containers, id, new CommonRegistrations.Container(items, provider)));
	}

	CommonRegistrations finish () {
		scope.finish();
		return new CommonRegistrations(inventories, containers, capacities, weightsReady, playerChanged, serverStopped);
	}

	@Override
	public void close () {
		scope.close();
	}
}
