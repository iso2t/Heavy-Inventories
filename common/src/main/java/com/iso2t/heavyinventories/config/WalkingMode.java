package com.iso2t.heavyinventories.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Stable configuration IDs, independent of display text and the user's locale.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum WalkingMode {

	PROGRESSIVE("progressive"),
	AT_NINETY_PERCENT("at_ninety_percent");

	private final String id;

	public static WalkingMode parse (String id) {
		for (var mode : values()) if (mode.id.equals(id)) return mode;
		throw new IllegalArgumentException("walkingMode must be progressive or at_ninety_percent");
	}
}
