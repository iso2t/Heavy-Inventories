package com.iso2t.heavyinventories.tooltips;

import com.iso2t.heavyinventories.weight.StackWeight;
import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.client.WeightDisplay;
import com.iso2t.heavyinventories.config.ConfigOptions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Tooltip {

	public static List<Component> addTooltips (List<Component> tooltip, ItemStack stack) {
		return addTooltips(tooltip, stack, Minecraft.getInstance().hasShiftDown());
	}

	static List<Component> addTooltips (List<Component> tooltip, ItemStack stack, boolean showDetails) {
		if (stack.isEmpty() || ClientWeightData.weight(BuiltInRegistries.ITEM.getKey(stack.getItem())) == null) return tooltip;

		var total = StackWeight.of(stack, ClientWeightData::unitWeight);
		if (!total.complete()) {
			tooltip.add(calculationLimit());
			return tooltip;
		}
		boolean stacked = stack.getCount() > 1;
		boolean belowMaxStack = stack.getCount() < stack.getMaxStackSize();
		tooltip.add(weightLine(stacked ? "tooltip.heavyinventories.item_stack_weight" : "tooltip.heavyinventories.item_weight", total.weight()));

		if (stacked || belowMaxStack) {
			if (showDetails) {
				if (stacked) {
					var single = StackWeight.of(stack.copyWithCount(1), ClientWeightData::unitWeight);
					tooltip.add(single.complete() ? weightLine("tooltip.heavyinventories.item_weight", single.weight()) : calculationLimit());
				}
				if (belowMaxStack) {
					var maximum = StackWeight.of(stack.copyWithCount(stack.getMaxStackSize()), ClientWeightData::unitWeight);
					tooltip.add(maximum.complete() ? weightLine("tooltip.heavyinventories.item_max_stack_weight", maximum.weight()) : calculationLimit());
				}
			} else {
				tooltip.add(Component.translatable("tooltip.heavyinventories.hold_shift", Component.translatable("tooltip.heavyinventories.shift_key").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
			}
		}

		return tooltip;
	}

	private static Component weightLine (String translation, float weight) {
		return Component.translatable(translation, Component.literal(WeightDisplay.weight(weight, ConfigOptions.WEIGHT_MEASURE)).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY);
	}

	private static Component calculationLimit () {
		return Component.translatable("tooltip.heavyinventories.calculation_limit").withStyle(ChatFormatting.RED);
	}

}
