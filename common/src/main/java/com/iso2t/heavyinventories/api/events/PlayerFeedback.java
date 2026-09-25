package com.iso2t.heavyinventories.api.events;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import java.util.function.Consumer;

/** The physical client installs presentation; dedicated servers keep the no-op handler. */
public final class PlayerFeedback {
    private static Consumer<PlayerHolder> jumpDenied = holder -> {};
    private PlayerFeedback() {}
    public static void registerJumpNotice(Consumer<PlayerHolder> listener) { jumpDenied = listener; }
    public static void jumpDenied(PlayerHolder holder) { jumpDenied.accept(holder); }
}
