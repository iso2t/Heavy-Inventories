package com.iso2t.heavyinventories.test.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface FluidTravelTestAccess {
    @Invoker("tickEffects") void heavyinventories$tickEffects();
    @Invoker("travelInFluid") void heavyinventories$travelInFluid(Vec3 input);
}
