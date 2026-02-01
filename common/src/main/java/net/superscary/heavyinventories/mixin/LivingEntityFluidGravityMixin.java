package net.superscary.heavyinventories.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.superscary.heavyinventories.api.player.PlayerHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFluidGravityMixin {

    @ModifyVariable(method = "getFluidFallingAdjustedMovement", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double heavyinventories$scaleFluidGravity(double gravity) {
        LivingEntity self = (LivingEntity)(Object)this;

        if (!(self instanceof Player player)) return gravity;
        if (!(self.isInWaterOrBubble() || self.isInLava())) return gravity;

        float multi = PlayerHolder.getOrCreate(player).getFluidSinkGravityMultiplier();
        return gravity * multi;
    }
}
