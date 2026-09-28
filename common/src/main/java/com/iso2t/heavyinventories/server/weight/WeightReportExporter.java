package com.iso2t.heavyinventories.server.weight;

import com.iso2t.heavyinventories.platform.Services;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.util.JsonFiles;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Exports the active server table; legacy file-based getters and setters have been retired.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WeightReportExporter {

	public static Path export (String namespace, Level level) throws IOException {
		if (!namespace.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("Invalid namespace");
		var state = ServerWeightState.of(level.getServer());
		var json = WeightReport.create(namespace, state.revision(), state.weights(), state.provenance());

		// a reviewable export from datapacks
		var directory = Services.PLATFORM.getGameDirectory().resolve("weight-exports");
		Files.createDirectories(directory);
		var path = directory.resolve(namespace + "-" + UUID.randomUUID() + ".json");
		JsonFiles.writeObject(path, json);
		return path;
	}

}
