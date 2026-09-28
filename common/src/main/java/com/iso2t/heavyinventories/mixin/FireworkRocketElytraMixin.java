package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.player.PlayerHolder;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketElytraMixin {

	@Shadow
	private LivingEntity attachedToEntity;

	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 heavyinventories$scaleRocketThrust (Vec3 direction) {
		if (!(attachedToEntity instanceof Player player)) return direction;
		float multiplier = PlayerHolder.getOrCreate(player).elytraEffects().rocketMultiplier();
		return multiplier == 1 ? direction : direction.scale(multiplier);
	}
}
