package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class ExternalMovementExhaustionMixin {
	@Inject(method = "push(DDD)V", at = @At("HEAD"))
	private void heavyinventories$externalImpulse (double x, double y, double z, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && (x != 0 || y != 0 || z != 0)) PlayerHolder.getOrCreate(player).suppressMovementExhaustion();
	}

	@Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
	private void heavyinventories$externalMove (MoverType type, Vec3 movement, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player && type != MoverType.SELF && type != MoverType.PLAYER && movement.lengthSqr() > 0) PlayerHolder.getOrCreate(player).suppressMovementExhaustion();
	}

	@Inject(method = { "onInsideBubbleColumn", "onAboveBubbleColumn" }, at = @At("HEAD"))
	private void heavyinventories$bubbleMotion (CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player) PlayerHolder.getOrCreate(player).suppressMovementExhaustion();
	}
}
