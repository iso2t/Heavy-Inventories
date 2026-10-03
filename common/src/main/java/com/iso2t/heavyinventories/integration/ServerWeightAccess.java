package com.iso2t.heavyinventories.integration;

import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.ServerWeights;
import com.iso2t.heavyinventories.api.WeightResult;
import com.iso2t.heavyinventories.api.WeightSource;
import com.iso2t.heavyinventories.player.PlayerHolder;
import com.iso2t.heavyinventories.player.PlayerWeightCache;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.server.weight.WeightPackData;
import com.iso2t.heavyinventories.weight.StackWeight;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

final class ServerWeightAccess implements ServerWeights {
	private static ServerWeightState state (MinecraftServer server) {
		if (!server.isSameThread()) throw new IllegalStateException("Weight queries require the owning server thread");
		return ServerWeightState.of(server);
	}

	@Override
	public WeightResult item (MinecraftServer server, Identifier item) {
		var state = state(server);
		if (state.revision() == 0 || state.stopped() || server.isStopped()) return WeightResult.unavailable();
		return BuiltInRegistries.ITEM.containsKey(item) ? WeightResult.complete(state.unitWeight(item)) : WeightResult.unknownItem();
	}

	@Override
	public WeightResult stack (MinecraftServer server, ItemStack stack) {
		var state = state(server);
		if (state.revision() == 0 || state.stopped() || server.isStopped()) return WeightResult.unavailable();
		var result = StackWeight.of(stack, state::unitWeight, server.overworld());
		return result.complete() ? WeightResult.complete(result.weight()) : WeightResult.incomplete();
	}

	@Override
	public WeightResult stack (ServerLevel level, ItemStack stack) {
		var state = state(level.getServer());
		if (state.revision() == 0 || state.stopped() || level.getServer().isStopped()) return WeightResult.unavailable();
		var result = StackWeight.of(stack, state::unitWeight, level);
		return result.complete() ? WeightResult.complete(result.weight()) : WeightResult.incomplete();
	}

	@Override
	public Optional<PlayerWeightSnapshot> player (ServerPlayer player) {
		var server = player.level().getServer();
		var state = state(server);
		return state.stopped() || server.isStopped() || player.isRemoved() ? Optional.empty() : Optional.ofNullable(PlayerHolder.getOrCreate(player).apiSnapshot());
	}

	@Override
	public Optional<WeightSource> source (MinecraftServer server, Identifier item) {
		var state = state(server);
		if (state.stopped() || server.isStopped()) return Optional.empty();
		var source = state.provenance().get(item);
		if (source == null) return Optional.empty();
		var definition = Optional.ofNullable(source.definition());
		return Optional.of(new WeightSource(WeightSource.Kind.valueOf(source.source().name()), definition.map(WeightPackData.Entry::sourcePack), definition.map(WeightPackData.Entry::resource)));
	}

	@Override
	public void invalidate (ServerPlayer player) {
		var server = player.level().getServer();
		var state = state(server);
		if (!state.stopped() && !player.isRemoved() && !server.isStopped()) PlayerWeightCache.markDirty(player);
	}
}
