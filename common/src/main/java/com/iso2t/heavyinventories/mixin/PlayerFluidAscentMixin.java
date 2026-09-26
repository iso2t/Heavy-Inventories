package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.api.events.PlayerFeedback;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerFluidAscentMixin {
	@WrapOperation(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 heavyinventories$denyUpwardSwimming (Vec3 movement, double x, double y, double z, Operation<Vec3> original) {
		var holder = PlayerHolder.getOrCreate((Player) (Object) this);
		if (y > 0 && holder.preventsFluidAscent()) {
			PlayerFeedback.fluidAscentDenied(holder);
			y = 0;
		}
		return original.call(movement, x, y, z);
	}
}
