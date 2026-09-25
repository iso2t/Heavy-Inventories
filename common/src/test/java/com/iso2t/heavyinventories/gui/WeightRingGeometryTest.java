package com.iso2t.heavyinventories.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeightRingGeometryTest {
	@Test
	void fillUsesEffectiveCapacityAndNeverOverflows () {
		assertEquals(0, WeightRingGeometry.filledRows(0, 1000));
		assertEquals(3, WeightRingGeometry.filledRows(250, 1000));
		assertEquals(7, WeightRingGeometry.filledRows(500, 1000));
		assertEquals(12, WeightRingGeometry.filledRows(899.99f, 1000));
		assertEquals(12, WeightRingGeometry.filledRows(900, 1000));
		assertEquals(13, WeightRingGeometry.filledRows(999.99f, 1000));
		assertEquals(14, WeightRingGeometry.filledRows(1000, 1000));
		assertEquals(14, WeightRingGeometry.filledRows(Float.MAX_VALUE, 1000));
		assertEquals(7, WeightRingGeometry.filledRows(1000, 2000));
		assertNotEquals(WeightRingGeometry.color(false, false), WeightRingGeometry.color(true, false));
		assertEquals(WeightRingGeometry.OVERLOADED, WeightRingGeometry.color(true, true));
	}

	@Test
	void maskMatchesTheSuppliedTextureInterior () throws Exception {
		try (var stream = getClass().getResourceAsStream("/assets/heavyinventories/textures/gui/hud_ring.png")) {
			assertNotNull(stream);
			var image = javax.imageio.ImageIO.read(stream);
			assertEquals(16, image.getWidth());
			assertEquals(16, image.getHeight());
			for (int row = 1; row <= 14; row++) {
				int left = WeightRingGeometry.left(row);
				assertNotEquals(0, image.getRGB(left - 1, row) >>> 24);
				assertNotEquals(0, image.getRGB(16 - left, row) >>> 24);
				for (int x = left; x < 16 - left; x++) assertEquals(0, image.getRGB(x, row) >>> 24);
			}
		}
	}
}
