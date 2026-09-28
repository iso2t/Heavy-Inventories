package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.player.PlayerEvents;
import com.iso2t.heavyinventories.player.PlayerHolder;
import net.minecraft.server.level.ServerPlayer;

/**
 * Connectionless fixtures only rebuild state; connected players also exercise synchronization.
 */
public final class TestPlayerTick {
	public static void update (ServerPlayer player) {
		if (player.connection == null) PlayerHolder.getOrCreate(player).update();
		else PlayerEvents.onPlayerTick(player);
	}
}
