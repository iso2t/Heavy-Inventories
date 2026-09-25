package com.iso2t.heavyinventories.api.weight;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RecipeWeightsTest {
    private static Identifier id(String path) { return Identifier.parse("test:" + path); }
    private static RecipeWeights.Recipe recipe(String output, int count, String... inputs) {
        return new RecipeWeights.Recipe(id(output), count, Arrays.stream(inputs).map(item -> List.of(id(item))).toList());
    }

    @Test void batchCountsMultiplicityAndFractionsArePreserved() {
        var values = RecipeWeights.resolve(List.of(recipe("planks", 4, "log"),
                recipe("stick", 4, "planks", "planks")), Map.of(id("log"), 1f));
        assertEquals(0.25f, values.get(id("planks")));
        assertEquals(0.125f, values.get(id("stick")));
    }

    @Test void alternativesAndRecipesChooseLowestValidMass() {
        var alternatives = new RecipeWeights.Recipe(id("tool"), 1, List.of(List.of(id("iron"), id("wood"))));
        var values = RecipeWeights.resolve(List.of(alternatives, recipe("tool", 1, "gold")),
                Map.of(id("iron"), 4f, id("wood"), 2f, id("gold"), 3f));
        assertEquals(2f, values.get(id("tool")));
    }

    @Test void unanchoredConversionCyclesDoNotBootstrapCheapWeights() {
        var recipes = List.of(recipe("ingot", 9, "block"),
                recipe("block", 1, "ingot", "ingot", "ingot", "ingot", "ingot", "ingot", "ingot", "ingot", "ingot"),
                recipe("self", 8, "self"));
        var unknown = RecipeWeights.resolve(recipes, Map.of());
        assertEquals(0.1f, unknown.get(id("ingot")));
        assertEquals(0.1f, unknown.get(id("block")));
        assertEquals(0.1f, unknown.get(id("self")));
        var anchored = RecipeWeights.resolve(recipes, Map.of(id("ingot"), 2f));
        assertEquals(2f, anchored.get(id("ingot")));
        assertEquals(18f, anchored.get(id("block")));
    }

    @Test void cyclesDoNotDiscardIndependentRecipesOrAlternatives() {
        var alternatives = new RecipeWeights.Recipe(id("a"), 1, List.of(List.of(id("b"), id("base"))));
        var values = RecipeWeights.resolve(List.of(alternatives, recipe("b", 1, "a")), Map.of(id("base"), 3f));
        assertEquals(3f, values.get(id("a")));
        // Intra-cycle inference is deliberately excluded, even if another member has an outside recipe.
        assertEquals(0.1f, values.get(id("b")));
    }

    @Test void explicitZeroWinsAndFallbackIsNotAnInferredMinimum() {
        var values = RecipeWeights.resolve(List.of(recipe("a", 1, "b"), recipe("c", 64, "d")),
                Map.of(id("a"), 0f, id("b"), 9f));
        assertEquals(0f, values.get(id("a")));
        assertEquals(0.1f / 64, values.get(id("c")));
    }

    @Test void traversalOrderAndEarlierRunsCannotAffectResults() {
        var recipes = new ArrayList<>(List.of(recipe("a", 2, "base"), recipe("b", 1, "a"), recipe("a", 1, "b")));
        var first = RecipeWeights.resolve(recipes, Map.of(id("base"), 8f));
        Collections.reverse(recipes);
        assertEquals(first, RecipeWeights.resolve(recipes, Map.of(id("base"), 8f)));
        assertEquals(2f, RecipeWeights.resolve(recipes, Map.of(id("base"), 4f)).get(id("a")));
    }

    @Test void invalidRecipesAndWeightsAreRejectedAndLongChainsDoNotUseTheJavaStack() {
        assertThrows(IllegalArgumentException.class, () -> recipe("bad", 0, "input"));
        assertThrows(IllegalArgumentException.class, () -> RecipeWeights.resolve(List.of(), Map.of(id("a"), Float.NaN)));
        var chain = new ArrayList<RecipeWeights.Recipe>();
        for (int i = 1; i < 5000; i++) chain.add(recipe("item" + i, 1, "item" + (i - 1)));
        assertEquals(2f, RecipeWeights.resolve(chain, Map.of(id("item0"), 2f)).get(id("item4999")));
    }
}
