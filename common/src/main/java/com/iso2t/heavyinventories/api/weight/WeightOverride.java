package com.iso2t.heavyinventories.api.weight;

import com.iso2t.heavyinventories.api.files.JsonFiles;
import com.iso2t.heavyinventories.command.ModCommands;
import com.iso2t.heavyinventories.platform.Services;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.server.weight.WeightReport;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Exports the active server table; legacy file-based getters and setters have been retired.
 */
public final class WeightOverride {

	private WeightOverride () {
	}

	/**
	 * For writing dumps
	 *
	 * @see ModCommands
	 */
	public static Path putDumpFile (String namespace, Level level) throws IOException {
		if (!namespace.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("Invalid namespace");
		var state = ServerWeightState.of(level.getServer());
		var json = WeightReport.create(namespace, state.revision(), state.weights(), state.provenance());
		// A unique reviewable export, outside world datapacks.
		var directory = Services.PLATFORM.getGameDirectory().resolve("weight-exports");
		Files.createDirectories(directory);
		var path = directory.resolve(namespace + "-" + java.util.UUID.randomUUID() + ".json");
		JsonFiles.writeObject(path, json);
		return path;
	}

}
