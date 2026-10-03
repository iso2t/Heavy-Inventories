package com.iso2t.heavyinventories.api.plugin;

import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.ServerWeights;
import com.iso2t.heavyinventories.api.provider.CapacityProvider;
import com.iso2t.heavyinventories.api.provider.ContainerContentsProvider;
import com.iso2t.heavyinventories.api.provider.InventoryProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Initialization-only registration. IDs must be unique within each registration category.
 */
public interface PluginRegistration {
	/**
	 * A stateless service that can be retained. Queries still require a ready world and its thread.
	 */
	ServerWeights weights ();

	void inventory (Identifier id, InventoryProvider provider);

	/**
	 * Exclusive ownership of the listed item IDs; overrides standard contents for those items.
	 */
	void container (Identifier id, Set<Identifier> items, ContainerContentsProvider provider);

	void capacity (Identifier id, CapacityProvider provider);

	/**
	 * Called after initial definitions or a successful reload commit, on the server thread.
	 */
	void onWeightsReady (Identifier id, BiConsumer<MinecraftServer, Long> listener);

	/**
	 * Called after a changed snapshot is committed. Invalidation inside a listener is deferred.
	 */
	void onPlayerChanged (Identifier id, PlayerWeightListener listener);

	/**
	 * Release any plugin-owned session state here. Registrations themselves survive server restarts.
	 */
	void onServerStopped (Identifier id, Consumer<MinecraftServer> listener);

	@FunctionalInterface
	interface PlayerWeightListener {
		void changed (ServerPlayer player, Optional<PlayerWeightSnapshot> previous, PlayerWeightSnapshot current);
	}
}
