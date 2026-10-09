package com.iso2t.heavyinventories.integration.client;

import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.WeightResult;
import com.iso2t.heavyinventories.api.client.ClientWeights;
import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.client.WeightDisplay;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.player.PlayerHolder;
import com.iso2t.heavyinventories.weight.StackWeight;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

final class ClientWeightAccess implements ClientWeights {

	private static Minecraft client () {
		var client = Minecraft.getInstance();
		if (!client.isSameThread()) throw new IllegalStateException("Weight queries require the client thread");
		return client;
	}

	private static boolean ready () {
		var client = client();
		if (client.player == null || client.player.isRemoved() || client.getConnection() == null || ClientWeightData.revision() == 0) return false;
		var holder = PlayerHolder.getOrCreate(client.player);
		return holder.hasServerState() && holder.serverRevision() == ClientWeightData.revision();
	}

	@Override
	public WeightResult item (Identifier item) {
		if (!ready()) return WeightResult.unavailable();
		if (!BuiltInRegistries.ITEM.containsKey(item)) return WeightResult.unknownItem();
		var weight = ClientWeightData.weight(item);
		return weight == null ? WeightResult.unavailable() : WeightResult.complete(weight);
	}

	@Override
	public WeightResult stack (ItemStack stack) {
		if (!ready()) return WeightResult.unavailable();
		var result = StackWeight.of(stack, ClientWeightData::unitWeight, client().level);
		return result.complete() ? WeightResult.complete(result.weight()) : WeightResult.incomplete();
	}

	@Override
	public Optional<PlayerWeightSnapshot> player () {
		return ready() ? Optional.ofNullable(PlayerHolder.getOrCreate(client().player).apiSnapshot()) : Optional.empty();
	}

	@Override
	public String format (double pounds) {
		client();
		if (!Double.isFinite(pounds) || pounds < 0) throw new IllegalArgumentException("Weight must be finite and nonnegative");
		return WeightDisplay.weight(pounds, ConfigOptions.WEIGHT_MEASURE);
	}

}
