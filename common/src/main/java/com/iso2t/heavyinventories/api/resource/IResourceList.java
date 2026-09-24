package com.iso2t.heavyinventories.api.resource;

import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Extracts ingredients and static outputs from server recipes for weight inference.
 * Brewing, stonecutting, campfire cooking, and dynamic recipe outputs are not inferred.
 * Ingredient alternatives retain the existing first-matching-item policy.
 */
public interface IResourceList {

    /**
     * @param ingredients ingredient items and their required counts
     * @param outputCount number of items produced by the recipe
     */
    record RecipeData(HashMap<ItemLike, Integer> ingredients, int outputCount) {}

    Collection<RecipeData> getResources();

    static IResourceList getResourceList(ItemLike itemLike, Level level) {
        Collection<RecipeData> resources = new ArrayList<>();
        resources.addAll(getCraftingTableList(itemLike, level).getResources());
        resources.addAll(getSmeltingList(itemLike, level).getResources());
        resources.addAll(getBlastingList(itemLike, level).getResources());
        resources.addAll(getSmokingList(itemLike, level).getResources());
        resources.addAll(getSmithingList(itemLike, level).getResources());
        return () -> resources;
    }

    static IResourceList getCraftingTableList(ItemLike itemLike, Level level) {
        return getRecipesFor(itemLike, level, RecipeType.CRAFTING);
    }

    static IResourceList getSmeltingList(ItemLike itemLike, Level level) {
        return getRecipesFor(itemLike, level, RecipeType.SMELTING);
    }

    static IResourceList getBlastingList(ItemLike itemLike, Level level) {
        return getRecipesFor(itemLike, level, RecipeType.BLASTING);
    }

    static IResourceList getSmokingList(ItemLike itemLike, Level level) {
        return getRecipesFor(itemLike, level, RecipeType.SMOKING);
    }

    static IResourceList getSmithingList(ItemLike itemLike, Level level) {
        return getRecipesFor(itemLike, level, RecipeType.SMITHING);
    }

    private static IResourceList getRecipesFor(ItemLike itemLike, Level level, RecipeType<?> type) {
        Collection<RecipeData> resources = new ArrayList<>();
        var server = level.getServer();
        if (server == null) {
            return () -> resources;
        }

        var context = SlotDisplayContext.fromLevel(level);
        for (var holder : server.getRecipeManager().getRecipes()) {
            var recipe = holder.value();
            if (recipe.getType() != type) continue;

            var placement = recipe.placementInfo();
            if (placement.isImpossibleToPlace()) continue;

            for (var display : recipe.display()) {
                // Demo results (for example armor trims) are not fixed recipe outputs.
                if (!(display.result() instanceof SlotDisplay.ItemStackSlotDisplay)
                        && !(display.result() instanceof SlotDisplay.ItemSlotDisplay)) continue;
                var output = display.result().resolveForFirstStack(context);
                if (output.isEmpty() || !output.is(itemLike.asItem())) continue;

                HashMap<ItemLike, Integer> ingredients = new HashMap<>();
                for (int index : placement.slotsToIngredientIndex()) {
                    if (index < 0) continue;
                    placement.ingredients().get(index).items().findFirst()
                            .ifPresent(item -> ingredients.merge(item.value(), 1, Integer::sum));
                }
                resources.add(new RecipeData(ingredients, output.getCount()));
                break;
            }
        }
        return () -> resources;
    }
}
