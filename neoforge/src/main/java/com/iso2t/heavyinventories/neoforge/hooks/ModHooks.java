package com.iso2t.heavyinventories.neoforge.hooks;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.iso2t.heavyinventories.api.events.PlayerEvents;
import com.iso2t.heavyinventories.command.ModCommands;

public class ModHooks {


    public static void hookPlayer(PlayerTickEvent.Pre event) {
        PlayerEvents.onPlayerTick(event.getEntity());
    }

    public static void hookPlayerClone(PlayerEvent.Clone event) {
        PlayerEvents.clone(event.getOriginal(), event.getEntity());
    }

    public static void hookPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        PlayerEvents.playerChangedDimension(event.getEntity());
    }

    public static void hookPlayerPickupItem(ItemEntityPickupEvent.Post event) {
        PlayerEvents.onPickupItem(event.getPlayer());
    }

    public static void hookOnCraft(PlayerEvent.ItemCraftedEvent event) {
        PlayerEvents.onCraft(event.getEntity());
    }

    public static void hookOnSmelt(PlayerEvent.ItemSmeltedEvent event) {
        PlayerEvents.onSmelt(event.getEntity());
    }

    public static void hookPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerEvents.logout(event.getEntity());
    }


    public static void hookPlayerEquip(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if(!event.getSlot().isArmor()) return;

        //PlayerEvents.onEquipItem(player);
        PlayerEvents.onUnequipItem(player);
    }

    public static void hookCommands(RegisterCommandsEvent event) {
        ModCommands.registerCommands(event.getDispatcher());
    }

}
