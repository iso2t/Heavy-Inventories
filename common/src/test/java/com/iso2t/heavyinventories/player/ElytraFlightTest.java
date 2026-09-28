package com.iso2t.heavyinventories.player;

import com.iso2t.heavyinventories.config.EffectsSettings;
import com.iso2t.heavyinventories.weight.StackWeight;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ElytraFlightTest {

	@Test
	void weightScalesSmoothlyToACappedPenalty () {
		var settings = EffectsSettings.DEFAULT.elytra();
		assertEquals(ElytraFlight.State.NONE, ElytraFlight.calculate(0, settings, false));
		var half = ElytraFlight.calculate(500, settings, false);
		assertEquals(.925f, half.liftMultiplier(), .000001f);
		assertEquals(.875f, half.rocketMultiplier(), .000001f);
		var full = ElytraFlight.calculate(1000, settings, false);
		assertEquals(.85f, full.liftMultiplier(), .000001f);
		assertEquals(.75f, full.rocketMultiplier(), .000001f);
		assertEquals(full, ElytraFlight.calculate(2000, settings, false));
		assertEquals(full, ElytraFlight.calculate(StackWeight.TOO_COMPLEX, settings, false));
		assertEquals(full.liftMultiplier(), ElytraFlight.calculate(Math.nextDown(1000f), settings, false).liftMultiplier(), .000001f);
	}

	@Test
	void disablingOrExemptionRestoresVanillaAndPenaltiesCanBeDisabledSeparately () {
		assertEquals(ElytraFlight.State.NONE, ElytraFlight.calculate(2000, EffectsSettings.DEFAULT.elytra(), true));
		assertEquals(ElytraFlight.State.NONE, ElytraFlight.calculate(2000, new EffectsSettings.Elytra(false, 1000, 1, 1), false));
		var glideOnly = ElytraFlight.calculate(1000, new EffectsSettings.Elytra(true, 500, .4f, 0), false);
		assertEquals(.6f, glideOnly.liftMultiplier());
		assertEquals(1, glideOnly.rocketMultiplier());
		var rocketOnly = ElytraFlight.calculate(1000, new EffectsSettings.Elytra(true, 2000, 0, .8f), false);
		assertEquals(1, rocketOnly.liftMultiplier());
		assertEquals(.6f, rocketOnly.rocketMultiplier());
	}

	@Test
	void extremeConfigurationStaysFiniteAndInvalidWeightsAreRejected () {
		assertEquals(new ElytraFlight.State(0, 0), ElytraFlight.calculate(Float.MAX_VALUE, new EffectsSettings.Elytra(true, Float.MIN_VALUE, 1, 1), false));
		for (float weight : new float[] { -1, Float.NaN, Float.POSITIVE_INFINITY })
			assertThrows(IllegalArgumentException.class, () -> ElytraFlight.calculate(weight, EffectsSettings.DEFAULT.elytra(), false));
	}
}
