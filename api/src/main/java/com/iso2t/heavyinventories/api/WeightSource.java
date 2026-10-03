package com.iso2t.heavyinventories.api;

import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Optional;

/**
 * Server-side provenance; source details are not promised to clients.
 */
public record WeightSource(Kind kind, Optional<String> pack, Optional<Identifier> resource) {
	public WeightSource {
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(pack, "pack");
		Objects.requireNonNull(resource, "resource");
	}

	public enum Kind {
		EXPLICIT,
		RECIPE,
		FALLBACK,
		SESSION
	}
}
