package com.iso2t.heavyinventories.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.network.ItemWeightsPayload;
import com.iso2t.heavyinventories.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Gameplay definitions/configuration live as long as this logical server. Access on its thread. */
public final class ServerWeightState {
    private ServerSettings settings = ServerSettings.DEFAULT;
    private Map<Identifier, Float> weights = Map.of();
    private Map<Identifier, Float> overrides = Map.of();
    private List<ItemWeightsPayload> packets = List.of();
    private long revision;

    public static ServerWeightState of(MinecraftServer server) {
        return ((ServerStateAccess) server).heavyinventories$getWeightState();
    }

    public static void start(MinecraftServer server) {
        var state = of(server);
        try { state.reload(server); }
        catch (IOException | IllegalArgumentException e) {
            HeavyInventories.LOGGER.error("Invalid Heavy Inventories server files; using defaults for this session", e);
            state.replace(ServerSettings.DEFAULT, defaults());
        }
    }

    public void reload(MinecraftServer server) throws IOException {
        Path gameDir = Services.PLATFORM.getGameDirectory();
        // Read and validate everything before changing the active session.
        var newSettings = ConfigFileManager.readServerConfig(gameDir.resolve("config/heavyinventories-server.json"));
        var newOverrides = loadOverrides(gameDir.resolve("weights"));
        var newWeights = defaults();
        newWeights.putAll(newOverrides);
        replace(newSettings, newWeights, newOverrides);
        // Holders observe the revision on their next tick, including players joining after startup.
    }

    private static Map<Identifier, Float> defaults() {
        var values = new HashMap<Identifier, Float>();
        BuiltInRegistries.ITEM.forEach(item -> values.put(BuiltInRegistries.ITEM.getKey(item), 0.1f));
        return values;
    }

    public static Map<Identifier, Float> loadWeights(Path directory) throws IOException {
        var values = defaults();
        values.putAll(loadOverrides(directory));
        return values;
    }

    public static Map<Identifier, Float> loadOverrides(Path directory) throws IOException {
        var values = defaults();
        var overrides = new HashMap<Identifier, Float>();
        var namespaces = new HashMap<String, JsonObject>();
        for (var id : values.keySet()) {
            if (!namespaces.containsKey(id.getNamespace())) {
                var file = directory.resolve(id.getNamespace() + ".json");
                JsonObject root = com.iso2t.heavyinventories.api.files.WriteFile.readWeights(file);
                namespaces.put(id.getNamespace(), root);
            }
            var root = namespaces.get(id.getNamespace());
            if (root.has(id.getPath())) {
                var entry = root.get(id.getPath());
                if (!entry.isJsonObject()) throw new IllegalArgumentException("Invalid weight entry " + id);
                var value = entry.getAsJsonObject().get("weight");
                if (value != null) {
                    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
                        throw new IllegalArgumentException("Invalid weight value " + id);
                    overrides.put(id, ServerSettings.validateItemWeight(value.getAsFloat()));
                }
            }
        }
        return Map.copyOf(overrides);
    }

    /** Validate the complete candidate before touching disk or the running session. */
    public void setWeight(Identifier id, float value) throws IOException {
        ServerSettings.validateItemWeight(value);
        Path gameDir = Services.PLATFORM.getGameDirectory();
        var nextSettings = ConfigFileManager.readServerConfig(gameDir.resolve("config/heavyinventories-server.json"));
        var nextOverrides = new HashMap<>(loadOverrides(gameDir.resolve("weights")));
        nextOverrides.put(id, value);
        var nextWeights = defaults();
        nextWeights.putAll(nextOverrides);
        Path path = com.iso2t.heavyinventories.api.files.FileValidator.validate(id.getNamespace());
        var root = com.iso2t.heavyinventories.api.files.WriteFile.readWeights(path);
        root = com.iso2t.heavyinventories.api.files.WriteFile.withValue(root, id.getPath(),
                com.iso2t.heavyinventories.api.files.DataType.WEIGHT, value);
        com.iso2t.heavyinventories.api.files.JsonFiles.writeObject(path, root);
        replace(nextSettings, nextWeights, Map.copyOf(nextOverrides));
    }

    /** Also used by runtime tests to supply deterministic session definitions without changing files. */
    public void replace(ServerSettings settings, Map<Identifier, Float> values) {
        replace(settings, values, overrides);
    }

    private void replace(ServerSettings settings, Map<Identifier, Float> values, Map<Identifier, Float> overrides) {
        values.values().forEach(ServerSettings::validateItemWeight);
        var entries = values.entrySet().stream().map(e -> new ItemWeightsPayload.Entry(e.getKey(), e.getValue())).toList();
        int chunks = Math.max(1, (entries.size() + ItemWeightsPayload.CHUNK_SIZE - 1) / ItemWeightsPayload.CHUNK_SIZE);
        var next = new ArrayList<ItemWeightsPayload>(chunks);
        for (int i = 0; i < chunks; i++) {
            int from = i * ItemWeightsPayload.CHUNK_SIZE;
            next.add(new ItemWeightsPayload(revision + 1, i, chunks,
                    entries.subList(from, Math.min(from + ItemWeightsPayload.CHUNK_SIZE, entries.size()))));
        }
        this.settings = settings;
        this.overrides = overrides;
        weights = Map.copyOf(values);
        packets = List.copyOf(next);
        revision++;
    }

    public float weight(ItemStack stack) {
        return com.iso2t.heavyinventories.api.weight.StackWeight.of(stack, this::unitWeight).weight();
    }
    public float unitWeight(Identifier item) { return weights.getOrDefault(item, 0.1f); }
    public Map<Identifier, Float> overrides() { return overrides; }
    public ServerSettings settings() { return settings; }
    public long revision() { return revision; }
    public List<ItemWeightsPayload> packets() { return packets; }
    public Map<Identifier, Float> weights() { return weights; }
}
