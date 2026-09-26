package com.iso2t.heavyinventories.api.player;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerExhaustionTest {
	@Test
	void idleAndSidewaysOrBackwardDriftDoNotCount () {
		assertEquals(0, PlayerExhaustion.voluntaryDistance(new Vec3(0, 0, 10), Vec3.ZERO));
		assertEquals(0, PlayerExhaustion.voluntaryDistance(new Vec3(10, 0, 0), new Vec3(0, 0, 1)));
		assertEquals(0, PlayerExhaustion.voluntaryDistance(new Vec3(0, 0, -10), new Vec3(0, 0, 1)));
		assertEquals(0, PlayerExhaustion.voluntaryDistance(new Vec3(Double.NaN, 0, 1), new Vec3(0, 0, 1)));
	}

	@Test
	void diagonalAndVerticalInputMeasureActualProgress () {
		assertEquals(Math.sqrt(2), PlayerExhaustion.voluntaryDistance(new Vec3(1, 0, 1), new Vec3(1, 0, 1)), 1e-8);
		assertEquals(1, PlayerExhaustion.voluntaryDistance(new Vec3(1, 0, 1), new Vec3(0, 0, 1)), 1e-8);
		assertEquals(2, PlayerExhaustion.voluntaryDistance(new Vec3(0, 2, 0), new Vec3(0, 1, 0)), 1e-8);
	}

	@Test
	void sprintAndSwimNeverAlsoAddWalkingCost () {
		assertEquals(0.05, PlayerExhaustion.extraMovementCost(0.1f, 1, 1, 1.5f, 0.01f), 1e-7);
		assertEquals(0.005, PlayerExhaustion.extraMovementCost(0.01f, 1, 1, 1.5f, 0.01f), 1e-7);
		assertEquals(0.01, PlayerExhaustion.extraMovementCost(0, 1, 1, 1.5f, 0.01f), 1e-7);
	}

	@Test
	void distancePartitionAndPartialIntentDoNotInflateCosts () {
		assertEquals(PlayerExhaustion.extraMovementCost(0, 1, 1, 1.5f, .01f),
				10 * PlayerExhaustion.extraMovementCost(0, .1, .1, 1.5f, .01f), 1e-7);
		assertEquals(.025, PlayerExhaustion.extraMovementCost(.1f, 1, .5, 1.5f, .01f), 1e-7);
		assertEquals(0, PlayerExhaustion.extraMovementCost(.1f, 0, 0, 1.5f, .01f));
		assertEquals(0, PlayerExhaustion.extraMovementCost(.1f, Double.NaN, 1, 1.5f, .01f));
	}
}
