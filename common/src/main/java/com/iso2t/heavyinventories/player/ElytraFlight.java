package com.iso2t.heavyinventories.player;

import com.iso2t.heavyinventories.config.EffectsSettings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ElytraFlight {

	public record State(float liftMultiplier, float rocketMultiplier) {
		public static final State NONE = new State(1, 1);
	}

	public static State calculate (float weight, EffectsSettings.Elytra settings, boolean exempt) {
		if (!Float.isFinite(weight) || weight < 0) throw new IllegalArgumentException("Invalid flight weight");
		if (exempt || !settings.enabled()) return State.NONE;
		double load = Math.clamp((double) weight / settings.referenceWeight(), 0, 1);
		return new State((float) (1 - settings.maxLiftReduction() * load), (float) (1 - settings.maxRocketReduction() * load));
	}
}
