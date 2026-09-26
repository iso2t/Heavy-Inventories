package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Scale normalized horizontal acceleration once; optionally deny only positive fluid input.
 */
@Mixin(Entity.class)
public abstract class EntityMovementMixin {
	@ModifyExpressionValue(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 heavyinventories$scaleMovement (Vec3 movement) {
		if (!((Object) this instanceof Player player)) return movement;
		var holder = PlayerHolder.getOrCreate(player);
		float multiplier = player.isInWater() || player.isInLava() ? holder.getFluidSwimMultiplier() : holder.getWalkingMultiplier();
		double vertical = movement.y;
		if (vertical > 0 && holder.preventsFluidAscent()) {
			vertical = 0;
			com.iso2t.heavyinventories.api.events.PlayerFeedback.fluidAscentDenied(holder);
		}
		return multiplier == 1 && vertical == movement.y ? movement : new Vec3(movement.x * multiplier, vertical, movement.z * multiplier);
	}
}
