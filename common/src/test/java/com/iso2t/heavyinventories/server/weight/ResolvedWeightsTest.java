package com.iso2t.heavyinventories.server.weight;

import com.iso2t.heavyinventories.api.weight.RecipeWeights;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ResolvedWeightsTest {
    private static Identifier id(String name) { return Identifier.parse("test:" + name); }
    private static RecipeWeights.Recipe recipe(String output, int count, String... inputs) {
        return new RecipeWeights.Recipe(id(output), count, Arrays.stream(inputs).map(name -> List.of(id(name))).toList());
    }
    private static WeightPackData.Result pack(Map<String, WeightDefinition> entries) {
        var definitions = new HashMap<Identifier, WeightPackData.Entry>();
        entries.forEach((name, value) -> definitions.put(id(name), new WeightPackData.Entry(value,
                id("heavyinventories/weights/" + name + ".json"), "test-pack")));
        return new WeightPackData.Result(definitions, List.of(), List.of());
    }
    private static Set<Identifier> items(String... names) { return new HashSet<>(Arrays.stream(names).map(ResolvedWeightsTest::id).toList()); }

    @Test void materialWeightsPropagateWithoutTurningDerivedValuesIntoAnchors() {
        var recipes = List.of(recipe("planks", 4, "log"), recipe("stick", 4, "planks", "planks"));
        var registry = items("log", "planks", "stick", "unobtainable");
        var first = ResolvedWeights.resolve(pack(Map.of("log", new WeightDefinition.Fixed(8))), recipes, registry);
        assertEquals(2f, first.weights().get(id("planks")));
        assertEquals(1f, first.weights().get(id("stick")));
        assertEquals(0.1f, first.weights().get(id("unobtainable")));
        assertEquals(Map.of(id("log"), 8f), first.explicitWeights());
        var changed = ResolvedWeights.resolve(pack(Map.of("log", new WeightDefinition.Fixed(16))), recipes, registry);
        assertEquals(2f, changed.weights().get(id("stick")));
        assertEquals(1f, first.weights().get(id("stick")));
        assertThrows(UnsupportedOperationException.class, () -> changed.weights().clear());
    }

    @Test void explicitZeroAndExplicitProductOverrideRecipes() {
        var data = pack(Map.of("log", new WeightDefinition.Fixed(8), "planks", new WeightDefinition.Fixed(0),
                "stick", new WeightDefinition.Fixed(0.053125f)));
        var result = ResolvedWeights.resolve(data, List.of(recipe("planks", 4, "log"), recipe("stick", 4, "planks", "planks")),
                items("log", "planks", "stick"));
        assertEquals(0f, result.weights().get(id("planks")));
        assertEquals(0.053125f, result.weights().get(id("stick")));
        assertEquals(3, result.explicitWeights().size());
    }

    @Test void inferMarkerAndRemovedDefinitionRestoreDerivedWeights() {
        var recipes = List.of(recipe("planks", 4, "log"));
        var registry = items("log", "planks");
        var inferred = ResolvedWeights.resolve(pack(Map.of("log", new WeightDefinition.Fixed(8), "planks", WeightDefinition.Infer.INSTANCE)), recipes, registry);
        var removed = ResolvedWeights.resolve(pack(Map.of("log", new WeightDefinition.Fixed(8))), recipes, registry);
        assertEquals(2f, inferred.weights().get(id("planks")));
        assertEquals(removed.weights(), inferred.weights());
        assertEquals(removed.explicitWeights(), inferred.explicitWeights());
        assertEquals(WeightDefinition.Infer.INSTANCE, inferred.provenance().get(id("planks")).definition().definition());
        assertNull(removed.provenance().get(id("planks")).definition());
        assertFalse(inferred.explicitWeights().containsKey(id("planks")));
    }

    @Test void allRegisteredItemsGetWeightsAndFallbackCanFeedRecipes() {
        var result = ResolvedWeights.resolve(pack(Map.of()), List.of(recipe("arrow", 4, "flint", "feather", "stick")),
                items("arrow", "flint", "feather", "stick", "unknown"));
        assertEquals(5, result.weights().size());
        assertEquals(0.075f, result.weights().get(id("arrow")));
        assertEquals(0.1f, result.weights().get(id("unknown")));
        assertTrue(result.explicitWeights().isEmpty());
    }

    @Test void invalidDatapackCannotBecomeFallbackSuccess() {
        var data = new WeightPackData.Result(Map.of(), List.of(new WeightPackData.Problem(
                "test:heavyinventories/weights/log.json", "broken-pack", "Invalid weight")), List.of());
        var error = assertThrows(IllegalArgumentException.class,
                () -> ResolvedWeights.resolve(data, List.of(), items("log")));
        assertTrue(error.getMessage().contains("broken-pack"));
        assertTrue(error.getMessage().contains("weights/log.json"));
    }

    @Test void provenanceDistinguishesEqualValuesAndRecordsWinningDefinition() {
        var data = pack(Map.of("fixed", new WeightDefinition.Fixed(0.1f), "derived", WeightDefinition.Infer.INSTANCE));
        var result = ResolvedWeights.resolve(data, List.of(recipe("derived", 1, "raw"), recipe("cycle", 1, "cycle")),
                items("fixed", "derived", "raw", "cycle"));
        assertEquals(WeightProvenance.Source.EXPLICIT, result.provenance().get(id("fixed")).source());
        assertEquals(WeightProvenance.Source.RECIPE, result.provenance().get(id("derived")).source());
        assertEquals(WeightProvenance.Source.FALLBACK, result.provenance().get(id("raw")).source());
        assertEquals(WeightProvenance.Source.FALLBACK, result.provenance().get(id("cycle")).source());
        assertTrue(result.weights().values().stream().allMatch(value -> value == 0.1f));
        var report = WeightReport.create("test", 7, result.weights(), result.provenance());
        assertEquals(7, report.get("revision").getAsInt());
        var entries = report.getAsJsonObject("items");
        assertEquals("recipe", entries.getAsJsonObject("test:derived").get("source").getAsString());
        assertEquals("infer", entries.getAsJsonObject("test:derived").getAsJsonObject("definition").get("mode").getAsString());
        assertEquals("test-pack", entries.getAsJsonObject("test:fixed").getAsJsonObject("definition").get("pack").getAsString());
        assertEquals("fallback", entries.getAsJsonObject("test:cycle").get("source").getAsString());
        assertThrows(UnsupportedOperationException.class, () -> result.provenance().clear());
    }

    @Test void compressionCyclesRequireAnchorsAndRespectOutputCounts() {
        var recipes = List.of(recipe("ingot", 9, "block"), recipe("block", 1,
                "ingot", "ingot", "ingot", "ingot", "ingot", "ingot", "ingot", "ingot", "ingot"));
        var registry = items("ingot", "block");
        assertEquals(0.1f, ResolvedWeights.resolve(pack(Map.of()), recipes, registry).weights().get(id("block")));
        assertEquals(18f, ResolvedWeights.resolve(pack(Map.of("ingot", new WeightDefinition.Fixed(2))), recipes, registry).weights().get(id("block")));
    }
}
