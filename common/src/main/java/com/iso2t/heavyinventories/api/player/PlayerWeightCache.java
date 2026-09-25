package com.iso2t.heavyinventories.api.player;

import com.iso2t.heavyinventories.api.weight.CalculateWeight;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * One entity's inventory snapshot and total. No UUID map or global player references.
 * All access runs on the owning entity's game thread.
 */
public final class PlayerWeightCache {
    static final int FALLBACK_TICKS = 20;
    private final List<ItemStack> snapshot = new ArrayList<>();
    private Object level;
    private long lastComputedTick;
    private float weight;
    private boolean dirty = true;

    PlayerWeightCache() {}

    public static float getOrCompute(Player player) {
        if (player.level().isClientSide()) return PlayerHolder.getOrCreate(player).getWeight();
        var stacks = CalculateWeight.carriedStacks(player);
        return PlayerHolder.getOrCreate(player).weightCache().compute(
                stacks, player.level(), player.tickCount, () -> CalculateWeight.from(player, stacks));
    }

    /** Invalidation never reads partially updated inventories or calls back into holder.update(). */
    public static void markDirty(Player player) {
        PlayerHolder.getOrCreate(player).weightCache().invalidate();
    }

    /** Invalidate only this server's live entities, never integrated-client state. */
    public static void clearAll(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(PlayerWeightCache::markDirty);
    }

    void invalidate() {
        dirty = true;
    }

    float compute(Container inventory, Object currentLevel, long tick, DoubleSupplier calculate) {
        var stacks = new ArrayList<ItemStack>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) stacks.add(inventory.getItem(slot));
        return compute(stacks, currentLevel, tick, calculate);
    }

    float compute(List<ItemStack> stacks, Object currentLevel, long tick, DoubleSupplier calculate) {
        if (dirty || level != currentLevel || tick < lastComputedTick
                || tick - lastComputedTick >= FALLBACK_TICKS || inventoryChanged(stacks)) {
            float updatedWeight = (float) calculate.getAsDouble();
            snapshot.clear();
            for (var stack : stacks) {
                snapshot.add(stack.copy());
            }
            weight = updatedWeight;
            level = currentLevel;
            lastComputedTick = tick;
            dirty = false;
        }
        return weight;
    }

    private boolean inventoryChanged(List<ItemStack> stacks) {
        if (snapshot.size() != stacks.size()) return true;
        for (int slot = 0; slot < snapshot.size(); slot++) {
            // Includes count and all data components, rather than a lossy item/damage hash.
            if (!ItemStack.matches(snapshot.get(slot), stacks.get(slot))) return true;
        }
        return false;
    }
}
