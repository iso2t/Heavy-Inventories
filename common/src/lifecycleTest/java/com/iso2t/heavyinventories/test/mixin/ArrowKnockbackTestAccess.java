package com.iso2t.heavyinventories.test.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractArrow.class)
public interface ArrowKnockbackTestAccess {
	@Invoker("doKnockback") void heavyinventories$knockback(LivingEntity target, DamageSource source);
}
