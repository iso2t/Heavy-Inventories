package com.iso2t.heavyinventories.api.client;

import java.util.Objects;

/**
 * One element's resolved layout. Positive XP offset moves its number upward; zero leaves it alone.
 * Only the ring owner controls XP offset. A numeric layout must use zero.
 * Hidden layouts must use zero so hiding a ring cannot leave its XP displacement behind.
 */
public record HudLayout(HudBounds bounds, boolean visible, int xpOffset) {
	public HudLayout {
		Objects.requireNonNull(bounds, "bounds");
		if (!visible && xpOffset != 0) throw new IllegalArgumentException("A hidden element cannot move XP");
	}
}
