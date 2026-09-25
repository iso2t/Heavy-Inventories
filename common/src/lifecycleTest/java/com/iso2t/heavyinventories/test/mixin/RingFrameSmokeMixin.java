package com.iso2t.heavyinventories.test.mixin;

import com.iso2t.heavyinventories.test.RingHudScenario;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class RingFrameSmokeMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void heavyinventories$frame (CallbackInfo ci) {
		RingHudScenario.ringsThisFrame = 0;
	}
}
