package com.iso2t.heavyinventories.neoforge.hooks;

import com.iso2t.heavyinventories.api.events.PlayerEvents;
import com.iso2t.heavyinventories.command.ModCommands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class ModHooks {
    public static void hookServerStart(net.neoforged.neoforge.event.server.ServerAboutToStartEvent event) {
        com.iso2t.heavyinventories.server.ServerWeightState.start(event.getServer());
    }
    public static void hookServerTick(ServerTickEvent.Post event) {
        event.getServer().getPlayerList().getPlayers().forEach(PlayerEvents::onPlayerTick);
    }


    public static void hookCommands(RegisterCommandsEvent event) {
        ModCommands.registerCommands(event.getDispatcher());
    }
}
