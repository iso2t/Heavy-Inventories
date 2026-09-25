package com.iso2t.heavyinventories.neoforge.hooks;

import com.iso2t.heavyinventories.api.events.PlayerEvents;
import com.iso2t.heavyinventories.command.ModCommands;
import com.iso2t.heavyinventories.server.weight.WeightPackReloadListener;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class ModHooks {
    public static void hookReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(WeightPackReloadListener.ID, new WeightPackReloadListener());
    }

    public static void hookServerStart(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        com.iso2t.heavyinventories.server.ServerWeightState.start(event.getServer());
    }
    public static void hookServerTick(ServerTickEvent.Post event) {
        event.getServer().getPlayerList().getPlayers().forEach(PlayerEvents::onPlayerTick);
    }


    public static void hookCommands(RegisterCommandsEvent event) {
        ModCommands.registerCommands(event.getDispatcher());
    }
}
