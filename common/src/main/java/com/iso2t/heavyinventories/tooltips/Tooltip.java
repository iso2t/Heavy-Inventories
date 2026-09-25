package com.iso2t.heavyinventories.tooltips;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import com.iso2t.heavyinventories.client.ClientWeightData;
import net.minecraft.core.registries.BuiltInRegistries;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.api.weight.StackWeight;

import java.util.List;
import com.iso2t.heavyinventories.client.WeightDisplay;

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

        var single = StackWeight.of(stack.copyWithCount(1), ClientWeightData::unitWeight);
        var total = StackWeight.of(stack, ClientWeightData::unitWeight);
        if (!single.complete() || !total.complete()) {
            tooltip.add(Component.translatable("tooltip.heavyinventories.calculation_limit"));
            return tooltip;
        }
        weight = single.weight();
        tooltip.add(Component.translatable("tooltip.heavyinventories.item_weight", WeightDisplay.weight(weight, ConfigOptions.WEIGHT_MEASURE)));
        if (stack.getCount() > 1) tooltip.add(Component.translatable("tooltip.heavyinventories.item_stack_weight", WeightDisplay.weight(total.weight(), ConfigOptions.WEIGHT_MEASURE)));

        if (stack.getCount() < stack.getMaxStackSize()) {
            if (Minecraft.getInstance().hasShiftDown()) {
                var maximum = StackWeight.of(stack.copyWithCount(stack.getMaxStackSize()), ClientWeightData::unitWeight);
                tooltip.add(maximum.complete()
                        ? Component.translatable("tooltip.heavyinventories.item_max_stack_weight", WeightDisplay.weight(maximum.weight(), ConfigOptions.WEIGHT_MEASURE))
                        : Component.translatable("tooltip.heavyinventories.calculation_limit"));
            } else {
                tooltip.add(Component.translatable("tooltip.heavyinventories.hold_shift"));
            }
        }

        return tooltip;
    }

}
