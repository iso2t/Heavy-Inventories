package net.superscary.heavyinventories.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.superscary.heavyinventories.api.player.PlayerHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * While in a fluid, scale the horizontal part of the travel vector for players,
 * based on encumbrance. We do NOT touch Y here (vertical).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFluidTravelMixin {

    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 heavyinventories$scaleFluidTravel(Vec3 travel) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Player player)) return travel;
        if (!(self.isInWaterOrBubble() || self.isInLava())) return travel;

        float multi = PlayerHolder.getOrCreate(player).getFluidSwimMultiplier();
        if (multi == 1.0f) return travel;

        return new Vec3(travel.x * multi, travel.y, travel.z * multi);
    }

}
