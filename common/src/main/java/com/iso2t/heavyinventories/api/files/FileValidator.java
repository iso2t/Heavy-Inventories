package com.iso2t.heavyinventories.api.files;

import com.iso2t.heavyinventories.platform.Services;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

/**
 * Weight files are named by registry namespace, never by arbitrary caller paths.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileValidator {

	public static Path validate (String namespace) {
		if (namespace == null || !namespace.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("Invalid item namespace: " + namespace);
		return Services.PLATFORM.getGameDirectory().resolve("weights").resolve(namespace + ".json");
	}
}
