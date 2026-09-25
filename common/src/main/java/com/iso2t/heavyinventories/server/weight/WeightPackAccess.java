package com.iso2t.heavyinventories.server.weight;

import java.util.Optional;

/**
 * Data belongs to one resource-manager generation, never to a process-wide cache.
 */
public interface WeightPackAccess {

	Optional<WeightPackData.Result> heavyinventories$getWeightPackData ();

	void heavyinventories$setWeightPackData (WeightPackData.Result result);
}
