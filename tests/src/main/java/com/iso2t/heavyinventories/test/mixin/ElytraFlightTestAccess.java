package com.iso2t.heavyinventories.test.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface ElytraFlightTestAccess {

	@Invoker("updateFallFlyingMovement")
	Vec3 heavyinventories$glide (Vec3 movement);

}
