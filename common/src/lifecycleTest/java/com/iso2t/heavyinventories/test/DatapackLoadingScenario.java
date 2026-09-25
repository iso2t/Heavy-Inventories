package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.server.weight.WeightDefinition;
import com.iso2t.heavyinventories.server.weight.WeightPackAccess;
import com.iso2t.heavyinventories.server.weight.WeightPackData;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Opt-in disposable-world test: fixtures are removed and the original pack selection restored on success. */
public final class DatapackLoadingScenario {
    private static int stage, ticks;
    private static Path dataDirectory, base, override, convertedPack;
    private static List<String> originalSelection;
    private static List<String> testSelection;
    private static CompletableFuture<Void> reload;
    private static WeightPackData.Result initial, first;
    private static ResourceManager firstManager;
    private static long gameplayRevision;
    private static Map<Identifier, Float> gameplayWeights, baselineWeights;
    private static List<com.iso2t.heavyinventories.network.ItemWeightsPayload> gameplayPackets;
    private static Map<Identifier, com.iso2t.heavyinventories.server.weight.WeightProvenance> gameplayProvenance;
    private static ResourceManager beforeFailure;
    private static Runnable afterClient;
    private static int checkpointId;
    private static volatile Checkpoint checkpoint;
    private static volatile int acknowledged;
    private static volatile boolean complete;
    public record Checkpoint(int id, long revision, float arrow, float stone, float total) {}
    public static Checkpoint checkpoint() { return checkpoint; }
    public static void acknowledge(int id) { acknowledged = id; }
    public static boolean complete() { return complete; }
    public static boolean failReload;

    public static void tick(MinecraftServer server) {
        if (complete) return;
        if (!server.isDedicatedServer() && server.getPlayerList().getPlayers().isEmpty()) return;
        require(++ticks < 1200, "Datapack test timed out at stage " + stage);
        if (afterClient != null) {
            if (acknowledged < checkpoint.id()) return;
            var action = afterClient;
            afterClient = null;
            action.run();
            return;
        }
        if (reload != null && !reload.isDone()) return;
        if (reload != null) {
            if (stage == 6) {
                require(reload.isCompletedExceptionally(), "Injected listener failure did not fail the vanilla reload");
            } else reload.join();
            reload = null;
        }
        try {
            switch (stage) {
                case 0 -> {
                    initial = data(server);
                    require(initial.valid(), "Disposable world contains invalid weight definitions");
                    require(initial.warnings().isEmpty(), "Bundled definitions contain unknown item IDs: " + initial.warnings());
                    var state = ServerWeightState.of(server);
                    gameplayRevision = state.revision();
                    gameplayWeights = state.weights();
                    gameplayPackets = state.packets();
                    gameplayProvenance = state.provenance();
                    baselineWeights = state.weights();
                    BundledDefaultsScenario.verify(server);
                    near(state, "minecraft:arrow", 0.09f);
                    if (!server.isDedicatedServer()) {
                        var player = server.getPlayerList().getPlayers().getFirst();
                        player.getInventory().clearContent();
                        player.containerMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
                        player.removeAllEffects();
                        player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW, 64));
                        com.iso2t.heavyinventories.api.events.PlayerEvents.onPlayerTick(player);
                    }
                    originalSelection = List.copyOf(server.getPackRepository().getSelectedIds());
                    dataDirectory = server.getWorldPath(LevelResource.DATAPACK_DIR).toAbsolutePath().normalize();
                    String suffix = UUID.randomUUID().toString();
                    base = dataDirectory.resolve("heavyinventories-test-base-" + suffix);
                    override = dataDirectory.resolve("heavyinventories-test-override-" + suffix);
                    Files.createDirectories(base);
                    Files.createDirectories(override);
                    metadata(base);
                    metadata(override);
                    write(base, "minecraft:arrow", "not valid JSON, deliberately shadowed");
                    write(base, "minecraft:stick", "{\"weight\":9}");
                    write(base, "minecraft:brick", "{\"weight\":2}");
                    write(override, "minecraft:arrow", "{\"weight\":0.053125}");
                    write(override, "minecraft:stick", "{\"infer\":true}");
                    write(override, "minecraft:charcoal", "{\"infer\":true}");
                    write(override, "minecraft:stone", "{\"weight\":0}");
                    write(override, "heavyinventories_absent:metals/test_ingot", "{\"weight\":4}");
                    var legacy = Files.createDirectories(base.resolve("legacy-input"));
                    Files.writeString(legacy.resolve("minecraft.json"), "{\"paper\":{\"weight\":0.375}}");
                    var format = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
                    var converted = com.iso2t.heavyinventories.server.weight.LegacyWeightConverter.convert(
                            legacy, base.resolve("conversion-output"), "converted", format.major(), format.minor());
                    convertedPack = dataDirectory.resolve("heavyinventories-test-converted-" + suffix + ".zip");
                    Files.copy(converted.file(), convertedPack);
                    testSelection = new ArrayList<>(originalSelection);
                    testSelection.add("file/" + base.getFileName());
                    testSelection.add("file/" + override.getFileName());
                    testSelection.add("file/" + convertedPack.getFileName());
                    server.getPackRepository().reload();
                    afterClient(server, () -> reload = server.reloadResources(testSelection));
                    stage = 1;
                }
                case 1 -> {
                    first = data(server);
                    firstManager = server.getResourceManager();
                    require(first.valid(), "Valid fixture pack was rejected: " + first.errors());
                    require(first.definitions().get(id("minecraft:arrow")).definition().equals(new WeightDefinition.Fixed(0.053125f)), "Pack priority/fraction mismatch");
                    require(first.definitions().get(id("minecraft:arrow")).sourcePack().contains(override.getFileName().toString()), "Missing winning source pack");
                    require(first.definitions().get(id("minecraft:stone")).definition().equals(new WeightDefinition.Fixed(0)), "Explicit zero lost");
                    require(first.definitions().get(id("minecraft:stick")).definition() == WeightDefinition.Infer.INSTANCE, "Inference did not replace fixed value");
                    require(first.warnings().stream().anyMatch(p -> p.message().contains("heavyinventories_absent:metals/test_ingot")), "Missing optional item diagnostic");
                    require(!first.definitions().containsKey(id("heavyinventories_absent:metals/test_ingot")), "Unknown item became a gameplay definition");
                    adoptedGameplay(server);
                    near(ServerWeightState.of(server), "minecraft:arrow", 0.053125f);
                    near(ServerWeightState.of(server), "minecraft:stone", 0);
                    near(ServerWeightState.of(server), "minecraft:paper", 0.375f);
                    var paper = ServerWeightState.of(server).provenance().get(id("minecraft:paper"));
                    require(paper.source() == com.iso2t.heavyinventories.server.weight.WeightProvenance.Source.EXPLICIT
                            && paper.definition().sourcePack().contains(convertedPack.getFileName().toString()), "Converted pack provenance lost");
                    near(ServerWeightState.of(server), "minecraft:stick", 0.25f);
                    near(ServerWeightState.of(server), "minecraft:charcoal", 2f);
                    require(!ServerWeightState.of(server).explicitWeights().containsKey(id("minecraft:stick")), "Infer retained a fixed anchor");
                    write(override, "minecraft:arrow", "{\"weight\":-1}");
                    write(override, "minecraft:dirt", "{\"weight\":5}");
                    afterClient(server, () -> reload = server.reloadResources(testSelection));
                    stage = 2;
                }
                case 2 -> {
                    var rejected = data(server);
                    require(!rejected.valid() && rejected.definitions().isEmpty(), "Invalid winner exposed partial data");
                    require(rejected.errors().stream().anyMatch(p -> p.resource().endsWith("weights/arrow.json")
                            && p.sourcePack().contains(override.getFileName().toString())), "Error lost file/pack identity");
                    require(server.getResourceManager() != firstManager, "Reload reused resource manager");
                    require(((WeightPackAccess) firstManager).heavyinventories$getWeightPackData().orElseThrow() == first,
                            "New reload mutated old candidate");
                    unchangedGameplay(server);
                    try {
                        ServerWeightState.of(server).reload(server);
                        throw new AssertionError("Invalid candidate applied");
                    } catch (IllegalArgumentException expected) { }
                    unchangedGameplay(server);
                    write(override, "minecraft:arrow", "{\"infer\":true}");
                    write(override, "minecraft:flint", "{\"weight\":0.7}");
                    Files.delete(resource(override, "minecraft:stick"));
                    Files.delete(resource(override, "minecraft:charcoal"));
                    afterClient(server, () -> reload = server.reloadResources(testSelection));
                    stage = 3;
                }
                case 3 -> {
                    var repaired = data(server);
                    require(repaired.valid(), "Valid reload did not recover");
                    require(repaired.definitions().get(id("minecraft:arrow")).definition() == WeightDefinition.Infer.INSTANCE, "Repaired value not loaded");
                    require(repaired.definitions().get(id("minecraft:stick")).definition().equals(new WeightDefinition.Fixed(9)), "Removing override did not reveal base");
                    adoptedGameplay(server);
                    near(ServerWeightState.of(server), "minecraft:arrow", 2.4275f);
                    near(ServerWeightState.of(server), "minecraft:charcoal", 0.5f);
                    writeRecipe(8);
                    writeTag("minecraft:flint");
                    afterClient(server, () -> reload = server.reloadResources(testSelection));
                    stage = 4;
                }
                case 4 -> {
                    adoptedGameplay(server);
                    near(ServerWeightState.of(server), "minecraft:arrow", 1.21375f);
                    writeTag("minecraft:coal");
                    afterClient(server, () -> reload = server.reloadResources(testSelection));
                    stage = 5;
                }
                case 5 -> {
                    adoptedGameplay(server);
                    near(ServerWeightState.of(server), "minecraft:arrow", 1.18875f);
                    beforeFailure = server.getResourceManager();
                    // Valid weights must still stay unapplied when another part of the vanilla reload fails.
                    write(override, "minecraft:arrow", "{\"weight\":999}");
                    failReload = true;
                    afterClient(server, () -> reload = server.reloadResources(testSelection));
                    stage = 6;
                }
                case 6 -> {
                    require(server.getResourceManager() == beforeFailure, "Vanilla failure replaced resources");
                    unchangedGameplay(server);
                    failReload = false;
                    afterClient(server, () -> reload = server.reloadResources(originalSelection));
                    stage = 7;
                }
                case 7 -> {
                    require(data(server).definitions().equals(initial.definitions()), "Disabled fixtures left stale definitions");
                    adoptedGameplay(server);
                    require(ServerWeightState.of(server).weights().equals(baselineWeights), "Removed packs left stale gameplay weights");
                    cleanupPack(base);
                    cleanupPack(override);
                    Files.delete(convertedPack);
                    server.getPackRepository().reload();
                    afterClient(server, () -> {
                        complete = true;
                        HeavyInventories.LOGGER.info("DATAPACK LOADING PASSED: registered startup/reload listener, pack priority, fractions, zero, infer, optional items, invalid candidate isolation, repair/removal");
                        HeavyInventories.LOGGER.info("DATAPACK GAMEPLAY PASSED: startup recipes, explicit zero, infer/removal, ingredient propagation, rejected candidate retention, restored complete table");
                        HeavyInventories.LOGGER.info("DATAPACK RELOAD PASSED: automatic adoption exactly once, recipe output and tag edits, failed vanilla reload retention, repair and disable");
                        HeavyInventories.LOGGER.info("DATAPACK CONVERSION PASSED: generated ZIP discovered, metadata accepted, explicit weight applied, pack provenance and failure retention, removal");
                        if (server.isDedicatedServer()) server.halt(false);
                    });
                    stage = 8;
                }
            }
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    private static WeightPackData.Result data(MinecraftServer server) {
        return ((WeightPackAccess) server.getResourceManager()).heavyinventories$getWeightPackData()
                .orElseThrow(() -> new AssertionError("Weight datapack listener did not run"));
    }
    private static void unchangedGameplay(MinecraftServer server) {
        var state = ServerWeightState.of(server);
        require(state.revision() == gameplayRevision && state.weights().equals(gameplayWeights) && state.packets() == gameplayPackets && state.provenance() == gameplayProvenance, "Failed reload changed gameplay");
    }
    private static void adoptedGameplay(MinecraftServer server) {
        var state = ServerWeightState.of(server);
        require(state.revision() == gameplayRevision + 1, "Successful application did not advance revision");
        require(state.weights().size() == net.minecraft.core.registries.BuiltInRegistries.ITEM.size(), "Incomplete gameplay table");
        gameplayRevision = state.revision();
        gameplayWeights = state.weights();
        gameplayPackets = state.packets();
        gameplayProvenance = state.provenance();
    }
    private static void afterClient(MinecraftServer server, Runnable action) {
        if (server.isDedicatedServer()) { action.run(); return; }
        var state = ServerWeightState.of(server);
        var player = server.getPlayerList().getPlayers().getFirst();
        float total = state.unitWeight(id("minecraft:arrow")) * 64;
        require(Math.abs(com.iso2t.heavyinventories.api.player.PlayerHolder.getOrCreate(player).getWeight() - total) < 0.0001f,
                "Reload did not immediately refresh the unchanged inventory");
        afterClient = action;
        checkpoint = new Checkpoint(++checkpointId, state.revision(), state.unitWeight(id("minecraft:arrow")),
                state.unitWeight(id("minecraft:stone")), total);
    }
    private static void writeRecipe(int count) throws IOException {
        var path = override.resolve("data/minecraft/recipe/arrow.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, """
                {"type":"minecraft:crafting_shapeless","ingredients":["#heavyinventories_test:arrow_material","minecraft:stick","minecraft:feather"],
                 "result":{"id":"minecraft:arrow","count":%d}}
                """.formatted(count));
    }
    private static void writeTag(String item) throws IOException {
        var path = override.resolve("data/heavyinventories_test/tags/item/arrow_material.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, "{\"replace\":true,\"values\":[\"" + item + "\"]}");
    }
    private static void near(ServerWeightState state, String item, float expected) {
        require(Math.abs(state.unitWeight(id(item)) - expected) < 0.000001f, item + " expected " + expected + ", got " + state.unitWeight(id(item)));
    }
    private static Identifier id(String value) { return Identifier.parse(value); }
    private static Path resource(Path pack, String item) {
        var key = id(item);
        return pack.resolve("data/" + key.getNamespace() + "/heavyinventories/weights/" + key.getPath() + ".json");
    }
    private static void write(Path pack, String item, String json) throws IOException {
        var path = resource(pack, item);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json);
    }
    private static void metadata(Path pack) throws IOException {
        var format = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
        var version = "[" + format.major() + "," + format.minor() + "]";
        Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Heavy Inventories disposable test\",\"min_format\":"
                + version + ",\"max_format\":" + version + "}}");
    }
    private static void cleanupPack(Path root) throws IOException {
        // Only remove the unique fixture directory created by this scenario, inside this test world's datapacks.
        root = root.toAbsolutePath().normalize();
        require(root.getParent().equals(dataDirectory) && root.getFileName().toString().startsWith("heavyinventories-test-"), "Unsafe fixture cleanup path");
        try (var paths = Files.walk(root)) {
            for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
