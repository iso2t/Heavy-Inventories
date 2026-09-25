package com.iso2t.heavyinventories.api.weight;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class StackWeightTest {
	@BeforeAll
	static void bootstrap () {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		var defaults = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		BuiltInRegistries.ITEM.listElements().forEach(item -> item.bindComponents(defaults));
	}

	@Test
	void shellContentsAndOuterCountAreCountedOnce () {
		var bundle = new ItemStack(Items.BUNDLE);
		bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(new ItemStackTemplate(Items.STONE, 4))));
		var box = box(bundle);
		box.setCount(2); // Component contents describe each container in the stack.
		var result = StackWeight.of(box, id -> switch (id.getPath()) {
			case "shulker_box" -> 3;
			case "bundle" -> 1;
			case "stone" -> 2;
			default -> throw new AssertionError(id);
		});
		assertEquals(24, result.weight());
		assertTrue(result.complete());
	}

	@Test
	void componentMutationChangesWeightAndBothContentComponentsCount () {
		var container = box(new ItemStack(Items.STONE, 3));
		assertEquals(4, StackWeight.of(container, id -> 1).weight());
		container.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE, 5))));
		container.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(new ItemStackTemplate(Items.DIRT, 2))));
		assertEquals(8, StackWeight.of(container, id -> 1).weight());
	}

	@Test
	void depthAndWorkLimitsNeverPublishAnUnderestimatedTotal () {
		ItemStack nested = new ItemStack(Items.STONE);
		for (int i = 0; i < StackWeight.MAX_DEPTH; i++) nested = box(nested);
		assertTrue(StackWeight.of(nested, id -> 1).complete());
		assertFalse(StackWeight.of(box(nested), id -> 1).complete());
		var wide = new ItemStack(Items.BUNDLE);
		wide.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(Collections.nCopies(StackWeight.MAX_VISITS, new ItemStackTemplate(Items.STONE))));
		var limited = StackWeight.of(wide, id -> 1);
		assertFalse(limited.complete());
		assertEquals(StackWeight.TOO_COMPLEX, limited.weight());
		assertFalse(StackWeight.total(Collections.nCopies(StackWeight.MAX_VISITS + 1, new ItemStack(Items.STONE)), id -> 1).complete());
	}

	@Test
	void missingInvalidOrOverflowingDefinitionsDoNotProduceNaNOrPartialWeight () {
		for (double value : new double[] { Double.NaN, Double.POSITIVE_INFINITY, -1, Double.MAX_VALUE }) {
			var result = StackWeight.of(new ItemStack(Items.STONE, 64), id -> value);
			assertFalse(result.complete());
			assertEquals(StackWeight.TOO_COMPLEX, result.weight());
		}
		assertEquals(0, StackWeight.of(ItemStack.EMPTY, id -> {
			throw new AssertionError();
		}).weight());
		assertEquals(0, StackWeight.of(new ItemStack(Items.STONE), id -> 0).weight());
	}

	@Test
	void calculationsRetainFractionsUnderCommaDecimalLocale () {
		var previous = Locale.getDefault();
		try {
			Locale.setDefault(Locale.GERMANY);
			assertEquals(0.375f, StackWeight.of(new ItemStack(Items.STONE, 3), id -> 0.125).weight());
		} finally {
			Locale.setDefault(previous);
		}
	}

	private static ItemStack box (ItemStack contents) {
		var stack = new ItemStack(Items.SHULKER_BOX);
		stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(contents)));
		return stack;
	}
}
