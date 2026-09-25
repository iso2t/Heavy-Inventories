package com.iso2t.heavyinventories.test.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Controlled fluid state for physics tests; never included in the mod jar. */
@Mixin(Entity.class)
public interface FluidTestAccess {
    @org.spongepowered.asm.mixin.gen.Invoker("updateFluidInteraction") boolean heavyinventories$updateFluid();
    @Accessor("firstTick") boolean heavyinventories$isFirstTick();
    @Accessor("wasTouchingWater") void heavyinventories$setWater(boolean value);
}
