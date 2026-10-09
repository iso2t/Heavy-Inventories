package com.iso2t.heavyinventories.api;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * The last completed player update. Safe to retain as data; it contains no player or world references.
 * Revision identifies the weight/settings table. Tick is the owning level's game time when this
 * side captured the state: calculation on the server, receipt on the client. It is not a shared clock.
 * Client reads apply locally known game-mode and movement exemptions without recalculating weight.
 * An absent snapshot means not ready; an incomplete carried weight means a calculation could not finish.
 */
public record PlayerWeightSnapshot(WeightResult carriedWeight, double baseCapacity, double capacity, EncumbranceState state, double walkingMultiplier, boolean effectsApply, long revision, long tick) {

	public PlayerWeightSnapshot {
		Objects.requireNonNull(carriedWeight, "carriedWeight");
		Objects.requireNonNull(state, "state");
		if (carriedWeight.status() != WeightResult.Status.COMPLETE && carriedWeight.status() != WeightResult.Status.INCOMPLETE) {
			throw new IllegalArgumentException("A player snapshot needs a completed calculation attempt");
		}
		if (!Double.isFinite(baseCapacity) || baseCapacity <= 0 || !Double.isFinite(capacity) || capacity <= 0) {
			throw new IllegalArgumentException("Capacity must be finite and positive");
		}
		if (!Double.isFinite(walkingMultiplier) || walkingMultiplier < 0 || walkingMultiplier > 1) {
			throw new IllegalArgumentException("Walking multiplier must be between zero and one");
		}
		if (revision < 0 || tick < 0) throw new IllegalArgumentException("Revision and tick must be nonnegative");
	}

	/**
	 * Unclamped load ratio. Empty when contents could not be fully counted.
	 */
	public OptionalDouble loadRatio () {
		return carriedWeight.pounds().isPresent() ? OptionalDouble.of(carriedWeight.pounds().getAsDouble() / capacity) : OptionalDouble.empty();
	}

}
