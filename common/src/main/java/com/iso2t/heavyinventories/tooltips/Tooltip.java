package com.iso2t.heavyinventories.tooltips;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import com.iso2t.heavyinventories.client.ClientWeightData;
import net.minecraft.core.registries.BuiltInRegistries;
import com.iso2t.heavyinventories.config.ConfigOptions;

import java.util.List;

public class Tooltip {

    /**
     * Adds the weight tooltips to the given list of tooltips.
     * @param tooltip The list of tooltips to add to.
     * @param stack The {@link ItemStack} to get the weight for.
     * @return The list of tooltips with the weight tooltips added.
     */
    public static List<Component> addTooltips(List<Component> tooltip, ItemStack stack) {
        Float weight = ClientWeightData.weight(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (weight == null || stack.isEmpty()) return tooltip;

        tooltip.add(Component.translatable("tooltip.heavyinventories.item_weight", weight, ConfigOptions.WEIGHT_MEASURE.getSub()));
        if (stack.getCount() > 1) tooltip.add(Component.translatable("tooltip.heavyinventories.item_stack_weight", weight * stack.getCount(), ConfigOptions.WEIGHT_MEASURE.getSub()));

        if (stack.getCount() < stack.getMaxStackSize()) {
            if (Minecraft.getInstance().hasShiftDown()) {
                tooltip.add(Component.translatable("tooltip.heavyinventories.item_max_stack_weight", weight * stack.getMaxStackSize(), ConfigOptions.WEIGHT_MEASURE.getSub()));
            } else {
                tooltip.add(Component.translatable("tooltip.heavyinventories.hold_shift"));
            }
        }

        return tooltip;
    }

}
