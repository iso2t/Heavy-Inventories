package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.server.weight.WeightPackAccess;
import com.iso2t.heavyinventories.server.weight.WeightPackData;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;

@Mixin(MultiPackResourceManager.class)
public abstract class WeightPackDataMixin implements WeightPackAccess {
	@Unique
	private volatile Optional<WeightPackData.Result> heavyinventories$weightPackData = Optional.empty();

	@Override
	public Optional<WeightPackData.Result> heavyinventories$getWeightPackData () {
		return heavyinventories$weightPackData;
	}

	@Override
	public void heavyinventories$setWeightPackData (WeightPackData.Result result) {
		heavyinventories$weightPackData = Optional.of(result);
	}
}
