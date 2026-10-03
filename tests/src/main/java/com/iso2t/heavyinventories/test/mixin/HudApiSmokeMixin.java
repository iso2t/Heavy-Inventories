package com.iso2t.heavyinventories.test.mixin;

import com.iso2t.heavyinventories.test.HudApiScenario;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class HudApiSmokeMixin {
	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
	private void fixtureFrame (Hud hud, GuiGraphicsExtractor graphics, DeltaTracker delta, Operation<Void> original) {
		HudApiScenario.begin(graphics);
		original.call(hud, graphics, delta);

	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void endFrame (CallbackInfo ci) {
		HudApiScenario.end();
	}
}
