package com.iso2t.heavyinventories.neoforge.mixin;

import com.iso2t.heavyinventories.api.events.PlayerFeedback;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge routes jump input through its fluid-type extension instead of jumpInLiquid.
 */
@Mixin(LivingEntity.class)
public abstract class NeoForgeFluidAscentMixin {
	@WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;jumpInFluid(Lnet/neoforged/neoforge/fluids/FluidType;)V"), require = 2)
	private void heavyinventories$denyFluidJump (LivingEntity entity, FluidType fluid, Operation<Void> original) {
		if (entity instanceof Player player && PlayerHolder.getOrCreate(player).preventsFluidAscent()) {
			PlayerFeedback.fluidAscentDenied(PlayerHolder.getOrCreate(player));
			return;
		}
		original.call(entity, fluid);
	}
}
