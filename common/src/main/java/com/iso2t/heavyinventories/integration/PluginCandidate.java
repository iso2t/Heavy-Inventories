package com.iso2t.heavyinventories.integration;

import java.util.Set;
import java.util.function.Supplier;

public record PluginCandidate<T>(Set<String> owners, String className, Supplier<T> factory) {
	public PluginCandidate {
		owners = Set.copyOf(owners);
		if (owners.isEmpty() || className.isBlank()) throw new IllegalArgumentException("Plugin needs an owner and class name");
	}

	public String description () {
		return className + " from " + owners.stream().sorted().toList();
	}
}
