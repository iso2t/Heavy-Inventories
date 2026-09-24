package com.iso2t.heavyinventories.fabric.hooks;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.entity.player.Player;
import com.iso2t.heavyinventories.api.events.PlayerEvents;
import com.iso2t.heavyinventories.command.ModCommands;
import com.iso2t.heavyinventories.fabric.callbacks.PlayerCraftCallback;
import com.iso2t.heavyinventories.fabric.callbacks.PlayerPickupItemCallback;

public class ModHooks {

    public static void registerHooks() {

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, _) -> {
            if (entity instanceof Player player) {
                PlayerEvents.clone(player, player);
            }
        });


        PlayerPickupItemCallback.EVENT.register((livingEntity, slot, stack) -> PlayerEvents.onPickupItem(livingEntity.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, _) -> PlayerEvents.logout(handler.player));
        ServerPlayConnectionEvents.JOIN.register(((handler, _, _) -> PlayerEvents.logout(handler.player)));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, _, _) -> PlayerEvents.playerChangedDimension(player));
        PlayerCraftCallback.EVENT.register((player, stack) -> PlayerEvents.onCraft(player));

        registerCommands();
    }

    protected static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, _, _) -> ModCommands.registerCommands(commandDispatcher));
    }
}
