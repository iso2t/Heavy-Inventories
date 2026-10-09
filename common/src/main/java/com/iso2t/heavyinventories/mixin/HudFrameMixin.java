package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.gui.WeightHud;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Gui.class)
public abstract class HudFrameMixin {

	@WrapMethod(method = "extractRenderState")
	private void heavyinventories$frame (DeltaTracker delta, boolean renderLevel, boolean resourcesLoaded, Operation<Void> original) {
		WeightHud.beginFrame();
		try {
			original.call(delta, renderLevel, resourcesLoaded);
		} finally {
			WeightHud.endFrame();
		}
	}

}
