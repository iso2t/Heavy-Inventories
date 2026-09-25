package com.iso2t.heavyinventories.server.weight;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

/**
 * Reads only winning pack resources. Invalid candidates never expose partial definitions.
 */
public final class WeightPackData {

	public static final String DIRECTORY       = "heavyinventories/weights";
	public static final int    MAX_DEFINITIONS = 100_000;

	private WeightPackData () {
	}

	public record Entry(WeightDefinition definition, Identifier resource, String sourcePack) {
		public Entry {
			Objects.requireNonNull(definition);
			Objects.requireNonNull(resource);
			Objects.requireNonNull(sourcePack);
		}
	}

	public record Problem(String resource, String sourcePack, String message) {
		@Override
		public @NonNull String toString () {
			return resource + " [pack " + sourcePack + "]: " + message;
		}
	}

	public record Result(Map<Identifier, Entry> definitions, List<Problem> errors, List<Problem> warnings) {
		public Result {
			errors = List.copyOf(errors);
			warnings = List.copyOf(warnings);
			definitions = errors.isEmpty() ? Map.copyOf(definitions) : Map.of();
		}

		public boolean valid () {
			return errors.isEmpty();
		}
	}

	public static Result load (ResourceManager manager, Predicate<Identifier> registeredItem) {
		var definitions = new HashMap<Identifier, Entry>();
		var errors = new ArrayList<Problem>();
		var warnings = new ArrayList<Problem>();
		var resources = manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"));
		if (resources.size() > MAX_DEFINITIONS) return new Result(Map.of(), List.of(new Problem(DIRECTORY, "multiple", "Too many weight definitions (limit " + MAX_DEFINITIONS + ")")), List.of());
		// Stable ordering makes diagnostics reproducible across loaders and file systems.
		for (var resourceId : resources.keySet().stream().sorted(Comparator.comparing(Identifier::toString)).toList()) {
			var resource = resources.get(resourceId);
			var pack = resource.sourcePackId();
			try (var reader = resource.openAsReader()) {
				var item = itemId(resourceId);
				var definition = WeightDefinition.parse(reader);
				if (!registeredItem.test(item)) {
					warnings.add(new Problem(resourceId.toString(), pack, "Ignoring unregistered item " + item));
					continue;
				}
				definitions.put(item, new Entry(definition, resourceId, pack));
			} catch (IOException | IllegalArgumentException | IllegalStateException e) {
				errors.add(new Problem(resourceId.toString(), pack, e.getMessage()));
			}
		}
		return new Result(definitions, errors, warnings);
	}

	public static Identifier itemId (Identifier resource) {
		var path = resource.getPath();
		var prefix = DIRECTORY + "/";
		if (!path.startsWith(prefix) || !path.endsWith(".json")) throw new IllegalArgumentException("Not an item-weight resource: " + resource);
		var itemPath = path.substring(prefix.length(), path.length() - ".json".length());
		if (itemPath.isEmpty()) throw new IllegalArgumentException("Missing item path: " + resource);
		return Identifier.fromNamespaceAndPath(resource.getNamespace(), itemPath);
	}
}
