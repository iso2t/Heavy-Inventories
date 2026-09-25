package com.iso2t.heavyinventories.test.mixin;

import com.iso2t.heavyinventories.gui.WeightRingRenderer;
import com.iso2t.heavyinventories.test.RingHudScenario;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContextualBarRenderer.class)
public interface ExperiencePositionSmokeMixin {
	@Inject(method = "extractExperienceLevel", at = @At("HEAD"))
	private static void heavyinventories$position (GuiGraphicsExtractor graphics, Font font, int level, CallbackInfo ci) {
		if (!RingHudScenario.active) return;
		int expected = WeightRingRenderer.verticalOffset(Minecraft.getInstance());
		if (graphics.pose().m21() != -expected) throw new AssertionError("Vanilla XP layer offset differs: " + graphics.pose().m21());
		RingHudScenario.xpFrames++;
	}
}
