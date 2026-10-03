package com.iso2t.heavyinventories.integration;

import com.iso2t.heavyinventories.api.plugin.PluginRegistration;
import com.iso2t.heavyinventories.api.provider.CapacityProvider;
import com.iso2t.heavyinventories.api.provider.ContainerContentsProvider;
import com.iso2t.heavyinventories.api.provider.InventoryProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public record CommonRegistrations(Map<Identifier, InventoryProvider> inventories, Map<Identifier, Container> containers, Map<Identifier, CapacityProvider> capacities, Map<Identifier, BiConsumer<MinecraftServer, Long>> weightsReady,
								  Map<Identifier, PluginRegistration.PlayerWeightListener> playerChanged, Map<Identifier, Consumer<MinecraftServer>> serverStopped) {
	public static final CommonRegistrations EMPTY = new CommonRegistrations(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

	public CommonRegistrations {
		inventories = RegistrationScope.freeze(inventories);
		containers = RegistrationScope.freeze(containers);
		capacities = RegistrationScope.freeze(capacities);
		weightsReady = RegistrationScope.freeze(weightsReady);
		playerChanged = RegistrationScope.freeze(playerChanged);
		serverStopped = RegistrationScope.freeze(serverStopped);
	}

	public record Container(Set<Identifier> items, ContainerContentsProvider provider) {
		public Container {
			items = Set.copyOf(items);
			if (items.isEmpty()) throw new IllegalArgumentException("Container registration needs item IDs");
			Objects.requireNonNull(provider, "Container provider");
		}
	}
}
