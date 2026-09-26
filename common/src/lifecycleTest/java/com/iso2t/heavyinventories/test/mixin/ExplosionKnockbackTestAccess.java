package com.iso2t.heavyinventories.test.mixin;

import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerExplosion.class)
public interface ExplosionKnockbackTestAccess {
	@Invoker("hurtEntities") void heavyinventories$affectEntities();
}
