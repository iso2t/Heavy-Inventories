package com.iso2t.heavyinventories.integration;

import com.iso2t.heavyinventories.HeavyInventories;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PluginLoader {

	public record Loaded<T>(Identifier id, String description, T plugin) {

	}

	public static <T> List<Loaded<T>> load (List<PluginCandidate<T>> candidates, Function<T, Identifier> identifier, boolean optional) {
		var loaded = new ArrayList<Loaded<T>>();
		var seen = new HashSet<Identifier>();
		var duplicates = new HashSet<Identifier>();
		for (var candidate : candidates.stream().sorted(Comparator.comparing(PluginCandidate::description)).toList()) {
			try {
				var plugin = Objects.requireNonNull(candidate.factory().get(), "Plugin factory returned null");
				var id = Objects.requireNonNull(identifier.apply(plugin), "Plugin ID is null");
				if (!candidate.owners().contains(id.getNamespace())) throw new IllegalArgumentException("Plugin ID " + id + " must belong to its owning mod " + candidate.owners());
				if (!seen.add(id)) duplicates.add(id);
				loaded.add(new Loaded<>(id, candidate.description(), plugin));
			} catch (RuntimeException | LinkageError e) {
				if (!optional) throw new IllegalStateException("Invalid HI plugin " + candidate.description(), e);
				HeavyInventories.LOGGER.error("Disabled HI client plugin {}", candidate.description(), e);
			}
		}
		if (!duplicates.isEmpty()) {
			var descriptions = loaded.stream().filter(p -> duplicates.contains(p.id())).map(p -> p.id() + " (" + p.description() + ")").toList();
			if (!optional) throw new IllegalStateException("Duplicate HI plugin IDs: " + descriptions);
			HeavyInventories.LOGGER.error("Disabled HI client plugins with duplicate IDs: {}", descriptions);
			loaded.removeIf(plugin -> duplicates.contains(plugin.id()));
		}
		loaded.sort(Comparator.comparing(plugin -> plugin.id().toString()));
		return List.copyOf(loaded);
	}

}
