package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.api.events.PlayerEvents;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.api.resource.IResourceList;
import com.iso2t.heavyinventories.api.weight.RecipeWeights;
import com.iso2t.heavyinventories.server.ServerWeightState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.*;

/** Real inventory/menu/component and loaded-recipe checks shared by both runtime environments. */
public final class WeightCalculationScenario {
    public static void run(ServerPlayer player) {
        var state = ServerWeightState.of(player.level().getServer());
        var values = new HashMap<>(state.weights());
        for (var entry : Map.of(Items.STONE, 2f, Items.IRON_BOOTS, 7f, Items.SHIELD, 5f,
                Items.SHULKER_BOX, 3f, Items.BUNDLE, 1f).entrySet())
            values.put(BuiltInRegistries.ITEM.getKey(entry.getKey()), entry.getValue());
        state.replace(state.settings(), values);
        var inventory = player.getInventory();
        inventory.clearContent();
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.inventoryMenu.getCraftSlots().clearContent();
        inventory.setItem(0, new ItemStack(Items.STONE, 8));
        inventory.setItem(36, new ItemStack(Items.IRON_BOOTS));
        inventory.setItem(40, new ItemStack(Items.SHIELD));
        check(player, 28, "equipment and hotbar without mainhand double counting");
        player.containerMenu.setCarried(inventory.removeItemNoUpdate(0));
        check(player, 28, "cursor transfer must preserve weight immediately");
        // Vanilla sends the recipe result immediately; synthetic dedicated-test players have no connection.
        if (player.connection != null) {
            player.inventoryMenu.getCraftSlots().setItem(0, player.containerMenu.getCarried());
            player.containerMenu.setCarried(ItemStack.EMPTY);
            check(player, 28, "personal crafting input must preserve weight");
            player.inventoryMenu.getCraftSlots().clearContent();
        }
        player.containerMenu.setCarried(ItemStack.EMPTY);
        var bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(new ItemStackTemplate(Items.STONE, 4))));
        var box = new ItemStack(Items.SHULKER_BOX, 2);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(bundle)));
        inventory.setItem(0, box);
        check(player, 36, "nested contents, shell, and stack count");
        values.put(BuiltInRegistries.ITEM.getKey(Items.STONE), 3f);
        state.replace(state.settings(), values);
        check(player, 44, "definition change must refresh nested contents");
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE, 7))));
        check(player, 60, "component change must refresh without inventory replacement");
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(bundle)));
        check(player, 44, "restore synchronized nested-container fixture");

        var recipes = IResourceList.snapshot(player.level());
        var inferred = RecipeWeights.resolve(recipes, Map.of(
                BuiltInRegistries.ITEM.getKey(Items.OAK_LOG), 8f,
                BuiltInRegistries.ITEM.getKey(Items.OAK_WOOD), 8f,
                BuiltInRegistries.ITEM.getKey(Items.STRIPPED_OAK_LOG), 8f,
                BuiltInRegistries.ITEM.getKey(Items.STRIPPED_OAK_WOOD), 8f,
                BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT), 2f));
        require(inferred.get(BuiltInRegistries.ITEM.getKey(Items.OAK_PLANKS)) == 2f, "Loaded log-to-planks recipe must divide by four");
        require(inferred.get(BuiltInRegistries.ITEM.getKey(Items.IRON_BLOCK)) == 18f, "Loaded compression recipe must use nine anchored ingots");
        require(!inferred.containsKey(BuiltInRegistries.ITEM.getKey(Items.CAKE)), "Remainder recipes must be excluded from inference");
    }

    private static void check(ServerPlayer player, float expected, String message) {
        PlayerEvents.onPlayerTick(player);
        require(PlayerHolder.getOrCreate(player).getWeight() == expected, message);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
