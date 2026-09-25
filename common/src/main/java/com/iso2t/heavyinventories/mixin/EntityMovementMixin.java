package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Scale normalized horizontal acceleration once, preserving vertical input and other speed modifiers.
 */
@Mixin(Entity.class)
public abstract class EntityMovementMixin {
	@ModifyExpressionValue(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 heavyinventories$scaleMovement (Vec3 movement) {
		if (!((Object) this instanceof Player player)) return movement;
		var holder = PlayerHolder.getOrCreate(player);
		float multiplier = player.isInWater() || player.isInLava() ? holder.getFluidSwimMultiplier() : holder.getWalkingMultiplier();
		return multiplier == 1 ? movement : new Vec3(movement.x * multiplier, movement.y, movement.z * multiplier);
	}
}
