package com.iso2t.heavyinventories.gui;

/**
 * Pixel mask for the transparent interior of the supplied 16x16 hud_ring.png.
 */
public final class WeightRingGeometry {
	private static final int[] LEFT       = { 0, 6, 4, 3, 2, 2, 1, 1, 1, 1, 2, 2, 3, 4, 6, 0 };
	public static final  int   EMPTY      = 0xFF282D26;
	public static final  int   NORMAL     = 0xFF397541;
	public static final  int   ENCUMBERED = 0xFF9C7926;
	public static final  int   OVERLOADED = 0xFFA43D35;

	private WeightRingGeometry () {
	}

	public static int left (int row) {
		return LEFT[row];
	}

	public static int filledRows (float weight, float capacity) {
		if (!Float.isFinite(weight) || !Float.isFinite(capacity) || capacity <= 0) return 14;
		return (int) Math.floor(Math.clamp((double) weight / capacity, 0, 1) * 14);
	}

	public static int color (boolean encumbered, boolean overloaded) {
		return overloaded ? OVERLOADED : encumbered ? ENCUMBERED : NORMAL;
	}
}
