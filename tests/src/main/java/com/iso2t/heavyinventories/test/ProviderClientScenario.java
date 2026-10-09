package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.api.WeightResult;
import com.iso2t.heavyinventories.test.plugin.*;
import com.iso2t.heavyinventories.tooltips.Tooltip;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public final class ProviderClientScenario {

	private static int phase, ticks;
	private static CompletableFuture<double[]> operation;

	public static boolean tick (Minecraft client) {
		if (++ticks > 400) throw new AssertionError("Provider synchronization timed out at " + phase);
		var server = client.getSingleplayerServer();
		if (phase == 0) {
			operation = server.submit(() -> {
				FixtureNotifications.server(server, TestPlayerTick::update);
				FixtureProviders.server(server, TestPlayerTick::update);
				var player = server.getPlayerList().getPlayer(client.player.getUUID());
				player.removeAllEffects();
				player.getInventory().clearContent();
				FixtureProviders.target = player.getId();
				FixtureProviders.extra = new ItemStack(Items.STONE, 5);
				player.getInventory().setItem(0, FixtureProviders.container("serverOnly"));
				player.setItemSlot(EquipmentSlot.OFFHAND, FixtureProviders.capacityEquipment());
				TestPlayerTick.update(player);
				var snapshot = FixturePlugin.weights.player(player).orElseThrow();
				return new double[] { snapshot.carriedWeight().pounds().orElseThrow(), snapshot.capacity() };
			});
			phase = 1;
		} else if (phase == 1) {
			if (!synchronizedState()) return false;
			var weights = FixtureClientPlugin.weights;
			double expected = weights.item(BuiltInRegistries.ITEM.getKey(Items.CLOCK)).pounds().orElseThrow() + 4 * weights.item(BuiltInRegistries.ITEM.getKey(Items.STONE)).pounds().orElseThrow();
			if (Math.abs(weights.stack(FixtureProviders.container("normal")).pounds().orElseThrow() - expected) > 0.001) throw new AssertionError("Custom client preview differs");
			if (weights.stack(FixtureProviders.container("serverOnly")).status() != WeightResult.Status.INCOMPLETE) throw new AssertionError("Unsynchronized contents invented a client total");
			if (Tooltip.addTooltips(new ArrayList<>(), FixtureProviders.container("serverOnly")).stream().noneMatch(line -> line.getString().contains("could not be fully calculated")))
				throw new AssertionError("Tooltip hid unavailable custom contents");
			operation = server.submit(() -> {
				var player = server.getPlayerList().getPlayer(client.player.getUUID());
				FixtureProviders.reset();
				player.getInventory().clearContent();
				TestPlayerTick.update(player);
				var snapshot = FixturePlugin.weights.player(player).orElseThrow();
				return new double[] { 0, snapshot.capacity() };
			});
			phase = 2;
		} else if (phase == 2 && synchronizedState() && FixtureClientNotifications.current()) {
			FixtureClientNotifications.verify();
			LogUtils.getLogger().info("API PROVIDERS CLIENT PASSED: server-owned extra slots and capacity, custom previews, unavailable contents, synchronized removal");
			return true;
		}
		return false;
	}

	private static boolean synchronizedState () {
		if (!operation.isDone()) return false;
		var expected = operation.join();
		var snapshot = FixtureClientPlugin.weights.player();
		return snapshot.isPresent() && snapshot.get().carriedWeight().pounds().isPresent() && Math.abs(snapshot.get().carriedWeight().pounds().orElseThrow() - expected[0]) < 0.001 && Math.abs(snapshot.get().capacity() - expected[1]) < 0.001;
	}

}
