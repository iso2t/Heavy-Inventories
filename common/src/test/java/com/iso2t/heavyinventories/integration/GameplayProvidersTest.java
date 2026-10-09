package com.iso2t.heavyinventories.integration;

import com.iso2t.heavyinventories.api.provider.CapacityProvider;
import com.iso2t.heavyinventories.api.provider.InventoryProvider;
import com.iso2t.heavyinventories.config.WalkingMode;
import com.iso2t.heavyinventories.player.Encumbrance;
import com.iso2t.heavyinventories.weight.StackWeight;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GameplayProvidersTest {

	@BeforeAll
	static void bootstrap () {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		var defaults = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		BuiltInRegistries.ITEM.listElements().forEach(item -> item.bindComponents(defaults));
	}

	private static Identifier id (String value) {
		return Identifier.fromNamespaceAndPath("fixture", value);
	}

	private static GameplayProviders providers (Map<Identifier, InventoryProvider> inventory, Map<Identifier, CapacityProvider> capacity) {
		return new GameplayProviders(new CommonRegistrations(inventory, Map.of(), capacity, Map.of(), Map.of(), Map.of()));
	}

	@Test
	void stableSlotOrderAndCopiesProtectTheSnapshot () {
		var live = new ItemStack(Items.STONE, 2);
		var providers = providers(Map.of(id("inventory"), (player, sink) -> {
			sink.accept(id("b"), live);
			sink.accept(id("a"), ItemStack.EMPTY);
		}), Map.of());
		var first = providers.inventory(null, List.of());
		assertEquals(List.of(id("a"), id("b")), first.slots());
		live.setCount(5);
		assertEquals(2, first.stacks().get(1).getCount());
		assertEquals(5, providers.inventory(null, List.of()).stacks().get(1).getCount());
	}

	@Test
	void duplicateOwnershipAndVanillaAliasesFailTheWholeInventory () {
		var stone = new ItemStack(Items.STONE);
		assertFalse(providers(Map.of(id("a"), (p, sink) -> sink.accept(id("slot"), stone)), Map.of()).inventory(null, List.of(stone)).complete());
		assertFalse(providers(Map.of(id("a"), (p, sink) -> sink.accept(id("slot"), stone), id("b"), (p, sink) -> sink.accept(id("slot"), stone.copy())), Map.of()).inventory(null, List.of()).complete());
		assertFalse(providers(Map.of(id("a"), (p, sink) -> sink.accept(Identifier.withDefaultNamespace("slot"), stone)), Map.of()).inventory(null, List.of()).complete());
	}

	@Test
	void sinksAreBoundedAndExpireOnCallbackReturn () {
		var retained = new AtomicReference<InventoryProvider.SlotSink>();
		var providers = providers(Map.of(id("a"), (p, sink) -> {
			retained.set(sink);
			for (int i = 0; i < StackWeight.MAX_VISITS + 1; i++) if (!sink.accept(id("slot/" + i), ItemStack.EMPTY)) break;
		}), Map.of());
		assertFalse(providers.inventory(null, List.of()).complete());
		assertThrows(IllegalStateException.class, () -> retained.get().accept(id("late"), ItemStack.EMPTY));
	}

	@Test
	void invalidCapacityDoesNotCancelOtherBonusesOrReusePreviousValues () {
		var amount = new double[] { 125 };
		var providers = providers(Map.of(), Map.of(id("a"), p -> amount[0], id("b"), p -> 25));
		assertEquals(150, providers.capacity(null, 1000));
		for (double invalid : new double[] { -1, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE }) {
			amount[0] = invalid;
			assertEquals(25, providers.capacity(null, 1000));
		}
		amount[0] = 0;
		assertEquals(25, providers.capacity(null, 1000));
		assertEquals(0, providers(Map.of(), Map.of(id("a"), p -> (double) Float.MAX_VALUE)).capacity(null, Float.MAX_VALUE));
	}

	@Test
	void providersCannotReenterCalculationsAndGuardAlwaysClears () {
		assertThrows(IllegalStateException.class, () -> GameplayProviders.callback(() -> StackWeight.of(new ItemStack(Items.STONE), id -> 1)));
		assertEquals(1, StackWeight.of(new ItemStack(Items.STONE), id -> 1).weight());
	}

	@Test
	void additionalCapacityUsesTheSameThresholdsAndDoesNotAmplifyBuiltInBonuses () {
		var state = Encumbrance.calculate(1250, 1000, 1, 1, 1, 0, WalkingMode.PROGRESSIVE, false, 250);
		assertEquals(100, state.bracing());
		assertEquals(50, state.reinforced());
		assertEquals(100, state.strength());
		assertEquals(1500, state.capacity());
		assertFalse(state.encumbered());
		assertFalse(state.overloaded());
		assertEquals(Math.sqrt(1 - 1250d / 1500), state.walkingMultiplier(), 0.00001);
		assertTrue(Encumbrance.calculate(1250, 1000, 1, 1, 1, 0, WalkingMode.PROGRESSIVE, false, 0).overloaded());
	}

}
