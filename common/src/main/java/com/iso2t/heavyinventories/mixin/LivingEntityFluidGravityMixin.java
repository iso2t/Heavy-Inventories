package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Modify the shared fluid gravity source, including deep lava's separate gravity path.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFluidGravityMixin {
	// NeoForge delegates to an overload with FluidState; match both signatures.
	@ModifyExpressionValue(method = "travelInFluid*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getEffectiveGravity()D"))
	private double heavyinventories$scaleFluidGravity (double gravity) {
		if (!((Object) this instanceof Player player)) return gravity;
		return gravity * PlayerHolder.getOrCreate(player).getFluidSinkGravityMultiplier();
	}
}
