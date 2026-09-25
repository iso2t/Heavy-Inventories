package com.iso2t.heavyinventories.api.events;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import net.minecraft.world.entity.player.Player;

public final class PlayerEvents {
    private PlayerEvents() {}

    /** Called after each server tick; clients consume the resulting snapshot. */
    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide()) return;
        var holder = PlayerHolder.getOrCreate(player);
        holder.update();
        if (player instanceof net.minecraft.server.level.ServerPlayer target) holder.synchronize(target);
    }
}
