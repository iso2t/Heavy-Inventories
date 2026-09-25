package com.iso2t.heavyinventories.server;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.resource.IResourceList;
import com.iso2t.heavyinventories.api.weight.RecipeWeights;
import com.iso2t.heavyinventories.server.weight.ResolvedWeights;
import com.iso2t.heavyinventories.server.weight.WeightProvenance;
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
    private Map<Identifier, WeightProvenance> provenance = Map.of();
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
            var values = defaults();
            var sources = new HashMap<Identifier, WeightProvenance>();
            values.keySet().forEach(id -> sources.put(id, WeightProvenance.FALLBACK));
            resolved = new ResolvedWeights(values, Map.of(), sources);
        }
        state.replace(settings, resolved.weights(), resolved.explicitWeights(), resolved.provenance());
        warnLegacyFiles();
        HeavyInventories.LOGGER.info("Initialized {} item weights ({} explicit datapack anchors)",
                state.weights.size(), state.explicitWeights.size());
    }

    /** Rebuild from currently loaded resources; reading edited pack files requires Minecraft's resource reload. */
    public void reload(MinecraftServer server) throws IOException {
        var newSettings = readSettings();
        var resolved = resolveLoaded(server);
        // Prepare everything before publishing settings, definitions, packets and revision together.
        replace(newSettings, resolved.weights(), resolved.explicitWeights(), resolved.provenance());
    }

    /** Vanilla reload changes datapacks, not the separately managed server settings file. */
    public void reloadDatapacks(MinecraftServer server) {
        ResolvedWeights resolved;
        try { resolved = resolveLoaded(server); }
        catch (IllegalArgumentException | IllegalStateException e) {
            HeavyInventories.LOGGER.error("Weight reload rejected; keeping revision {}: {}", revision, e.getMessage());
            var message = net.minecraft.network.chat.Component.translatable("config.heavyinventories.weights_reload_failed", e.getMessage());
            server.getPlayerList().getPlayers().stream()
                    .filter(ServerConfiguration::canEdit).forEach(player -> player.sendSystemMessage(message));
            return;
        }
        replace(settings, resolved.weights(), resolved.explicitWeights(), resolved.provenance());
        // A changed revision invalidates each holder's cached inventory before it sends definitions and totals.
        server.getPlayerList().getPlayers().forEach(com.iso2t.heavyinventories.api.events.PlayerEvents::onPlayerTick);
        HeavyInventories.LOGGER.info("Applied {} item weights after datapack reload (revision {})", weights.size(), revision);
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
                    "Ignoring {} legacy weight JSON files in {}. Use /heavyinventories convert legacy <pack_name> to export a reviewable datapack; original files were preserved.",
                    count, directory);
        } catch (IOException e) {
            HeavyInventories.LOGGER.warn("Could not inspect legacy weight directory {}; it is not used for gameplay", directory, e);
        }
    }

    /** Also used by runtime tests to supply deterministic session definitions without changing files. */
    public void replace(ServerSettings settings, Map<Identifier, Float> values) {
        var sources = new HashMap<Identifier, WeightProvenance>();
        values.forEach((id, value) -> sources.put(id, value.equals(weights.get(id))
                ? provenance.getOrDefault(id, WeightProvenance.SESSION) : WeightProvenance.SESSION));
        var anchors = new HashMap<Identifier, Float>();
        explicitWeights.forEach((id, value) -> { if (value.equals(values.get(id))) anchors.put(id, value); });
        replace(settings, values, anchors, sources);
    }

    private void replace(ServerSettings settings, Map<Identifier, Float> values, Map<Identifier, Float> explicitWeights, Map<Identifier, WeightProvenance> sources) {
        values.values().forEach(ServerSettings::validateItemWeight);
        var entries = values.entrySet().stream().map(e -> new ItemWeightsPayload.Entry(e.getKey(), e.getValue())).toList();
        int chunks = Math.max(1, (entries.size() + ItemWeightsPayload.CHUNK_SIZE - 1) / ItemWeightsPayload.CHUNK_SIZE);
        var next = new ArrayList<ItemWeightsPayload>(chunks);
        for (int i = 0; i < chunks; i++) {
            int from = i * ItemWeightsPayload.CHUNK_SIZE;
            next.add(new ItemWeightsPayload(revision + 1, i, chunks,
                    entries.subList(from, Math.min(from + ItemWeightsPayload.CHUNK_SIZE, entries.size()))));
        }
        if (!sources.keySet().equals(values.keySet())) throw new IllegalArgumentException("Every weight needs provenance");
        var nextSources = Map.copyOf(sources);
        var nextExplicit = Map.copyOf(explicitWeights);
        var nextWeights = Map.copyOf(values);
        var nextPackets = List.copyOf(next);
        this.settings = settings;
        this.explicitWeights = nextExplicit;
        weights = nextWeights;
        provenance = nextSources;
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
    public Map<Identifier, WeightProvenance> provenance() { return provenance; }
}
