package com.iso2t.heavyinventories.player;

import com.iso2t.heavyinventories.config.EffectsSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import com.iso2t.heavyinventories.weight.StackWeight;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Shared balance calculations for exhaustion, fall damage, fluid movement, and knockback.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EncumbranceEffects {

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
		var exhaustion = settings.exhaustion();
		boolean exertion = exhaustion.enabled() && !locomotionExempt;
		var fallDamage = settings.fallDamage();
		var swimming = settings.swimming();
		var sinking = settings.sinking();
		var upwardMovement = settings.upwardMovement();
		var knockback = settings.knockback();
		boolean inFluid = !locomotionExempt && (fluid == Fluid.WATER && settings.water() || fluid == Fluid.LAVA && settings.lava());
		return new State(exertion ? (float) (1 + (exhaustion.maxMultiplier() - 1) * burden) : 1, exertion ? (float) (exhaustion.walkingCostPerBlock() * burden) : 0, fallDamage.enabled() ? (float) (1 + (fallDamage.maxMultiplier() - 1) * ramp(ratio, fallDamage.startPercent(), fallDamage.fullPercent())) : 1, inFluid && swimming.enabled() ? (float) (1 - (1 - swimming.minMultiplier()) * ramp(ratio, swimming.startPercent(), swimming.fullPercent())) : 1, inFluid && sinking.enabled() ? (float) (1 + (sinking.maxMultiplier() - 1) * ramp(ratio, sinking.startPercent(), sinking.fullPercent())) : 1, inFluid && upwardMovement.enabled() && ratio * 100 >= upwardMovement.thresholdPercent(), knockback.enabled() && !unknown ? (float) (knockback.maxResistance() * Math.clamp((double) weight / knockback.referenceWeight(), 0, 1)) : 0);
	}

	private static double ramp (double ratio, float start, float full) {
		return Math.clamp((ratio * 100 - start) / ((double) full - start), 0, 1);
	}
}
