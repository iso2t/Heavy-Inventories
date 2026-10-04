package com.iso2t.heavyinventories.api;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

class WeightResultTest {
	@Test
	void unavailableAndIncompleteWeightsCannotBeMistakenForZero () {
		assertEquals(0, WeightResult.complete(0).pounds().orElseThrow());
		assertEquals(0.1, WeightResult.complete(0.1).pounds().orElseThrow());
		assertTrue(WeightResult.unavailable().pounds().isEmpty());
		assertTrue(WeightResult.incomplete().pounds().isEmpty());
		assertEquals(WeightResult.Status.UNKNOWN_ITEM, WeightResult.unknownItem().status());
	}

	@Test
	void rejectsInconsistentAndInvalidResults () {
		for (double weight : new double[] { -1, Double.NaN, Double.POSITIVE_INFINITY }) {
			assertThrows(IllegalArgumentException.class, () -> WeightResult.complete(weight));
		}
		assertThrows(IllegalArgumentException.class, () -> new WeightResult(WeightResult.Status.COMPLETE, OptionalDouble.empty()));
		assertThrows(IllegalArgumentException.class, () -> new WeightResult(WeightResult.Status.INCOMPLETE, OptionalDouble.of(10)));
	}

	@Test
	void overloadRatioIsNotClampedAndIncompleteRatioIsNotInvented () {
		var overloaded = new PlayerWeightSnapshot(WeightResult.complete(1250), 1000, 1000, EncumbranceState.OVERLOADED, 0, true, 1, 20);
		assertEquals(1.25, overloaded.loadRatio().orElseThrow());
		var incomplete = new PlayerWeightSnapshot(WeightResult.incomplete(), 1000, 1000, EncumbranceState.OVERLOADED, 0, true, 1, 20);
		assertTrue(incomplete.loadRatio().isEmpty());
		assertThrows(IllegalArgumentException.class, () -> new PlayerWeightSnapshot(WeightResult.unavailable(), 1000, 1000, EncumbranceState.NORMAL, 1, true, 1, 20));
		assertThrows(IllegalArgumentException.class, () -> new PlayerWeightSnapshot(WeightResult.complete(5), 1000, 0, EncumbranceState.NORMAL, 1, true, 1, 20));
	}
}
