package com.iso2t.heavyinventories.tooltips;

import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.client.WeightDisplay;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.weight.StackWeight;
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
		tooltip.add(stacked ? stackWeightLine(total.weight()) : itemWeightLine(total.weight()));

		if (stacked || belowMaxStack) {
			if (showDetails) {
				if (stacked) {
					var single = StackWeight.of(stack.copyWithCount(1), ClientWeightData::unitWeight);
					tooltip.add(single.complete() ? itemWeightLine(single.weight()) : calculationLimit());
				}
				if (belowMaxStack) {
					var maximum = StackWeight.of(stack.copyWithCount(stack.getMaxStackSize()), ClientWeightData::unitWeight);
					tooltip.add(maximum.complete() ? Component.translatableWithFallback("tooltip.heavyinventories.item_max_stack_weight", "Max Stack Weight: %s", weightValue(maximum.weight())).withStyle(ChatFormatting.GRAY) : calculationLimit());
				}
			} else {
				tooltip.add(Component.translatableWithFallback("tooltip.heavyinventories.hold_shift", "Hold %s for more info", Component.translatableWithFallback("tooltip.heavyinventories.shift_key", "[SHIFT]").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
			}
		}

		return tooltip;
	}

	private static Component itemWeightLine (float weight) {
		return Component.translatableWithFallback("tooltip.heavyinventories.item_weight", "Weight: %s", weightValue(weight)).withStyle(ChatFormatting.GRAY);
	}

	private static Component stackWeightLine (float weight) {
		return Component.translatableWithFallback("tooltip.heavyinventories.item_stack_weight", "Stack Weight: %s", weightValue(weight)).withStyle(ChatFormatting.GRAY);
	}

	private static Component weightValue (float weight) {
		return Component.literal(WeightDisplay.weight(weight, ConfigOptions.WEIGHT_MEASURE)).withStyle(ChatFormatting.GOLD);
	}

	private static Component calculationLimit () {
		return Component.translatableWithFallback("tooltip.heavyinventories.calculation_limit", "Contents exceed the weight calculation limit; treated as over capacity.").withStyle(ChatFormatting.RED);
	}

}
