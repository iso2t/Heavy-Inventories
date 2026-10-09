package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.gui.HudGraphicsState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class HudGraphicsStateMixin implements HudGraphicsState {

	@Shadow
	@Final
	@Mutable
	private Matrix3x2fStack pose;

	@Unique
	private boolean         heavyinventories$scoped;

	@Unique
	private int             heavyinventories$clips;

	@Override
	public void heavyinventories$scoped (Runnable draw) {
		var previousPose = pose;
		boolean previousScope = heavyinventories$scoped;
		int previousClips = heavyinventories$clips;
		pose = new Matrix3x2fStack(16);
		pose.set(previousPose);
		heavyinventories$scoped = true;
		heavyinventories$clips = 0;
		try {
			draw.run();
		} finally {
			while (heavyinventories$clips > 0) ((GuiGraphicsExtractor) (Object) this).disableScissor();
			pose = previousPose;
			heavyinventories$scoped = previousScope;
			heavyinventories$clips = previousClips;
		}
	}

	@Inject(method = "enableScissor", at = @At("TAIL"))
	private void heavyinventories$pushedClip (int x0, int y0, int x1, int y1, CallbackInfo ci) {
		if (heavyinventories$scoped) heavyinventories$clips++;
	}

	@Inject(method = "disableScissor", at = @At("HEAD"))
	private void heavyinventories$protectParentClip (CallbackInfo ci) {
		if (heavyinventories$scoped && heavyinventories$clips == 0) throw new IllegalStateException("HUD hook cannot remove its caller's scissor");
	}

	@Inject(method = "disableScissor", at = @At("TAIL"))
	private void heavyinventories$poppedClip (CallbackInfo ci) {
		if (heavyinventories$scoped) heavyinventories$clips--;
	}

}
