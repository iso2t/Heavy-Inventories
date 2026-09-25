package com.iso2t.heavyinventories.api.resource;

import com.iso2t.heavyinventories.api.weight.RecipeWeights;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;

/** Snapshot only recipes with static outputs and no crafting remainders; custom/dynamic recipes are excluded. */
public final class IResourceList {
    private IResourceList() {}

    public static List<RecipeWeights.Recipe> snapshot(Level level) {
        var server = level.getServer();
        if (server == null) throw new IllegalArgumentException("Recipe inference requires a server");
        var result = new ArrayList<RecipeWeights.Recipe>();
        var context = SlotDisplayContext.fromLevel(level);
        int alternatives = 0;
        for (var holder : server.getRecipeManager().getRecipes()) {
            var recipe = holder.value();
            // Exact vanilla classes: subclasses may introduce custom ingredients, outputs, or remainders.
            var type = recipe.getClass();
            if (type != ShapedRecipe.class && type != ShapelessRecipe.class
                    && type != SmeltingRecipe.class && type != BlastingRecipe.class
                    && type != SmokingRecipe.class && type != CampfireCookingRecipe.class
                    && type != StonecutterRecipe.class) continue;
            var placement = recipe.placementInfo();
            if (placement.isImpossibleToPlace()) continue;
            var slots = new ArrayList<List<Identifier>>();
            boolean supported = true;
            for (int index : placement.slotsToIngredientIndex()) {
                if (index < 0) continue;
                var choices = placement.ingredients().get(index).items().toList();
                alternatives += choices.size();
                if (alternatives > RecipeWeights.MAX_ALTERNATIVES) throw new IllegalArgumentException("Recipe snapshot exceeds ingredient limit");
                if (choices.isEmpty() || choices.stream().anyMatch(item -> item.value().getCraftingRemainder() != null)) {
                    supported = false;
                    break;
                }
                slots.add(choices.stream().map(item -> BuiltInRegistries.ITEM.getKey(item.value())).toList());
            }
            if (!supported || slots.isEmpty()) continue;
            for (var display : recipe.display()) {
                if (!(display.result() instanceof SlotDisplay.ItemStackSlotDisplay)
                        && !(display.result() instanceof SlotDisplay.ItemSlotDisplay)) continue;
                var output = display.result().resolveForFirstStack(context);
                // A per-item definition cannot represent component-dependent results.
                if (output.isEmpty() || !output.getComponentsPatch().isEmpty()) continue;
                result.add(new RecipeWeights.Recipe(BuiltInRegistries.ITEM.getKey(output.getItem()), output.getCount(), slots));
                if (result.size() > RecipeWeights.MAX_RECIPES) throw new IllegalArgumentException("Recipe snapshot exceeds recipe limit");
                break;
            }
        }
        return List.copyOf(result);
    }
}
