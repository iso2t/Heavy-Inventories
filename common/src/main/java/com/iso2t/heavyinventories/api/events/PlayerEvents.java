package com.iso2t.heavyinventories.api.events;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.api.player.PlayerWeightCache;
import net.minecraft.world.entity.player.Player;

public final class PlayerEvents {
    private PlayerEvents() {}

    /** Called after each server tick by the loader adapters; client state arrives through packets. */
    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide()) return;
        var holder = PlayerHolder.getOrCreate(player);
        holder.update();
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) holder.synchronize(serverPlayer);
    }

    /**
     * Existing equipment-bonus reset retained until the enchantment repair in Step 6.
     * Inventory recalculation is deferred to the tick after the change has completed.
     */
    public static void onUnequipItem(Player player) {
        if (player.level().isClientSide()) return;
        PlayerWeightCache.markDirty(player);
        var holder = PlayerHolder.getOrCreate(player);
        holder.clearBracing();
        holder.clearReinforced();
        holder.clearSureFooted();
    }
}
