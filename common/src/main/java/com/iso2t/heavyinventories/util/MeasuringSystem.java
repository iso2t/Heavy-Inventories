package com.iso2t.heavyinventories.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MeasuringSystem {
	KGS("Metric", "Kilograms", "kg"),
	LBS("Imperial", "Pounds", "lbs"),
	NONE("None", "", "");

	final String unit;
	final String name;
	final String sub;

	/**
	 * Stored weights are pounds; preferences affect presentation only.
	 */
	public double fromStored (double pounds) {
		return this == KGS ? pounds * 0.45359237 : pounds;
	}

	@Override
	public String toString () {
		return String.format("%s (%s)", getUnit(), getName());
	}
}
