package com.iso2t.heavyinventories.api.weight;

import com.iso2t.heavyinventories.command.ModCommands;
import com.iso2t.heavyinventories.server.ServerWeightState;
import net.minecraft.world.level.Level;

/** Exports the active server table; legacy file-based getters and setters have been retired. */
public final class WeightOverride {
    private WeightOverride() {}

    /**
     * For writing dumps
     * @see ModCommands
     */
    public static java.nio.file.Path putDumpFile(String namespace, Level level) throws java.io.IOException {
        if (!namespace.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("Invalid namespace");
        var state = ServerWeightState.of(level.getServer());
        var json = com.iso2t.heavyinventories.server.weight.WeightReport.create(namespace, state.revision(), state.weights(), state.provenance());
        // A unique reviewable export, outside world datapacks.
        var directory = com.iso2t.heavyinventories.platform.Services.PLATFORM.getGameDirectory().resolve("weight-exports");
        java.nio.file.Files.createDirectories(directory);
        var path = directory.resolve(namespace + "-" + java.util.UUID.randomUUID() + ".json");
        com.iso2t.heavyinventories.api.files.JsonFiles.writeObject(path, json);
        return path;
    }

}
