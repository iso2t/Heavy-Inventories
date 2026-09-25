package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.server.weight.WeightReport;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Checks the bundled pack against the actual loaded vanilla recipes and tags. */
public final class BundledDefaultsScenario {
    private BundledDefaultsScenario() {}
    public static void verify(MinecraftServer server) throws java.io.IOException {
        var state = ServerWeightState.of(server);
        Files.writeString(Path.of("bundled-defaults-report.json"), WeightReport.create("minecraft",
                state.revision(), state.weights(), state.provenance()).toString());
        String[] items = {"stone", "oak_log", "iron_ingot", "gold_ingot", "oak_planks", "stick",
                "arrow", "iron_pickaxe", "iron_chestplate", "shield", "bow", "torch", "furnace",
                "crafting_table", "bucket", "bundle", "iron_block", "mace", "diamond_pickaxe", "netherite_pickaxe", "bone_block", "dried_kelp_block",
                "honey_block", "red_bundle", "red_shulker_box", "copper_lantern", "oxidized_copper_lantern"};
        float[] expected = {4, 2, 2, 4, .5f, .25f, .09f, 6.5f, 16, 5, .9f, .1875f, 32, 2, 6, 1.05f, 18, 12, 5, 9, .75f, .45f, 2, 1.05f, 8, 2.0208333f, 2.0208333f};
        for (int i = 0; i < items.length; i++) {
            float actual = state.unitWeight(Identifier.withDefaultNamespace(items[i]));
            if (Math.abs(actual - expected[i]) > .00001f)
                throw new AssertionError("Bundled " + items[i] + ": expected " + expected[i] + ", got " + actual);
        }
        var creativeFallbacks = java.util.Set.of("barrier", "bedrock", "chain_command_block", "command_block",
                "command_block_minecart", "debug_stick", "end_portal_frame", "jigsaw", "knowledge_book", "light",
                "repeating_command_block", "spawner", "structure_block", "structure_void", "test_block",
                "test_instance_block", "trial_spawner", "vault");
        state.provenance().forEach((id, source) -> {
            if (id.getNamespace().equals("minecraft") && source.source() ==
                    com.iso2t.heavyinventories.server.weight.WeightProvenance.Source.FALLBACK
                    && !id.getPath().endsWith("_spawn_egg") && !creativeFallbacks.contains(id.getPath()))
                throw new AssertionError("Unexpected survival-item fallback: " + id);
        });
        for (String item : new String[]{"arrow", "oak_planks", "iron_pickaxe", "bone_block", "dried_kelp_block"}) {
            if (state.provenance().get(Identifier.withDefaultNamespace(item)).source() !=
                    com.iso2t.heavyinventories.server.weight.WeightProvenance.Source.RECIPE)
                throw new AssertionError("Ordinary recipe output became fixed: " + item);
        }
        String[] kit = {"iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots", "iron_pickaxe",
                "iron_sword", "shield", "bow", "arrow", "cooked_beef", "torch", "crafting_table", "furnace"};
        int[] counts = {1, 1, 1, 1, 1, 1, 1, 1, 64, 32, 64, 1, 1};
        float total = 0;
        for (int i = 0; i < kit.length; i++) total += counts[i] * state.unitWeight(Identifier.withDefaultNamespace(kit[i]));
        if (Math.abs(total - 132.41f) > .0001f) throw new AssertionError("Example kit weight: " + total);
        HeavyInventories.LOGGER.info("BUNDLED DEFAULTS PASSED: material anchors and real recipe results");
    }
}
