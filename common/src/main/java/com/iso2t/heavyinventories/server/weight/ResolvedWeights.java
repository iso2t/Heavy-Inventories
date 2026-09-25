package com.iso2t.heavyinventories.server.weight;

import com.iso2t.heavyinventories.api.weight.RecipeWeights;
import net.minecraft.resources.Identifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Complete gameplay candidate: datapack anchors, then recipes, then fallback. */
public record ResolvedWeights(Map<Identifier, Float> weights, Map<Identifier, Float> explicitWeights) {
    public ResolvedWeights {
        weights = Map.copyOf(weights);
        explicitWeights = Map.copyOf(explicitWeights);
    }

    public static ResolvedWeights resolve(WeightPackData.Result data, Collection<RecipeWeights.Recipe> recipes,
                                         Set<Identifier> registeredItems) {
        if (!data.valid()) throw new IllegalArgumentException("Invalid weight datapack: " + data.errors().getFirst());
        var fixed = new HashMap<Identifier, Float>();
        data.definitions().forEach((item, entry) -> {
            if (registeredItems.contains(item) && entry.definition() instanceof WeightDefinition.Fixed value)
                fixed.put(item, value.weight());
        });
        var inferred = RecipeWeights.resolve(recipes, fixed);
        var complete = new HashMap<Identifier, Float>();
        for (var item : registeredItems) complete.put(item, inferred.getOrDefault(item, RecipeWeights.FALLBACK));
        return new ResolvedWeights(complete, fixed);
    }
}
