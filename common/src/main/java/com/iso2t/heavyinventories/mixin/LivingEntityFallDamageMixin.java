package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFallDamageMixin {

	// After vanilla's safe-distance/block/attribute calculation and rounding, before normal damage protection.
	@ModifyArg(method = "causeFallDamage(DFLnet/minecraft/world/damagesource/DamageSource;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"), index = 1)
	private float heavyinventories$scaleFallDamage (float damage) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (self.level().isClientSide()) return damage;
		if (!(self instanceof Player player)) return damage;

		var holder = PlayerHolder.getOrCreate(player);
		return damage * holder.getFallDamageMultiplier();
	}
}
