package com.iso2t.heavyinventories.test.mixin;

import com.iso2t.heavyinventories.gui.WeightRingRenderer;
import com.iso2t.heavyinventories.test.FeedbackScenario;
import com.iso2t.heavyinventories.test.RingHudScenario;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeightRingRenderer.class)
public class RingHudSmokeMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private static void heavyinventories$ring (CallbackInfo ci) {
		FeedbackScenario.hudFrames++;
		RingHudScenario.ringFrames++;
		if (RingHudScenario.active && ++RingHudScenario.ringsThisFrame > 1) throw new AssertionError("Ring rendered twice in one GUI frame");
	}
}
