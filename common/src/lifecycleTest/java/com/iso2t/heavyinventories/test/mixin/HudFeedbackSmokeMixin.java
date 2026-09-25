package com.iso2t.heavyinventories.test.mixin;
import com.iso2t.heavyinventories.gui.GraphicsRenderer;
import com.iso2t.heavyinventories.test.FeedbackScenario;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GraphicsRenderer.class)
public class HudFeedbackSmokeMixin {
    @Inject(method = "renderGui", at = @At("TAIL"))
    private static void heavyinventories$rendered(CallbackInfo ci) { FeedbackScenario.hudFrames++; }
}
