package com.iso2t.heavyinventories.server.weight;

import java.util.Optional;

/**
 * A candidate is staged on its resource manager, then retained on that generation's recipe manager.
 * Neither owner is process-wide; failed reloads cannot replace the active server's candidate.
 */
public interface WeightPackAccess {

	Optional<WeightPackData.Result> heavyinventories$getWeightPackData ();

	void heavyinventories$setWeightPackData (WeightPackData.Result result);

}
