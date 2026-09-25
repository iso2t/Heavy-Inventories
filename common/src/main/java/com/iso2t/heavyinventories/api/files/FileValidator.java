package com.iso2t.heavyinventories.api.files;

import com.iso2t.heavyinventories.platform.Services;
import java.nio.file.Path;

/** Weight files are named by registry namespace, never by arbitrary caller paths. */
public final class FileValidator {
    private FileValidator() {}
    public static Path validate(String namespace) {
        if (namespace == null || !namespace.matches("[a-z0-9_.-]+"))
            throw new IllegalArgumentException("Invalid item namespace: " + namespace);
        return Services.PLATFORM.getGameDirectory().resolve("weights").resolve(namespace + ".json");
    }
}
