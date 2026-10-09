package com.iso2t.heavyinventories.test.plugin;

import com.iso2t.heavyinventories.api.WeightResult;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class ApiWeightChecks {

	private static final Identifier STONE   = Identifier.withDefaultNamespace("stone");
	private static final Identifier UNKNOWN = Identifier.fromNamespaceAndPath("hi_fixture", "unknown");
	private static       boolean    unavailableChecked;

	public static void beforeDefinitions (MinecraftServer server) {
		var weights = FixturePlugin.weights;
		require(weights.item(server, STONE).status() == WeightResult.Status.UNAVAILABLE, "Pre-start item must be unavailable");
		require(weights.stack(server, ItemStack.EMPTY).status() == WeightResult.Status.UNAVAILABLE, "Pre-start stack must be unavailable");
		require(weights.source(server, STONE).isEmpty(), "Pre-start provenance must be absent");
	}

	public static void server (ServerPlayer player, Runnable update) {
		var weights = FixturePlugin.weights;
		var server = player.level().getServer();
		require(weights.player(player).isEmpty(), "New player must have no fabricated snapshot");
		require(weights.item(server, UNKNOWN).status() == WeightResult.Status.UNKNOWN_ITEM && weights.source(server, UNKNOWN).isEmpty(), "Unknown item must not become fallback");
		near(weights.stack(server, ItemStack.EMPTY).pounds().orElseThrow(), 0);
		var nested = nested();
		var original = nested.copy();
		double expected = 2 * (weights.item(server, id("shulker_box")).pounds().orElseThrow() + weights.item(server, id("bundle")).pounds().orElseThrow() + 4 * weights.item(server, STONE).pounds().orElseThrow());
		near(weights.stack(server, nested).pounds().orElseThrow(), expected);
		require(ItemStack.matches(original, nested), "Query mutated nested stack");
		var incomplete = weights.stack(server, tooDeep());
		require(incomplete.status() == WeightResult.Status.INCOMPLETE && incomplete.pounds().isEmpty(), "Limits must not expose sentinel or zero");
		player.getInventory().clearContent();
		player.getInventory().setItem(0, nested);
		update.run();
		var before = weights.player(player).orElseThrow();
		near(before.carriedWeight().pounds().orElseThrow(), expected);
		require(before.tick() == player.level().getGameTime(), "Server calculation timestamp");
		player.getInventory().setItem(0, new ItemStack(Items.STONE, 3));
		weights.invalidate(player);
		require(weights.player(player).orElseThrow() == before, "Query or invalidation recalculated player eagerly");
		update.run();
		near(weights.player(player).orElseThrow().carriedWeight().pounds().orElseThrow(), 3 * weights.item(server, STONE).pounds().orElseThrow());
		near(before.carriedWeight().pounds().orElseThrow(), expected);
		player.getInventory().setItem(0, tooDeep());
		update.run();
		var limited = weights.player(player).orElseThrow();
		require(limited.carriedWeight().status() == WeightResult.Status.INCOMPLETE && limited.loadRatio().isEmpty(), "Player limit must be explicit");
		rejectsWorker(() -> weights.item(server, STONE));
		rejectsWorker(() -> weights.player(player));
		rejectsWorker(() -> weights.invalidate(player));
		player.discard();
		require(weights.player(player).isEmpty(), "Removed player retained API availability");
		LogUtils.getLogger().info("API QUERIES SERVER PASSED: unknown/empty/nested/limited stacks, immutable snapshots, deferred invalidation, replacement, thread guards");
	}

	public static void provenance (MinecraftServer server) {
		var weights = FixturePlugin.weights;
		boolean explicit = false, recipe = false, fallback = false;
		for (var item : BuiltInRegistries.ITEM.keySet()) {
			var source = weights.source(server, item).orElseThrow();
			switch (source.kind()) {
				case EXPLICIT -> {
					explicit = true;
					require(source.pack().isPresent() && source.resource().isPresent(), "Explicit source lost provenance");
				}
				case RECIPE -> recipe = true;
				case FALLBACK -> {
					fallback = true;
					near(weights.item(server, item).pounds().orElseThrow(), 0.1);
				}
			}
		}
		require(explicit && recipe && fallback, "Must exercise explicit, recipe, and fallback sources");
	}

	public static void unavailableClient () {
		var weights = FixtureClientPlugin.weights;
		require(weights != null, "Client service was not registered");
		require(weights.player().isEmpty(), "Disconnected/unsynchronized player snapshot available");
		require(weights.item(STONE).status() == WeightResult.Status.UNAVAILABLE, "Disconnected/unsynchronized item available");
		require(weights.stack(ItemStack.EMPTY).status() == WeightResult.Status.UNAVAILABLE, "Disconnected/unsynchronized stack available");
		if (!unavailableChecked) {
			LogUtils.getLogger().info("API QUERIES UNAVAILABLE PASSED");
			unavailableChecked = true;
		}
	}

	public static void client (float total, double capacity, long revision) {
		var weights = FixtureClientPlugin.weights;
		var snapshot = weights.player().orElseThrow();
		near(snapshot.carriedWeight().pounds().orElseThrow(), total);
		near(snapshot.capacity(), capacity);
		require(snapshot.revision() == revision, "Client snapshot revision mismatch");
		near(snapshot.loadRatio().orElseThrow(), total / capacity);
		require(weights.item(UNKNOWN).status() == WeightResult.Status.UNKNOWN_ITEM, "Client unknown item status");
		var nested = nested();
		var before = nested.copy();
		double expected = 2 * (weights.item(id("shulker_box")).pounds().orElseThrow() + weights.item(id("bundle")).pounds().orElseThrow() + 4 * weights.item(STONE).pounds().orElseThrow());
		near(weights.stack(nested).pounds().orElseThrow(), expected);
		require(ItemStack.matches(before, nested), "Client query mutated stack");
		require(weights.stack(tooDeep()).status() == WeightResult.Status.INCOMPLETE, "Client depth limit status");
		near(weights.stack(ItemStack.EMPTY).pounds().orElseThrow(), 0);
		rejectsWorker(() -> weights.item(STONE));
		rejectsWorker(weights::player);
		var player = Minecraft.getInstance().player;
		boolean flying = player.getAbilities().flying;
		try {
			player.getAbilities().flying = true;
			var flight = weights.player().orElseThrow();
			near(flight.walkingMultiplier(), 1);
			require(flight.carriedWeight().equals(snapshot.carriedWeight()) && flight.tick() == snapshot.tick(), "Flight query changed authoritative data");
		} finally {
			player.getAbilities().flying = flying;
		}
		require(weights.player().orElseThrow().equals(snapshot), "Flight exemption left stale snapshot");
	}

	public static void reload (MinecraftServer server, ServerPlayer player, float arrow, float stone, float total, long revision) {
		var weights = FixturePlugin.weights;
		near(weights.item(server, id("arrow")).pounds().orElseThrow(), arrow);
		near(weights.item(server, STONE).pounds().orElseThrow(), stone);
		var snapshot = weights.player(player).orElseThrow();
		near(snapshot.carriedWeight().pounds().orElseThrow(), total);
		require(snapshot.revision() == revision, "Server API did not adopt/retain reload revision");
		LogUtils.getLogger().info("API QUERIES RELOAD CHECKPOINT: {}", revision);
	}

	public static void near (double actual, double expected) {
		require(Math.abs(actual - expected) < Math.max(0.00001, Math.abs(expected) * 0.000001), "Expected " + expected + ", got " + actual);
	}

	private static ItemStack nested () {
		var bundle = new ItemStack(Items.BUNDLE);
		bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(new ItemStackTemplate(Items.STONE, 4))));
		var box = new ItemStack(Items.SHULKER_BOX, 2);
		box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(bundle)));
		return box;
	}

	public static ItemStack tooDeep () {
		var stack = new ItemStack(Items.STONE);
		for (int depth = 0; depth < 20; depth++) {
			var parent = new ItemStack(Items.SHULKER_BOX);
			parent.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(stack)));
			stack = parent;
		}
		return stack;
	}

	private static void rejectsWorker (Runnable action) {
		var failure = new AtomicReference<Throwable>();
		var worker = new Thread(() -> {
			try {
				action.run();
			} catch (Throwable e) {
				failure.set(e);
			}
		});
		worker.start();
		try {
			worker.join();
		} catch (InterruptedException e) {
			throw new AssertionError(e);
		}
		require(failure.get() instanceof IllegalStateException, "Wrong-thread API call was not rejected: " + failure.get());
	}

	private static Identifier id (String path) {
		return Identifier.withDefaultNamespace(path);
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

}
