package com.iso2t.heavyinventories.api.player;

import com.iso2t.heavyinventories.api.weight.StackWeight;
import com.iso2t.heavyinventories.config.EffectsSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncumbranceEffectsTest {
	private static EncumbranceEffects.State state (float weight, float capacity, WalkingMode mode, EffectsSettings settings, EncumbranceEffects.Fluid fluid) {
		return EncumbranceEffects.calculate(weight, capacity, mode, settings, fluid, false, false);
	}

	@Test
	void agreedBoundaryTableAndBothWalkingModes () {
		float[] weights = { 0, 500, 890, 900, 990, 1000, 1250 };
		double[] exhaustion = { 1, 1.14644661, 1.33416876, 1.34188612, 1.45, 1.5, 1.5 };
		double[] threshold = { 1, 1, 1, 1, 1.45, 1.5, 1.5 };
		double[] falls = { 1, 1, 1, 1, 1.25714286, 1.28571429, 2 };
		double[] swimming = { 1, 1, 1, 1, 0.55, 0.5, 0.5 };
		double[] sinking = { 1, 1, 1, 1, 1.9, 2, 2 };
		double[] resistance = { 0, 0.2, 0.356, 0.36, 0.396, 0.4, 0.4 };
		for (var fluid : new EncumbranceEffects.Fluid[] { EncumbranceEffects.Fluid.WATER, EncumbranceEffects.Fluid.LAVA }) {
			for (int i = 0; i < weights.length; i++) {
				var s = state(weights[i], 1000, WalkingMode.PROGRESSIVE, EffectsSettings.DEFAULT, fluid);
				assertEquals(exhaustion[i], s.exhaustionMultiplier(), 0.000001);
				assertEquals(falls[i], s.fallMultiplier(), 0.000001);
				assertEquals(swimming[i], s.swimmingMultiplier(), 0.000001);
				assertEquals(sinking[i], s.sinkingMultiplier(), 0.000001);
				assertEquals(resistance[i], s.knockbackResistance(), 0.000001);
				assertFalse(s.deniesUpwardMovement());
				var late = state(weights[i], 1000, WalkingMode.AT_NINETY_PERCENT, EffectsSettings.DEFAULT, fluid);
				assertEquals(threshold[i], late.exhaustionMultiplier(), 0.000001);
			}
		}
		assertEquals(0.01f, state(1000, 1000, WalkingMode.PROGRESSIVE, EffectsSettings.DEFAULT, EncumbranceEffects.Fluid.NONE).walkingCostPerBlock());
	}

	@Test
	void nondefaultCurvesAndIndependentDisabledFeatures () {
		var d = EffectsSettings.DEFAULT;
		var settings = new EffectsSettings(true, false, new EffectsSettings.Exhaustion(false, 3, 0.04f), new EffectsSettings.FallDamage(true, 50, 150, 4), new EffectsSettings.Swimming(false, 0, 50, 0.1f), new EffectsSettings.Sinking(true, 50, 150, 5), new EffectsSettings.UpwardMovement(true, 100), d.knockback());
		var s = state(1000, 1000, WalkingMode.PROGRESSIVE, settings, EncumbranceEffects.Fluid.WATER);
		assertEquals(1, s.exhaustionMultiplier());
		assertEquals(0, s.walkingCostPerBlock());
		assertEquals(2.5, s.fallMultiplier());
		assertEquals(1, s.swimmingMultiplier());
		assertEquals(3, s.sinkingMultiplier());
		assertTrue(s.deniesUpwardMovement());
		assertFalse(state(Math.nextDown(1000f), 1000, WalkingMode.PROGRESSIVE, settings, EncumbranceEffects.Fluid.WATER).deniesUpwardMovement());
		var lava = state(1000, 1000, WalkingMode.PROGRESSIVE, settings, EncumbranceEffects.Fluid.LAVA);
		assertEquals(1, lava.sinkingMultiplier());
		assertFalse(lava.deniesUpwardMovement());
		var disabled = new EffectsSettings(false, false, settings.exhaustion(), new EffectsSettings.FallDamage(false, 90, 125, 2), d.swimming(), d.sinking(), d.upwardMovement(), new EffectsSettings.Knockback(false, 1000, 0.4f));
		assertEquals(EncumbranceEffects.State.NONE, state(99999, 1000, WalkingMode.PROGRESSIVE, disabled, EncumbranceEffects.Fluid.WATER));
	}

	@Test
	void capacityBonusesReduceStrainButNotPhysicalWeightResistance () {
		var before = state(1000, 1000, WalkingMode.PROGRESSIVE, EffectsSettings.DEFAULT, EncumbranceEffects.Fluid.WATER);
		var after = state(1000, 2000, WalkingMode.PROGRESSIVE, EffectsSettings.DEFAULT, EncumbranceEffects.Fluid.WATER);
		assertEquals(before.knockbackResistance(), after.knockbackResistance());
		assertTrue(after.exhaustionMultiplier() < before.exhaustionMultiplier());
		assertEquals(1, after.fallMultiplier());
		assertEquals(1, after.swimmingMultiplier());
	}

	@Test
	void unknownWeightIsMaximumStrainWithoutResistanceAndExtremesStayFinite () {
		var s = state(StackWeight.TOO_COMPLEX, 1000, WalkingMode.PROGRESSIVE, EffectsSettings.DEFAULT, EncumbranceEffects.Fluid.WATER);
		assertEquals(1.5, s.exhaustionMultiplier());
		assertEquals(2, s.fallMultiplier());
		assertEquals(0.5, s.swimmingMultiplier());
		assertEquals(0, s.knockbackResistance());
		var huge = state(Math.nextDown(StackWeight.TOO_COMPLEX), Float.MIN_VALUE, WalkingMode.AT_NINETY_PERCENT, EffectsSettings.DEFAULT, EncumbranceEffects.Fluid.WATER);
		assertEquals(1.5, huge.exhaustionMultiplier());
		assertEquals(2, huge.fallMultiplier());
		assertEquals(0.4f, huge.knockbackResistance());
		assertEquals(EncumbranceEffects.State.NONE, EncumbranceEffects.calculate(99999, 1000, WalkingMode.PROGRESSIVE, EffectsSettings.DEFAULT, EncumbranceEffects.Fluid.WATER, true, false));
		var riding = EncumbranceEffects.calculate(1000, 1000, WalkingMode.PROGRESSIVE, EffectsSettings.DEFAULT, EncumbranceEffects.Fluid.WATER, false, true);
		assertEquals(1, riding.exhaustionMultiplier());
		assertEquals(1, riding.swimmingMultiplier());
		assertEquals(1, riding.sinkingMultiplier());
		assertTrue(riding.fallMultiplier() > 1);
		assertEquals(0.4f, riding.knockbackResistance());
		assertThrows(IllegalArgumentException.class, () -> state(Float.NaN, 1000, WalkingMode.PROGRESSIVE, EffectsSettings.DEFAULT, EncumbranceEffects.Fluid.NONE));
		assertThrows(IllegalArgumentException.class, () -> state(100, 0, WalkingMode.PROGRESSIVE, EffectsSettings.DEFAULT, EncumbranceEffects.Fluid.NONE));
	}
}
