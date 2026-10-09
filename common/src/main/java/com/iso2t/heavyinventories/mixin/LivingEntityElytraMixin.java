package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.player.PlayerHolder;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityElytraMixin {

	@ModifyExpressionValue(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;square(D)D"))
	private double heavyinventories$scaleLift (double lift) {
		if (!((Object) this instanceof Player player)) return lift;
		return lift * PlayerHolder.getOrCreate(player).elytraEffects().liftMultiplier();
	}

}
