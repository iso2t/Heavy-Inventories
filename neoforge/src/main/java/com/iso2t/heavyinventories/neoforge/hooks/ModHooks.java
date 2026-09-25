package com.iso2t.heavyinventories.neoforge.hooks;

import com.iso2t.heavyinventories.api.events.PlayerEvents;
import com.iso2t.heavyinventories.command.ModCommands;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class ModHooks {
    public static void hookServerStart(net.neoforged.neoforge.event.server.ServerAboutToStartEvent event) {
        com.iso2t.heavyinventories.server.ServerWeightState.start(event.getServer());
    }
    public static void hookServerTick(ServerTickEvent.Post event) {
        event.getServer().getPlayerList().getPlayers().forEach(PlayerEvents::onPlayerTick);
    }

    public static void hookPlayerEquip(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!event.getSlot().isArmor()) return;
        PlayerEvents.onUnequipItem(player);
    }

    public static void hookCommands(RegisterCommandsEvent event) {
        ModCommands.registerCommands(event.getDispatcher());
    }
}
