package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.api.events.PlayerFeedback;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remove only fluid propulsion, never the entity's accumulated external velocity. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFluidAscentMixin {
	@Inject(method = "jumpInLiquid", at = @At("HEAD"), cancellable = true)
	private void heavyinventories$denyFluidJump (CallbackInfo ci) {
		if ((Object) this instanceof Player player && PlayerHolder.getOrCreate(player).preventsFluidAscent()) {
			PlayerFeedback.fluidAscentDenied(PlayerHolder.getOrCreate(player));
			ci.cancel();
		}
	}

	@WrapOperation(method = "jumpOutOfFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V"))
	private void heavyinventories$denyLedgeBoost (LivingEntity entity, double x, double y, double z, Operation<Void> original) {
		if (entity instanceof Player player && PlayerHolder.getOrCreate(player).preventsFluidAscent()) {
			PlayerFeedback.fluidAscentDenied(PlayerHolder.getOrCreate(player));
			return;
		}
		original.call(entity, x, y, z);
	}

	@WrapOperation(method = "travelInWater", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 heavyinventories$denySubmergedClimbBoost (double x, double y, double z, Operation<Vec3> original) {
		if ((Object) this instanceof Player player && y > player.getDeltaMovement().y && PlayerHolder.getOrCreate(player).preventsFluidAscent()) {
			PlayerFeedback.fluidAscentDenied(PlayerHolder.getOrCreate(player));
			y = player.getDeltaMovement().y;
		}
		return original.call(x, y, z);
	}
}
