package com.iso2t.heavyinventories.api.events;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerEvents {

	/**
	 * Called after each server tick; clients consume the resulting snapshot.
	 */
	public static void onPlayerTick (Player player) {
		if (player.level().isClientSide()) return;
		var holder = PlayerHolder.getOrCreate(player);
		holder.update();
		if (player instanceof ServerPlayer target) holder.synchronize(target);
	}
}
