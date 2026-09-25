package com.iso2t.heavyinventories.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFallDamageMixin {

    @ModifyVariable(method = "causeFallDamage(DFLnet/minecraft/world/damagesource/DamageSource;)Z", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float heavyinventories$scaleFallDamage(float damageMultiplier) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self.level().isClientSide()) return damageMultiplier;
        if (!(self instanceof Player player)) return damageMultiplier;

        var holder = PlayerHolder.getOrCreate(player);
        return damageMultiplier * holder.getFallDamageMultiplier();
    }
}
