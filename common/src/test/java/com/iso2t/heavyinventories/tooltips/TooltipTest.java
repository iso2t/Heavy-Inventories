package com.iso2t.heavyinventories.tooltips;

import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.client.WeightDisplay;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.network.ItemWeightsPayload;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipTest {

	@BeforeAll
	static void bootstrap () {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		var defaults = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		BuiltInRegistries.ITEM.listElements().forEach(item -> item.bindComponents(defaults));
	}

	@BeforeEach
	void loadDefinitions () {
		ClientWeightData.clear();
		ClientWeightData.accept(new ItemWeightsPayload(1, 0, 1, List.of(new ItemWeightsPayload.Entry(BuiltInRegistries.ITEM.getKey(Items.ARROW), 2), new ItemWeightsPayload.Entry(BuiltInRegistries.ITEM.getKey(Items.SHULKER_BOX), 3))));
	}

	@AfterEach
	void clearDefinitions () {
		ClientWeightData.clear();
	}

	@Test
	void collapsedTooltipsShowOnlyTheHoveredQuantity () {
		for (int count : new int[] { 1, 8, 64 }) {
			var lines = Tooltip.addTooltips(new ArrayList<>(), new ItemStack(Items.ARROW, count), false);
			assertEquals(2, lines.size());
			assertWeight(lines.getFirst(), count == 1 ? "item_weight" : "item_stack_weight", count * 2);
			assertEquals("tooltip.heavyinventories.hold_shift", contents(lines.getLast()).getKey());
		}
	}

	@Test
	void expandedTooltipsAvoidDuplicateQuantities () {
		var single = Tooltip.addTooltips(new ArrayList<>(), new ItemStack(Items.ARROW), true);
		assertEquals(2, single.size());
		assertWeight(single.getFirst(), "item_weight", 2);
		assertWeight(single.getLast(), "item_max_stack_weight", 128);

		var partial = Tooltip.addTooltips(new ArrayList<>(), new ItemStack(Items.ARROW, 8), true);
		assertEquals(3, partial.size());
		assertWeight(partial.get(0), "item_stack_weight", 16);
		assertWeight(partial.get(1), "item_weight", 2);
		assertWeight(partial.get(2), "item_max_stack_weight", 128);

		var full = Tooltip.addTooltips(new ArrayList<>(), new ItemStack(Items.ARROW, 64), true);
		assertEquals(2, full.size());
		assertWeight(full.getFirst(), "item_stack_weight", 128);
		assertWeight(full.getLast(), "item_weight", 2);
	}

	@Test
	void containerContentsRemainPartOfTheDisplayedWeight () {
		var box = new ItemStack(Items.SHULKER_BOX, 2);
		box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.ARROW, 4))));
		var lines = Tooltip.addTooltips(new ArrayList<>(), box, true);
		assertWeight(lines.get(0), "item_stack_weight", 22);
		assertWeight(lines.get(1), "item_weight", 11);

		box.setCount(1);
		box.set(DataComponents.MAX_STACK_SIZE, 1);
		for (boolean expanded : new boolean[] { false, true }) {
			var unstackable = Tooltip.addTooltips(new ArrayList<>(), box, expanded);
			assertEquals(1, unstackable.size());
			assertWeight(unstackable.getFirst(), "item_weight", 11);
		}
	}

	@Test
	void missingDefinitionsNeverProduceInventedWeights () {
		assertTrue(Tooltip.addTooltips(new ArrayList<>(), ItemStack.EMPTY, false).isEmpty());
		var box = new ItemStack(Items.SHULKER_BOX);
		box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE))));
		var lines = Tooltip.addTooltips(new ArrayList<>(), box, false);
		assertEquals(1, lines.size());
		assertEquals("tooltip.heavyinventories.calculation_limit", contents(lines.getFirst()).getKey());
		ClientWeightData.clear();
		assertTrue(Tooltip.addTooltips(new ArrayList<>(), box, true).isEmpty());
	}

	private static TranslatableContents contents (Component line) {
		return (TranslatableContents) line.getContents();
	}

	private static void assertWeight (Component line, String key, float weight) {
		assertEquals("tooltip.heavyinventories." + key, contents(line).getKey());
		var displayed = (Component) contents(line).getArgs()[0];
		assertEquals(WeightDisplay.weight(weight, ConfigOptions.WEIGHT_MEASURE), displayed.getString());
	}

}
