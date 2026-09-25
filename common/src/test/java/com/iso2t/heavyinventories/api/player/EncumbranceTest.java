package com.iso2t.heavyinventories.api.player;

import com.iso2t.heavyinventories.api.weight.StackWeight;
import com.iso2t.heavyinventories.config.WalkingMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EncumbranceTest {
    private static Encumbrance.State state(float weight, WalkingMode mode) {
        return Encumbrance.calculate(weight, 1000, 0, 0, 0, 0, mode, false);
    }

    @Test void thresholdsHaveNoGapsIncludingExtremeOverload() {
        for (var mode : WalkingMode.values()) {
            assertFalse(state(Math.nextDown(900f), mode).encumbered());
            assertTrue(state(900, mode).encumbered());
            assertTrue(state(Math.nextDown(1000f), mode).encumbered());
            for (float weight : new float[]{1000, 1100, 1150, 1250, 9999, StackWeight.TOO_COMPLEX}) {
                var result = state(weight, mode);
                assertTrue(result.overloaded());
                assertFalse(result.encumbered());
                assertEquals(0, result.walkingMultiplier());
            }
        }
    }

    @Test void progressiveAndNinetyPercentModesHaveTheirOwnCurves() {
        assertEquals(1, state(0, WalkingMode.PROGRESSIVE).walkingMultiplier());
        assertEquals(Math.sqrt(0.5), state(500, WalkingMode.PROGRESSIVE).walkingMultiplier(), 0.000001);
        assertEquals(Math.sqrt(0.1), state(900, WalkingMode.PROGRESSIVE).walkingMultiplier(), 0.000001);
        assertEquals(1, state(899, WalkingMode.AT_NINETY_PERCENT).walkingMultiplier());
        assertEquals(1, state(900, WalkingMode.AT_NINETY_PERCENT).walkingMultiplier(), 0.000001);
        assertEquals(0.5, state(950, WalkingMode.AT_NINETY_PERCENT).walkingMultiplier(), 0.000001);
        assertEquals(0.1, state(990, WalkingMode.AT_NINETY_PERCENT).walkingMultiplier(), 0.000001);
    }

    @Test void strengthAndArmorAreAdditiveAndCapacityChangesRebuildBonuses() {
        var result = Encumbrance.calculate(1000, 1000, 10, 5, 2, 0, WalkingMode.PROGRESSIVE, false);
        assertEquals(2450, result.capacity());
        assertEquals(1000, result.bracing());
        assertEquals(250, result.reinforced());
        assertEquals(200, result.strength());
        var changed = Encumbrance.calculate(1000, 2000, 10, 5, 2, 0, WalkingMode.PROGRESSIVE, false);
        assertEquals(4900, changed.capacity());
        var removed = Encumbrance.calculate(1000, 1000, 0, 0, 0, 0, WalkingMode.PROGRESSIVE, false);
        assertEquals(1000, removed.capacity());
        assertTrue(removed.overloaded());
    }

    @Test void strengthThresholdsStayContinuous() {
        assertFalse(Encumbrance.calculate(1079, 1000, 0, 0, 2, 0, WalkingMode.PROGRESSIVE, false).encumbered());
        assertTrue(Encumbrance.calculate(1080, 1000, 0, 0, 2, 0, WalkingMode.PROGRESSIVE, false).encumbered());
        assertTrue(Encumbrance.calculate(1200, 1000, 0, 0, 2, 0, WalkingMode.PROGRESSIVE, false).overloaded());
        assertTrue(Encumbrance.calculate(2000, 1000, 0, 0, 2, 0, WalkingMode.PROGRESSIVE, false).overloaded());
    }

    @Test void surefootedFloorsFollowLoadWithoutChangingEnchantmentLevel() {
        for (var mode : WalkingMode.values()) {
            for (int level = 1; level <= 4; level++) {
                var encumbered = Encumbrance.calculate(999, 1000, 0, 0, 0, level, mode, false);
                assertEquals(Math.max(0.25, level * 0.1), encumbered.walkingMultiplier(), 0.000001);
                var overloaded = Encumbrance.calculate(1500, 1000, 0, 0, 0, level, mode, false);
                assertEquals(level * 0.05, overloaded.walkingMultiplier(), 0.000001);
                assertEquals(1, Encumbrance.calculate(0, 1000, 0, 0, 0, level, mode, false).walkingMultiplier());
            }
        }
    }

    @Test void highLevelsAreCappedAndExemptPlayersKeepNormalMovement() {
        var result = Encumbrance.calculate(99999, 1000, 255, 255, 256, 255, WalkingMode.PROGRESSIVE, false);
        assertEquals(1000, result.bracing());
        assertEquals(250, result.reinforced());
        assertEquals(0.2f, result.walkingMultiplier());
        var exempt = Encumbrance.calculate(StackWeight.TOO_COMPLEX, 1000, 10, 5, 2, 4, WalkingMode.PROGRESSIVE, true);
        assertFalse(exempt.encumbered());
        assertFalse(exempt.overloaded());
        assertEquals(1, exempt.walkingMultiplier());
    }
}
