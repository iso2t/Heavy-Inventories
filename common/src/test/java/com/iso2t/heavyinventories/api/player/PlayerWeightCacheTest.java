package com.iso2t.heavyinventories.api.player;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.*;

class PlayerWeightCacheTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // 26.1 binds item defaults during world loading. This unit fixture needs
        // only stack size; component mutations below supply their own test data.
        var defaults = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
        BuiltInRegistries.ITEM.listElements().forEach(item -> item.bindComponents(defaults));
    }

    @Test
    void inventoryMutationsRefreshWithoutPickupOrCraftCallbacks() {
        var cache = new PlayerWeightCache();
        var inventory = new SimpleContainer(43);
        var level = new Object();
        DoubleSupplier count = () -> {
            int total = 0;
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) total += inventory.getItem(slot).getCount();
            return total;
        };
        assertEquals(0, cache.compute(inventory, level, 0, count));
        inventory.setItem(0, new ItemStack(Items.STONE, 32)); // command/container transfer
        assertEquals(32, cache.compute(inventory, level, 1, count));
        inventory.getItem(0).shrink(3); // dropping/consumption mutates the same stack
        assertEquals(29, cache.compute(inventory, level, 2, count));
        inventory.setItem(36, new ItemStack(Items.IRON_BOOTS));
        inventory.setItem(40, new ItemStack(Items.SHIELD));
        assertEquals(31, cache.compute(inventory, level, 3, count));
        inventory.removeItemNoUpdate(0);
        assertEquals(2, cache.compute(inventory, level, 4, count));
        inventory.clearContent(); // death without keepInventory, /clear
        assertEquals(0, cache.compute(inventory, level, 5, count));
    }

    @Test
    void equalStacksReuseCacheButItemAndComponentChangesDoNot() {
        var cache = new PlayerWeightCache();
        var inventory = new SimpleContainer(new ItemStack(Items.STONE));
        var level = new Object();
        var calls = new AtomicInteger();
        DoubleSupplier compute = calls::incrementAndGet;
        assertEquals(1, cache.compute(inventory, level, 0, compute));
        inventory.setItem(0, new ItemStack(Items.STONE));
        assertEquals(1, cache.compute(inventory, level, 1, compute));
        inventory.setItem(0, new ItemStack(Items.DIRT));
        assertEquals(2, cache.compute(inventory, level, 2, compute));
        inventory.getItem(0).set(DataComponents.CUSTOM_NAME, Component.literal("changed"));
        assertEquals(3, cache.compute(inventory, level, 3, compute));
        inventory.getItem(0).remove(DataComponents.CUSTOM_NAME);
        assertEquals(4, cache.compute(inventory, level, 4, compute));
    }

    @Test
    void nestedContentsAndEquipmentComponentsInvalidateSnapshot() {
        var cache = new PlayerWeightCache();
        var inventory = new SimpleContainer(43);
        var level = new Object();
        var calls = new AtomicInteger();
        DoubleSupplier compute = calls::incrementAndGet;
        inventory.setItem(0, new ItemStack(Items.SHULKER_BOX));
        inventory.setItem(36, new ItemStack(Items.IRON_BOOTS));
        assertEquals(1, cache.compute(inventory, level, 0, compute));
        inventory.getItem(0).set(DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE, 64))));
        assertEquals(2, cache.compute(inventory, level, 1, compute));
        inventory.getItem(36).set(DataComponents.DAMAGE, 1);
        assertEquals(3, cache.compute(inventory, level, 2, compute));
        // Snapshot detection is tested here; actual equipment/container weight rules are Step 5.
    }

    @Test
    void fallbackIsBoundedAndDirtyInvalidationIsLazy() {
        var cache = new PlayerWeightCache();
        var inventory = new SimpleContainer(1);
        var level = new Object();
        var calls = new AtomicInteger();
        DoubleSupplier compute = calls::incrementAndGet;
        assertEquals(1, cache.compute(inventory, level, 7, compute));
        for (int tick = 8; tick < 27; tick++) assertEquals(1, cache.compute(inventory, level, tick, compute));
        assertEquals(2, cache.compute(inventory, level, 27, compute));
        cache.invalidate();
        cache.invalidate();
        assertEquals(2, calls.get());
        assertEquals(3, cache.compute(inventory, level, 28, compute));
        assertEquals(3, cache.compute(inventory, level, 28, compute));
    }

    @Test
    void dimensionChangeAndClockResetForceRefresh() {
        var cache = new PlayerWeightCache();
        var inventory = new SimpleContainer(1);
        var overworld = new Object();
        var nether = new Object();
        assertEquals(10, cache.compute(inventory, overworld, 100, () -> 10));
        assertEquals(20, cache.compute(inventory, nether, 101, () -> 20));
        assertEquals(30, cache.compute(inventory, nether, 0, () -> 30));
    }

    @Test
    void replacementAndLogicalSideCachesAreIndependent() {
        var server = new PlayerWeightCache();
        var client = new PlayerWeightCache();
        var replacement = new PlayerWeightCache();
        var inventory = new SimpleContainer(1);
        var level = new Object();
        assertEquals(10, server.compute(inventory, level, 0, () -> 10));
        assertEquals(20, client.compute(inventory, level, 0, () -> 20));
        assertEquals(0, replacement.compute(inventory, level, 0, () -> 0));
        server.invalidate();
        assertEquals(20, client.compute(inventory, level, 1, () -> fail("Server invalidation leaked to client")));
        assertEquals(0, replacement.compute(inventory, level, 1, () -> fail("Old entity invalidation leaked")));
    }

    @Test
    void failedCalculationDoesNotMarkCacheClean() {
        var cache = new PlayerWeightCache();
        var inventory = new SimpleContainer(1);
        var level = new Object();
        assertThrows(IllegalStateException.class, () -> cache.compute(inventory, level, 0, () -> {
            throw new IllegalStateException("test");
        }));
        assertEquals(7, cache.compute(inventory, level, 1, () -> 7));
    }
}
