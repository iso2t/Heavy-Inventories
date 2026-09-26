package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.api.player.PlayerExhaustion;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerExhaustionMixin {
	@Unique private double heavyinventories$beforeJumpY;

	@WrapOperation(method = "checkMovementStatistics(DDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"), require = 6)
	private void heavyinventories$movementExhaustion (ServerPlayer player, float cost, Operation<Void> original, double dx, double dy, double dz) {
		original.call(player, PlayerExhaustion.movement(player, cost, dx, dy, dz));
	}

	@Inject(method = "jumpFromGround()V", at = @At("HEAD"), cancellable = true)
	private void heavyinventories$beginJump (CallbackInfo ci) {
		var player = (ServerPlayer) (Object) this;
		// Cancelling only LivingEntity's implementation still leaves this override's food/stat costs.
		if (PlayerHolder.getOrCreate(player).preventsGroundJump()) ci.cancel();
		heavyinventories$beforeJumpY = player.getDeltaMovement().y;
	}

	@ModifyArg(method = "jumpFromGround()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"), index = 0, require = 2)
	private float heavyinventories$jumpExhaustion (float cost) {
		return PlayerExhaustion.jump((ServerPlayer) (Object) this, cost, heavyinventories$beforeJumpY);
	}
}
