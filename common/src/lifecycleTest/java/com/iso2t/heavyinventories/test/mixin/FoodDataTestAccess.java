package com.iso2t.heavyinventories.test.mixin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FoodData.class)
public interface FoodDataTestAccess {
	@Accessor("exhaustionLevel") float heavyinventories$getExhaustion();
	@Accessor("exhaustionLevel") void heavyinventories$setExhaustion(float value);
}
