package com.iso2t.heavyinventories.api.player;

import com.iso2t.heavyinventories.api.weight.StackWeight;
import com.iso2t.heavyinventories.config.EffectsSettings;
import com.iso2t.heavyinventories.config.WalkingMode;

/**
 * Shared balance calculations; each mechanic uses these when its gameplay hooks are implemented.
 */
public final class EncumbranceEffects {
	private EncumbranceEffects () {
	}

	public record State(float exhaustionMultiplier, float walkingCostPerBlock, float fallMultiplier, float swimmingMultiplier, float sinkingMultiplier, boolean deniesUpwardMovement, float knockbackResistance) {
		public static final State NONE = new State(1, 0, 1, 1, 1, false, 0);
	}

	public enum Fluid {
		NONE,
		WATER,
		LAVA
	}

	/**
	 * allExempt covers creative/spectator; locomotionExempt additionally covers flying/gliding/riding.
	 */
	public static State calculate (float weight, float capacity, WalkingMode mode, EffectsSettings settings, Fluid fluid, boolean allExempt, boolean locomotionExempt) {
		if (!Float.isFinite(weight) || weight < 0 || !Float.isFinite(capacity) || capacity <= 0 || mode == null || settings == null || fluid == null) throw new IllegalArgumentException("Invalid encumbrance effect input");
		if (allExempt) return State.NONE;
		boolean unknown = weight == StackWeight.TOO_COMPLEX;
		double ratio = unknown ? Double.POSITIVE_INFINITY : (double) weight / capacity;
		double burden = Math.clamp(1 - Encumbrance.walkingBeforeSurefooted(ratio, mode), 0, 1);
		var e = settings.exhaustion();
		boolean exertion = e.enabled() && !locomotionExempt;
		var f = settings.fallDamage();
		var w = settings.swimming();
		var s = settings.sinking();
		var u = settings.upwardMovement();
		var k = settings.knockback();
		boolean inFluid = !locomotionExempt && (fluid == Fluid.WATER && settings.water() || fluid == Fluid.LAVA && settings.lava());
		return new State(exertion ? (float) (1 + (e.maxMultiplier() - 1) * burden) : 1, exertion ? (float) (e.walkingCostPerBlock() * burden) : 0, f.enabled() ? (float) (1 + (f.maxMultiplier() - 1) * ramp(ratio, f.startPercent(), f.fullPercent())) : 1, inFluid && w.enabled() ? (float) (1 - (1 - w.minMultiplier()) * ramp(ratio, w.startPercent(), w.fullPercent())) : 1, inFluid && s.enabled() ? (float) (1 + (s.maxMultiplier() - 1) * ramp(ratio, s.startPercent(), s.fullPercent())) : 1, inFluid && u.enabled() && ratio * 100 >= u.thresholdPercent(), k.enabled() && !unknown ? (float) (k.maxResistance() * Math.clamp((double) weight / k.referenceWeight(), 0, 1)) : 0);
	}

	private static double ramp (double ratio, float start, float full) {
		return Math.clamp((ratio * 100 - start) / ((double) full - start), 0, 1);
	}
}
