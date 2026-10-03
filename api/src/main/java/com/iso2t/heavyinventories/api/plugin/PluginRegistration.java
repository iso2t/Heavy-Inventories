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
 * Initialization-only registration, on the loader's registration thread, before register returns.
 * IDs use the plugin's namespace and must be unique within each registration category across plugins.
 * Notifications run in registration-ID order on the owning server thread. Runtime and linkage failures
 * are isolated and logged at most once per minute per listener/category for that session.
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
	 * Called after each committed definition/settings revision, including initial readiness.
	 * Rejected reloads emit nothing. Weight queries see the new table; players update afterward.
	 */
	void onWeightsReady (Identifier id, BiConsumer<MinecraftServer, Long> listener);

	/**
	 * Called after the first snapshot and each change to its values or revision; tick-only changes are ignored.
	 * Previous is the last delivered snapshot, or empty for a new player entity.
	 * Invalidation inside a listener schedules a later update; recursive HI commits are rejected.
	 */
	void onPlayerChanged (Identifier id, PlayerWeightListener listener);

	/**
	 * Called once after normal server shutdown; weight queries are unavailable at this point.
	 * Release plugin-owned session state here. Registrations themselves survive server restarts.
	 */
	void onServerStopped (Identifier id, Consumer<MinecraftServer> listener);

	@FunctionalInterface
	interface PlayerWeightListener {
		void changed (ServerPlayer player, Optional<PlayerWeightSnapshot> previous, PlayerWeightSnapshot current);
	}
}
