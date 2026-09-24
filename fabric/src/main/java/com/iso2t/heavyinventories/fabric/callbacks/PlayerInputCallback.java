package com.iso2t.heavyinventories.fabric.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;

public interface PlayerInputCallback {

    Event<PlayerInputCallback> EVENT = EventFactory.createArrayBacked(
            PlayerInputCallback.class,
            listeners -> (player, input) -> {
                for (var l : listeners) l.onInput(player, input);
            }
    );

    void onInput (Player player, Input input);

}
