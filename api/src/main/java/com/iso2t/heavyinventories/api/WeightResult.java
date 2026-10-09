package com.iso2t.heavyinventories.api;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * A complete weight in pounds, or the reason no reliable total is available.
 */
public record WeightResult(Status status, OptionalDouble pounds) {

	public WeightResult {
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(pounds, "pounds");
		if ((status == Status.COMPLETE) != pounds.isPresent()) {
			throw new IllegalArgumentException("Only a complete result has a weight");
		}
		if (pounds.isPresent() && (!Double.isFinite(pounds.getAsDouble()) || pounds.getAsDouble() < 0)) {
			throw new IllegalArgumentException("Weight must be finite and nonnegative");
		}
	}

	public static WeightResult complete (double pounds) {
		return new WeightResult(Status.COMPLETE, OptionalDouble.of(pounds));
	}

	public static WeightResult unavailable () {
		return new WeightResult(Status.UNAVAILABLE, OptionalDouble.empty());
	}

	public static WeightResult unknownItem () {
		return new WeightResult(Status.UNKNOWN_ITEM, OptionalDouble.empty());
	}

	public static WeightResult incomplete () {
		return new WeightResult(Status.INCOMPLETE, OptionalDouble.empty());
	}

	public enum Status {
		COMPLETE,
		/**
		 * No active world or complete synchronized table yet.
		 */
		UNAVAILABLE,
		/**
		 * The requested item ID is not registered. A registered fallback item is COMPLETE.
		 */
		UNKNOWN_ITEM,
		/**
		 * Contents were unavailable, a provider failed, or calculation limits were exceeded.
		 */
		INCOMPLETE
	}

}
