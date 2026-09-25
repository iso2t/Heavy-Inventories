package com.iso2t.heavyinventories.server;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.resource.IResourceList;
import com.iso2t.heavyinventories.api.weight.RecipeWeights;
import com.iso2t.heavyinventories.server.weight.ResolvedWeights;
import com.iso2t.heavyinventories.server.weight.WeightPackAccess;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Gameplay definitions/configuration live as long as this logical server. Access on its thread. */
public final class ServerWeightState {
    private ServerSettings settings = ServerSettings.DEFAULT;
    private Map<Identifier, Float> weights = Map.of();
    private Map<Identifier, Float> explicitWeights = Map.of();
    private List<ItemWeightsPayload> packets = List.of();
    private long revision;

    public static ServerWeightState of(MinecraftServer server) {
        return ((ServerStateAccess) server).heavyinventories$getWeightState();
    }

    /** Called after recipes, tags and the overworld are ready, before players join. */
    public static void start(MinecraftServer server) {
        var state = of(server);
        var settings = ServerSettings.DEFAULT;
        try { settings = readSettings(); }
        catch (IOException | IllegalArgumentException e) {
            HeavyInventories.LOGGER.error("Invalid Heavy Inventories settings; using default settings for this session", e);
        }
        ResolvedWeights resolved;
        try { resolved = resolveLoaded(server); }
        catch (IllegalArgumentException | IllegalStateException e) {
            HeavyInventories.LOGGER.error("Invalid Heavy Inventories weight data; using fallback weights for this session", e);
            resolved = new ResolvedWeights(defaults(), Map.of());
        }
        state.replace(settings, resolved.weights(), resolved.explicitWeights());
        warnLegacyFiles();
        HeavyInventories.LOGGER.info("Initialized {} item weights ({} explicit datapack anchors)",
                state.weights.size(), state.explicitWeights.size());
    }

    /** Rebuild from currently loaded resources; reading edited pack files requires Minecraft's resource reload. */
    public void reload(MinecraftServer server) throws IOException {
        var newSettings = readSettings();
        var resolved = resolveLoaded(server);
        // Prepare everything before publishing settings, definitions, packets and revision together.
        replace(newSettings, resolved.weights(), resolved.explicitWeights());
    }

    private static ServerSettings readSettings() throws IOException {
        return ConfigFileManager.readServerConfig(Services.PLATFORM.getGameDirectory().resolve("config/heavyinventories-server.json"));
    }

    private static ResolvedWeights resolveLoaded(MinecraftServer server) {
        var data = ((WeightPackAccess) server.getResourceManager()).heavyinventories$getWeightPackData()
                .orElseThrow(() -> new IllegalStateException("Weight datapack listener did not supply a candidate"));
        if (server.overworld() == null) throw new IllegalStateException("World must be ready before resolving recipe weights");
        return ResolvedWeights.resolve(data, IResourceList.snapshot(server.overworld()), BuiltInRegistries.ITEM.keySet());
    }

    private static Map<Identifier, Float> defaults() {
        var values = new HashMap<Identifier, Float>();
        BuiltInRegistries.ITEM.forEach(item -> values.put(BuiltInRegistries.ITEM.getKey(item), RecipeWeights.FALLBACK));
        return values;
    }

    private static void warnLegacyFiles() {
        var directory = Services.PLATFORM.getGameDirectory().resolve("weights");
        if (!Files.isDirectory(directory)) return;
        try (var files = Files.list(directory)) {
            long count = files.filter(path -> path.getFileName().toString().endsWith(".json")).count();
            if (count > 0) HeavyInventories.LOGGER.warn(
                    "Ignoring {} legacy weight JSON files in {}. Convert entries to data/<namespace>/heavyinventories/weights/<item>.json in a world datapack; original files were preserved.",
                    count, directory);
        } catch (IOException e) {
            HeavyInventories.LOGGER.warn("Could not inspect legacy weight directory {}; it is not used for gameplay", directory, e);
        }
    }

    /** Also used by runtime tests to supply deterministic session definitions without changing files. */
    public void replace(ServerSettings settings, Map<Identifier, Float> values) {
        replace(settings, values, explicitWeights);
    }

    private void replace(ServerSettings settings, Map<Identifier, Float> values, Map<Identifier, Float> explicitWeights) {
        values.values().forEach(ServerSettings::validateItemWeight);
        var entries = values.entrySet().stream().map(e -> new ItemWeightsPayload.Entry(e.getKey(), e.getValue())).toList();
        int chunks = Math.max(1, (entries.size() + ItemWeightsPayload.CHUNK_SIZE - 1) / ItemWeightsPayload.CHUNK_SIZE);
        var next = new ArrayList<ItemWeightsPayload>(chunks);
        for (int i = 0; i < chunks; i++) {
            int from = i * ItemWeightsPayload.CHUNK_SIZE;
            next.add(new ItemWeightsPayload(revision + 1, i, chunks,
                    entries.subList(from, Math.min(from + ItemWeightsPayload.CHUNK_SIZE, entries.size()))));
        }
        var nextExplicit = Map.copyOf(explicitWeights);
        var nextWeights = Map.copyOf(values);
        var nextPackets = List.copyOf(next);
        this.settings = settings;
        this.explicitWeights = nextExplicit;
        weights = nextWeights;
        packets = nextPackets;
        revision++;
    }

    public float weight(ItemStack stack) {
        return com.iso2t.heavyinventories.api.weight.StackWeight.of(stack, this::unitWeight).weight();
    }
    public float unitWeight(Identifier item) { return weights.getOrDefault(item, RecipeWeights.FALLBACK); }
    public Map<Identifier, Float> explicitWeights() { return explicitWeights; }
    public ServerSettings settings() { return settings; }
    public long revision() { return revision; }
    public List<ItemWeightsPayload> packets() { return packets; }
    public Map<Identifier, Float> weights() { return weights; }
}
