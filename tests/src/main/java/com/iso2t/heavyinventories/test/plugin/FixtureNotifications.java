package com.iso2t.heavyinventories.test.plugin;

import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.WeightResult;
import com.iso2t.heavyinventories.api.plugin.PluginRegistration;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class FixtureNotifications {
	public static int readyEvents, playerEvents, failures, stoppedEvents;
	public static  long                 readyRevision;
	private static int                  target = -1;
	private static boolean              invalidate;
	private static PlayerWeightSnapshot delivered;

	private static Identifier id (String path) {
		return Identifier.fromNamespaceAndPath("heavyinventories_lifecycle_test", path);
	}

	public static void register (PluginRegistration registration) {
		registration.onWeightsReady(id("a_failed_ready"), (server, revision) -> {
			throw new IllegalStateException("Expected ready listener failure");
		});
		registration.onWeightsReady(id("b_ready"), (server, revision) -> {
			require(server.isSameThread(), "Ready callback ran off-thread");
			require(revision > readyRevision, "Duplicate ready notification");
			require(FixturePlugin.weights.item(server, Identifier.withDefaultNamespace("stone")).status() == WeightResult.Status.COMPLETE, "Ready listener ran before commit");
			if (readyRevision == 0) readyEvents = 0;
			readyRevision = revision;
			readyEvents++;
		});
		registration.onPlayerChanged(id("a_failed_player"), (player, previous, current) -> {
			if (player.getId() == target) {
				failures++;
				throw new IllegalStateException("Expected player listener failure");
			}
		});
		registration.onPlayerChanged(id("b_player"), (player, previous, current) -> {
			require(player.level().getServer().isSameThread(), "Player callback ran off-thread");
			require(FixturePlugin.weights.player(player).orElseThrow().equals(current), "Player listener ran before commit");
			if (player.getId() != target) return;
			require(previous.equals(Optional.ofNullable(delivered)), "Previous snapshot was not the last delivered result");
			delivered = current;
			playerEvents++;
			if (invalidate) {
				FixturePlugin.weights.invalidate(player);
				require(FixturePlugin.weights.player(player).orElseThrow() == current, "Listener invalidation recalculated recursively");
			}
		});
		registration.onServerStopped(id("a_failed_stop"), server -> {
			throw new IllegalStateException("Expected stop listener failure");
		});
		registration.onServerStopped(id("b_stopped"), server -> {
			require(server.isSameThread(), "Stop callback ran off-thread");
			require(FixturePlugin.weights.item(server, Identifier.withDefaultNamespace("stone")).status() == WeightResult.Status.UNAVAILABLE, "Stopped query still available");
			stoppedEvents++;
			readyRevision = 0;
			delivered = null;
			target = -1;
			LogUtils.getLogger().info("API NOTIFICATIONS STOPPED PASSED: once-only cleanup callback, unavailable queries, failure isolation");
		});
	}

	public static void server (MinecraftServer server, Consumer<ServerPlayer> update) {
		require(readyEvents >= 1 && readyRevision > 0, "Initial weights-ready event missing");
		var player = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.randomUUID(), "NotificationTest"), ClientInformation.createDefault());
		target = player.getId();
		delivered = null;
		playerEvents = failures = 0;
		try {
			update.accept(player);
			require(playerEvents == 1 && failures == 1, "First completed player snapshot did not notify once");
			var first = delivered;
			for (int i = 0; i < 5; i++) {
				player.tickCount++;
				update.accept(player);
			}
			require(playerEvents == 1, "Unchanged ticks emitted events");
			player.getInventory().setItem(0, new ItemStack(Items.STONE, 2));
			invalidate = true;
			update.accept(player);
			require(playerEvents == 2 && failures == 2, "Changed weight was not delivered past failed listener");
			require(first.carriedWeight().pounds().orElseThrow() == 0, "Previous snapshot mutated");
			update.accept(player);
			require(playerEvents == 2, "Deferred invalidation emitted an unchanged event");
			player.getInventory().setItem(0, ApiWeightChecks.tooDeep());
			update.accept(player);
			require(playerEvents == 3 && delivered.carriedWeight().status() == WeightResult.Status.INCOMPLETE, "Incomplete state change missing");
			var replacement = new ServerPlayer(server, server.overworld(), player.getGameProfile(), ClientInformation.createDefault());
			target = replacement.getId();
			delivered = null;
			update.accept(replacement);
			require(playerEvents == 4 && delivered.carriedWeight().pounds().orElseThrow() == 0, "Replacement player did not start fresh");
			LogUtils.getLogger().info("API NOTIFICATIONS SERVER PASSED: initial-ready, coherent old/new snapshots, unchanged-tick suppression, deferred invalidation, incomplete results, entity replacement and listener isolation");
		} finally {
			target = -1;
			delivered = null;
			invalidate = false;
		}
	}

	private static void require (boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
}
