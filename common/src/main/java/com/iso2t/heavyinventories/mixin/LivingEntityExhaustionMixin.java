package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.player.PlayerHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityExhaustionMixin {
	@Inject(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V", at = @At("HEAD"))
	private void heavyinventories$knockbackMotion (double strength, double x, double z, DamageSource source, float damage, boolean comesFromEffect, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player && strength > 0 && player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < 1) PlayerHolder.getOrCreate(player).suppressMovementExhaustion();
	}
}
