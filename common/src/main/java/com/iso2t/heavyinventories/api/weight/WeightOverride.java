package com.iso2t.heavyinventories.api.weight;

import com.iso2t.heavyinventories.command.ModCommands;
import com.iso2t.heavyinventories.server.ServerWeightState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Exports the active server table; legacy file-based getters and setters have been retired. */
public final class WeightOverride {
    private WeightOverride() {}

    /**
     * For writing dumps
     * @see ModCommands
     */
    public static java.nio.file.Path putDumpFile(String namespace, List<Item> items, List<Block> blocks, Level level) throws java.io.IOException {
        if (!namespace.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("Invalid namespace");
        var active = ServerWeightState.of(level.getServer()).weights();
        var targets = new java.util.TreeMap<Identifier, Item>(java.util.Comparator.comparing(Identifier::toString));
        items.forEach(item -> targets.put(BuiltInRegistries.ITEM.getKey(item), item));
        blocks.stream().map(Block::asItem).filter(item -> item != net.minecraft.world.item.Items.AIR)
                .forEach(item -> targets.put(BuiltInRegistries.ITEM.getKey(item), item));
        var json = new com.google.gson.JsonObject();
        targets.forEach((id, item) -> {
            var entry = new com.google.gson.JsonObject();
            entry.addProperty("weight", com.iso2t.heavyinventories.config.ServerSettings.validateItemWeight(active.getOrDefault(id, RecipeWeights.FALLBACK)));
            json.add(id.getPath(), entry);
        });
        // A unique reviewable export, outside world datapacks.
        var directory = com.iso2t.heavyinventories.platform.Services.PLATFORM.getGameDirectory().resolve("weight-exports");
        java.nio.file.Files.createDirectories(directory);
        var path = directory.resolve(namespace + "-" + java.util.UUID.randomUUID() + ".json");
        com.iso2t.heavyinventories.api.files.JsonFiles.writeObject(path, json);
        return path;
    }

}
