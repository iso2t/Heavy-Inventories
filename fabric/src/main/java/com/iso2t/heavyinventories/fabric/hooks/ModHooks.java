package com.iso2t.heavyinventories.fabric.hooks;

import com.iso2t.heavyinventories.api.events.PlayerEvents;
import com.iso2t.heavyinventories.command.ModCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class ModHooks {
	public static void registerHooks () {
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(PlayerEvents::onPlayerTick));
		CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> ModCommands.registerCommands(dispatcher));
	}
}
