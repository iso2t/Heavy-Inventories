package com.iso2t.heavyinventories.server.weight;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

/**
 * Review format, deliberately distinct from legacy input and installable datapack definitions.
 */
public final class WeightReport {

	private WeightReport () {
	}

	public static JsonObject create (String namespace, long revision, Map<Identifier, Float> weights, Map<Identifier, WeightProvenance> sources) {
		var root = new JsonObject();
		root.addProperty("format", "heavyinventories:weight_report");
		root.addProperty("version", 1);
		root.addProperty("unit", "lb");
		root.addProperty("revision", revision);
		root.addProperty("note", "Review only, not an installable datapack. Recipe values may depend on fallback ingredients.");
		var entries = new JsonObject();
		weights.keySet().stream().filter(id -> id.getNamespace().equals(namespace)).sorted(Comparator.comparing(Identifier::toString)).forEach(id -> {
			var provenance = sources.get(id);
			if (provenance == null) throw new IllegalArgumentException("Missing provenance for " + id);
			var entry = new JsonObject();
			entry.addProperty("weight", weights.get(id));
			entry.addProperty("source", provenance.source().name().toLowerCase(Locale.ROOT));
			if (provenance.definition() != null) {
				var definition = new JsonObject();
				definition.addProperty("pack", provenance.definition().sourcePack());
				definition.addProperty("resource", provenance.definition().resource().toString());
				definition.addProperty("mode", provenance.definition().definition() instanceof WeightDefinition.Fixed ? "weight" : "infer");
				entry.add("definition", definition);
			}
			entries.add(id.toString(), entry);
		});
		root.add("items", entries);
		return root;
	}
}
