package com.iso2t.heavyinventories.server.weight;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class WeightPackDataTest {
    @TempDir Path directory;
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    private static Identifier id(String value) { return Identifier.parse(value); }

    private void definition(String pack, String item, String json) throws Exception {
        var key = id(item);
        var file = directory.resolve(pack + "/data/" + key.getNamespace() + "/heavyinventories/weights/" + key.getPath() + ".json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
    }

    private MultiPackResourceManager manager(String... packs) {
        return new MultiPackResourceManager(PackType.SERVER_DATA, java.util.Arrays.stream(packs)
                .map(name -> (net.minecraft.server.packs.PackResources) new PathPackResources(
                        new PackLocationInfo(name, Component.literal(name), PackSource.DEFAULT, Optional.empty()), directory.resolve(name)))
                .toList());
    }

    @Test void packPriorityUsesOnlyWinningResourceAndPreservesSource() throws Exception {
        definition("base", "minecraft:arrow", "invalid shadowed JSON");
        definition("override", "minecraft:arrow", "{\"weight\":0.053125}");
        try (var manager = manager("base", "override")) {
            var result = WeightPackData.load(manager, item -> true);
            assertTrue(result.valid(), result.errors().toString());
            var arrow = result.definitions().get(id("minecraft:arrow"));
            assertEquals(new WeightDefinition.Fixed(0.053125f), arrow.definition());
            assertEquals("override", arrow.sourcePack());
            assertEquals(id("minecraft:heavyinventories/weights/arrow.json"), arrow.resource());
        }
    }

    @Test void inferenceReplacementAndNestedPathsFollowPackRemoval() throws Exception {
        definition("base", "examplemod:metals/steel_ingot", "{\"weight\":2}");
        definition("override", "examplemod:metals/steel_ingot", "{\"infer\":true}");
        definition("override", "minecraft:stone", "{\"weight\":0}");
        WeightPackData.Result first;
        try (var manager = manager("base", "override")) {
            first = WeightPackData.load(manager, item -> true);
            assertEquals(WeightDefinition.Infer.INSTANCE, first.definitions().get(id("examplemod:metals/steel_ingot")).definition());
            assertEquals(new WeightDefinition.Fixed(0), first.definitions().get(id("minecraft:stone")).definition());
        }
        try (var manager = manager("base")) {
            var next = WeightPackData.load(manager, item -> true);
            assertEquals(new WeightDefinition.Fixed(2), next.definitions().get(id("examplemod:metals/steel_ingot")).definition());
            assertFalse(next.definitions().containsKey(id("minecraft:stone")));
            assertEquals(WeightDefinition.Infer.INSTANCE, first.definitions().get(id("examplemod:metals/steel_ingot")).definition());
            assertThrows(UnsupportedOperationException.class, () -> next.definitions().clear());
        }
    }

    @Test void malformedWinnerRejectsWholeCandidateWithResourceAndPack() throws Exception {
        definition("base", "minecraft:arrow", "{\"weight\":1}");
        definition("override", "minecraft:arrow", "{\"weight\":-1}");
        definition("override", "minecraft:stone", "{\"weight\":3}");
        try (var manager = manager("base", "override")) {
            var result = WeightPackData.load(manager, item -> true);
            assertFalse(result.valid());
            assertTrue(result.definitions().isEmpty(), "No valid subset may escape an invalid candidate");
            assertEquals(1, result.errors().size());
            assertEquals("override", result.errors().getFirst().sourcePack());
            assertTrue(result.errors().getFirst().resource().endsWith("weights/arrow.json"));
        }
    }

    @Test void optionalItemsWarnButMalformedOptionalDefinitionsStillFail() throws Exception {
        definition("pack", "absent:metals/steel_ingot", "{\"weight\":2}");
        try (var manager = manager("pack")) {
            var result = WeightPackData.load(manager, item -> false);
            assertTrue(result.valid());
            assertTrue(result.definitions().isEmpty());
            assertTrue(result.warnings().getFirst().message().contains("absent:metals/steel_ingot"));
            assertEquals("pack", result.warnings().getFirst().sourcePack());
        }
        definition("pack", "absent:metals/steel_ingot", "{\"infer\":false}");
        try (var manager = manager("pack")) {
            var result = WeightPackData.load(manager, item -> false);
            assertFalse(result.valid());
        }
    }

    @Test void emptyResourceSetAndItemPathValidation() {
        var empty = WeightPackData.load(net.minecraft.server.packs.resources.ResourceManager.Empty.INSTANCE, item -> true);
        assertTrue(empty.valid());
        assertTrue(empty.definitions().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> WeightPackData.itemId(id("test:heavyinventories/weights/.json")));
        assertThrows(IllegalArgumentException.class, () -> WeightPackData.itemId(id("test:other/arrow.json")));
    }
}
