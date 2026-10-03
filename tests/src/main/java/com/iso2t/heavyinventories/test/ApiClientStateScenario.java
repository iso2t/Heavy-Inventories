package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.client.WeightDisplay;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.network.ItemWeightsPayload;
import com.iso2t.heavyinventories.player.PlayerHolder;
import com.iso2t.heavyinventories.test.mixin.ClientGameModeTestAccess;
import com.iso2t.heavyinventories.test.plugin.ApiWeightChecks;
import com.iso2t.heavyinventories.test.plugin.FixtureClientPlugin;
import com.iso2t.heavyinventories.util.MeasuringSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;

public final class ApiClientStateScenario {
	public static void run (Minecraft client) {
		var holder = PlayerHolder.getOrCreate(client.player);
		ApiWeightChecks.client(holder.getWeight(), holder.getMaxWeight(), holder.serverRevision());
		var weights = FixtureClientPlugin.weights;
		var snapshot = weights.player().orElseThrow();
		var info = client.getConnection().getPlayerInfo(client.player.getUUID());
		var mode = info.getGameMode();
		try {
			for (var exempt : new GameType[] { GameType.CREATIVE, GameType.SPECTATOR }) {
				((ClientGameModeTestAccess) info).heavyinventories$setMode(exempt);
				var exemptSnapshot = weights.player().orElseThrow();
				if (exemptSnapshot.effectsApply() || exemptSnapshot.walkingMultiplier() != 1) throw new AssertionError("Client game-mode exemption is stale");
				if (!exemptSnapshot.carriedWeight().equals(snapshot.carriedWeight())) throw new AssertionError("Game mode changed authoritative weight");
			}
		} finally {
			((ClientGameModeTestAccess) info).heavyinventories$setMode(mode);
		}
		if (!weights.player().orElseThrow().equals(snapshot)) throw new AssertionError("Game-mode change mutated retained snapshot");
		var entries = new ArrayList<ItemWeightsPayload.Entry>();
		for (var id : BuiltInRegistries.ITEM.keySet()) {
			var weight = ClientWeightData.weight(id);
			if (weight != null) entries.add(new ItemWeightsPayload.Entry(id, weight));
		}
		int chunks = (entries.size() + ItemWeightsPayload.CHUNK_SIZE - 1) / ItemWeightsPayload.CHUNK_SIZE;
		long revision = ClientWeightData.revision();
		try {
			for (int i = 0; i < chunks; i++) {
				ClientWeightData.accept(new ItemWeightsPayload(revision + 1, i, chunks, entries.subList(i * ItemWeightsPayload.CHUNK_SIZE, Math.min(entries.size(), (i + 1) * ItemWeightsPayload.CHUNK_SIZE))));
				if (i < chunks - 1 && !weights.player().orElseThrow().equals(snapshot)) throw new AssertionError("Partial table replaced coherent snapshot");
			}
			ApiWeightChecks.unavailableClient();
		} finally {
			ClientWeightData.clear();
			ApiWeightChecks.unavailableClient();
			for (int i = 0; i < chunks; i++) ClientWeightData.accept(new ItemWeightsPayload(revision, i, chunks, entries.subList(i * ItemWeightsPayload.CHUNK_SIZE, Math.min(entries.size(), (i + 1) * ItemWeightsPayload.CHUNK_SIZE))));
		}
		if (!weights.player().orElseThrow().equals(snapshot)) throw new AssertionError("Definition restore changed immutable snapshot");
		var units = ConfigOptions.WEIGHT_MEASURE;
		try {
			for (var measure : MeasuringSystem.values()) {
				ConfigOptions.WEIGHT_MEASURE = measure;
				for (double value : new double[] { 0, 0.053125, 0.0000001, 1000 })
					if (!weights.format(value).equals(WeightDisplay.weight(value, measure))) throw new AssertionError("API display formatting diverged");
			}
			for (double value : new double[] { -1, Double.NaN, Double.POSITIVE_INFINITY }) {
				try {
					weights.format(value);
					throw new AssertionError("Invalid display value accepted");
				} catch (IllegalArgumentException expected) {
				}
			}
		} finally {
			ConfigOptions.WEIGHT_MEASURE = units;
		}
		LogUtils.getLogger().info("API QUERIES CLIENT PASSED: nested/limited stacks, incomplete tables, revision mismatch, unavailable state, restored snapshots, display units");
	}
}
