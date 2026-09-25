package com.iso2t.heavyinventories.api.weight;

import com.iso2t.heavyinventories.command.ModCommands;
import com.iso2t.heavyinventories.api.resource.IResourceList;
import com.iso2t.heavyinventories.server.ServerWeightState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.iso2t.heavyinventories.api.files.DataType;
import com.iso2t.heavyinventories.api.files.ReadFile;
import com.iso2t.heavyinventories.api.files.WriteFile;

import java.util.List;

/**
 * If the default weight for an item is overridden in the .json file, with convention to its "modid.json",
 * we will load that weight from {@link ReadFile} here instead of using the default weight.
 */
public final class WeightOverride {

    private WeightOverride() {}

    public static float get(ItemLike itemLike) {
        return get(new ItemStack(itemLike, 1));
    }

    public static float get(ItemStack itemStack) {
        return ReadFile.get(itemStack.getItem(), DataType.WEIGHT) * itemStack.getCount();
    }

    public static void put(ItemLike itemLike, float weight) throws java.io.IOException {
        Identifier id = BuiltInRegistries.ITEM.getKey(itemLike.asItem());
        WriteFile.writeToFile(id.getNamespace(), id.getPath(), DataType.WEIGHT, weight);
    }

    /**
     * For writing dumps
     * @see ModCommands
     */
    public static java.nio.file.Path putDumpFile(String namespace, List<Item> items, List<Block> blocks, Level level) throws java.io.IOException {
        if (!namespace.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("Invalid namespace");
        var inferred = RecipeWeights.resolve(IResourceList.snapshot(level), ServerWeightState.of(level.getServer()).overrides());
        var targets = new java.util.TreeMap<Identifier, Item>(java.util.Comparator.comparing(Identifier::toString));
        items.forEach(item -> targets.put(BuiltInRegistries.ITEM.getKey(item), item));
        blocks.stream().map(Block::asItem).filter(item -> item != net.minecraft.world.item.Items.AIR)
                .forEach(item -> targets.put(BuiltInRegistries.ITEM.getKey(item), item));
        var json = new com.google.gson.JsonObject();
        targets.forEach((id, item) -> {
            var entry = new com.google.gson.JsonObject();
            entry.addProperty("weight", com.iso2t.heavyinventories.config.ServerSettings.validateItemWeight(inferred.getOrDefault(id, RecipeWeights.FALLBACK)));
            json.add(id.getPath(), entry);
        });
        // A unique reviewable export, outside the live weights directory.
        var directory = com.iso2t.heavyinventories.platform.Services.PLATFORM.getGameDirectory().resolve("weight-exports");
        java.nio.file.Files.createDirectories(directory);
        var path = directory.resolve(namespace + "-" + java.util.UUID.randomUUID() + ".json");
        com.iso2t.heavyinventories.api.files.JsonFiles.writeObject(path, json);
        return path;
    }

}
