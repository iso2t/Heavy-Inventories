package com.iso2t.heavyinventories.api.player;

import com.iso2t.heavyinventories.api.weight.StackWeight;
import com.iso2t.heavyinventories.config.WalkingMode;

/**
 * Pure balance rules; all bonuses are additive percentages of the configured base capacity.
 */
public final class Encumbrance {

	private Encumbrance () {
	}

	public record State(float bracing, float reinforced, float strength, float capacity, boolean encumbered, boolean overloaded, float walkingMultiplier) {
	}

	public static State calculate (float weight, float base, int bracingLevel, int reinforcedLevel, int strengthLevel, int surefootedLevel, WalkingMode mode, boolean exempt) {
		float bracing = base * (Math.clamp(bracingLevel, 0, 10) * 0.1f);
		float reinforced = base * (Math.clamp(reinforcedLevel, 0, 5) * 0.05f);
		float strength = base * (Math.clamp(strengthLevel, 0, 256) * 0.1f);
		float capacity = base + bracing + reinforced + strength;
		double ratio = (double) weight / capacity;
		boolean over = !exempt && (weight == StackWeight.TOO_COMPLEX || ratio >= 1);
		boolean encumbered = !exempt && !over && ratio >= 0.9;
		double multiplier = exempt ? 1 : mode == WalkingMode.PROGRESSIVE ? Math.sqrt(Math.max(0, 1 - ratio)) : Math.clamp((1 - ratio) / 0.1, 0, 1);
		int boots = Math.clamp(surefootedLevel, 0, 4);
		if (over) multiplier = boots * 0.05;
		else if (encumbered && boots > 0) multiplier = Math.max(multiplier, Math.max(0.25, boots * 0.1));
		return new State(bracing, reinforced, strength, capacity, encumbered, over, (float) multiplier);
	}
}
