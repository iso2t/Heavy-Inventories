package com.iso2t.heavyinventories.server.weight;

import com.iso2t.heavyinventories.api.weight.RecipeWeights;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Complete gameplay candidate: datapack anchors, then recipes, then fallback.
 */
public record ResolvedWeights(Map<Identifier, Float> weights, Map<Identifier, Float> explicitWeights, Map<Identifier, WeightProvenance> provenance) {

	public ResolvedWeights {
		weights = Map.copyOf(weights);
		explicitWeights = Map.copyOf(explicitWeights);
		provenance = Map.copyOf(provenance);
	}

	public static ResolvedWeights resolve (WeightPackData.Result data, Collection<RecipeWeights.Recipe> recipes, Set<Identifier> registeredItems) {
		if (!data.valid()) throw new IllegalArgumentException("Invalid weight datapack: " + data.errors().getFirst());
		var fixed = new HashMap<Identifier, Float>();
		data.definitions().forEach((item, entry) -> {
			if (registeredItems.contains(item) && entry.definition() instanceof WeightDefinition.Fixed(float weight)) fixed.put(item, weight);
		});
		var inferred = RecipeWeights.resolveWithSources(recipes, fixed);
		var complete = new HashMap<Identifier, Float>();
		var sources = new HashMap<Identifier, WeightProvenance>();
		for (var item : registeredItems) {
			complete.put(item, inferred.weights().getOrDefault(item, RecipeWeights.FALLBACK));
			var source = fixed.containsKey(item) ? WeightProvenance.Source.EXPLICIT : inferred.inferred().contains(item) ? WeightProvenance.Source.RECIPE : WeightProvenance.Source.FALLBACK;
			sources.put(item, new WeightProvenance(source, data.definitions().get(item)));
		}
		return new ResolvedWeights(complete, fixed, sources);
	}
}
