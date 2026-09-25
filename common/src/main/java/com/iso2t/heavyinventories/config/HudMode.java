package com.iso2t.heavyinventories.config;

public enum HudMode {
	RING,
	NUMBERS,
	BOTH;

	public boolean ring () {
		return this != NUMBERS;
	}

	public boolean numbers () {
		return this != RING;
	}
}
