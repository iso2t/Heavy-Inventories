package com.iso2t.heavyinventories.api.weight;

import com.iso2t.heavyinventories.config.ServerSettings;
import net.minecraft.resources.Identifier;

import java.util.*;

/**
 * One immutable inference run. No process-wide cache or file access during calculation.
 */
public final class RecipeWeights {

	public static final  float                  FALLBACK         = 0.1f;
	public static final  int                    MAX_RECIPES      = 100_000;
	public static final  int                    MAX_ALTERNATIVES = 1_000_000;
	private static final Comparator<Identifier> ORDER            = Comparator.comparing(Identifier::toString);

	private RecipeWeights () {
	}

	/**
	 * Each slot consumes one item chosen from its alternatives; repeated slots retain multiplicity.
	 */
	public record Recipe(Identifier output, int outputCount, List<List<Identifier>> slots) {
		public Recipe {
			Objects.requireNonNull(output);
			if (outputCount <= 0 || slots.isEmpty()) throw new IllegalArgumentException("Recipe must consume and produce items");
			slots = slots.stream().map(slot -> {
				if (slot.isEmpty()) throw new IllegalArgumentException("Empty ingredient alternatives");
				return slot.stream().distinct().sorted(ORDER).toList();
			}).sorted(Comparator.comparing(Object::toString)).toList();
		}
	}

	public record Resolution(Map<Identifier, Float> weights, Set<Identifier> inferred) {
		public Resolution {
			weights = Map.copyOf(weights);
			inferred = Set.copyOf(inferred);
		}
	}

	public static Map<Identifier, Float> resolve (Collection<Recipe> recipes, Map<Identifier, Float> overrides) {
		return resolveWithSources(recipes, overrides).weights();
	}

	public static Resolution resolveWithSources (Collection<Recipe> recipes, Map<Identifier, Float> overrides) {
		if (recipes.size() > MAX_RECIPES) throw new IllegalArgumentException("Too many recipes to infer safely");
		overrides.values().forEach(ServerSettings::validateItemWeight);
		var graph = new TreeMap<Identifier, Set<Identifier>>(ORDER);
		var byOutput = new HashMap<Identifier, List<Recipe>>();
		int alternatives = 0;
		for (var id : overrides.keySet()) graph.put(id, new TreeSet<>(ORDER));
		for (var recipe : recipes) {
			var edges = graph.computeIfAbsent(recipe.output, unused -> new TreeSet<>(ORDER));
			byOutput.computeIfAbsent(recipe.output, unused -> new ArrayList<>()).add(recipe);
			for (var slot : recipe.slots) {
				alternatives += slot.size();
				if (alternatives > MAX_ALTERNATIVES) throw new IllegalArgumentException("Too many ingredient alternatives to infer safely");
				for (var item : slot) {
					graph.computeIfAbsent(item, unused -> new TreeSet<>(ORDER));
					if (!overrides.containsKey(recipe.output)) edges.add(item);
				}
			}
		}

		// Iterative Kosaraju avoids Java-stack recursion even for long modpack recipe chains.
		var reverse = new HashMap<Identifier, List<Identifier>>();
		graph.keySet().forEach(id -> reverse.put(id, new ArrayList<>()));
		graph.forEach((id, edges) -> edges.forEach(edge -> reverse.get(edge).add(id)));
		var visited = new HashSet<Identifier>();
		var finished = new ArrayList<Identifier>();
		record Frame(Identifier id, Iterator<Identifier> edges) {
		}
		for (var root : graph.keySet()) {
			if (!visited.add(root)) continue;
			var stack = new ArrayDeque<Frame>();
			stack.push(new Frame(root, graph.get(root).iterator()));
			while (!stack.isEmpty()) {
				var frame = stack.peek();
				if (frame.edges.hasNext()) {
					var child = frame.edges.next();
					if (visited.add(child)) stack.push(new Frame(child, graph.get(child).iterator()));
				} else {
					finished.add(frame.id);
					stack.pop();
				}
			}
		}
		var component = new HashMap<Identifier, Integer>();
		var groups = new ArrayList<List<Identifier>>();
		for (int i = finished.size() - 1; i >= 0; i--) {
			var root = finished.get(i);
			if (component.containsKey(root)) continue;
			int group = groups.size();
			var members = new ArrayList<Identifier>();
			var queue = new ArrayDeque<Identifier>();
			component.put(root, group);
			queue.add(root);
			while (!queue.isEmpty()) {
				var item = queue.remove();
				members.add(item);
				for (var parent : reverse.get(item)) {
					if (component.putIfAbsent(parent, group) == null) queue.add(parent);
				}
			}
			groups.add(members);
		}

		var result = new HashMap<>(overrides);
		var inferred = new HashSet<Identifier>();
		// Dependencies outside a strongly connected component are resolved before its outputs.
		for (int group = groups.size() - 1; group >= 0; group--) {
			for (var output : groups.get(group)) {
				if (overrides.containsKey(output)) continue;
				double best = Double.POSITIVE_INFINITY;
				for (var recipe : byOutput.getOrDefault(output, List.of())) {
					double total = 0;
					for (var slot : recipe.slots) {
						double ingredient = Double.POSITIVE_INFINITY;
						for (var candidate : slot) {
							// A dependency within this cycle is not a trustworthy mass anchor.
							if (component.get(candidate) != group) ingredient = Math.min(ingredient, result.get(candidate));
						}
						total += ingredient;
					}
					double weight = total / recipe.outputCount;
					if (Double.isFinite(weight) && weight <= ServerSettings.MAX_VALUE) best = Math.min(best, weight);
				}
				if (Double.isFinite(best)) inferred.add(output);
				result.put(output, Double.isFinite(best) ? (float) best : FALLBACK);
			}
		}
		return new Resolution(result, inferred);
	}
}
