package com.iso2t.heavyinventories.test.mixin;

import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FireworkRocketEntity.class)
public interface FireworkFlightTestAccess {

	@Accessor("lifetime")
	int heavyinventories$lifetime ();

	@Accessor("life")
	int heavyinventories$life ();

}
