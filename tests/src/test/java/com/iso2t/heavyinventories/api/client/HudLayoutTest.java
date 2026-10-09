package com.iso2t.heavyinventories.api.client;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HudLayoutTest {

	@Test
	void hiddenRingCannotLeaveAnXpOffset () {
		var bounds = new HudBounds(100, 100, 16, 16);
		assertThrows(IllegalArgumentException.class, () -> new HudLayout(bounds, false, 7));
		assertEquals(0, new HudLayout(bounds, false, 0).xpOffset());
	}

	@Test
	void onlyTheRingCanMoveXp () {
		var ring = new HudLayout(new HudBounds(100, 100, 16, 16), true, 7);
		var numbers = new HudLayout(new HudBounds(200, 100, 100, 20), true, 7);
		assertThrows(IllegalArgumentException.class, () -> new HudContext(Optional.empty(), 320, 240, false, true, true, ring, numbers));
		var corrected = new HudLayout(numbers.bounds(), true, 0);
		assertEquals(ring, new HudContext(Optional.empty(), 320, 240, false, true, true, ring, corrected).ring());
	}

}
