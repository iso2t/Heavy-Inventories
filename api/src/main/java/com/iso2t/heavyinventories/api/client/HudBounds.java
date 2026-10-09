package com.iso2t.heavyinventories.api.client;

/**
 * Top-left position and occupied size in scaled GUI pixels. Negative positions are allowed.
 */
public record HudBounds(int x, int y, int width, int height) {

	public HudBounds {
		if (width < 0 || height < 0) throw new IllegalArgumentException("HUD dimensions cannot be negative");
	}

}
