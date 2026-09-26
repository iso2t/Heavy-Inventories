package com.iso2t.heavyinventories.api.events;

import com.iso2t.heavyinventories.api.player.PlayerHolder;

import java.util.function.Consumer;

/**
 * The physical client installs presentation; dedicated servers keep the no-op handler.
 */
public final class PlayerFeedback {

	private static Consumer<PlayerHolder> jumpDenied = _ -> {
	};
	private static Consumer<PlayerHolder> fluidDenied = _ -> {};

	private PlayerFeedback () {
	}

	public static void registerJumpNotice (Consumer<PlayerHolder> listener) {
		jumpDenied = listener;
	}

	public static void jumpDenied (PlayerHolder holder) {
		jumpDenied.accept(holder);
	}

	public static void registerFluidNotice (Consumer<PlayerHolder> listener) {
		fluidDenied = listener;
	}

	public static void fluidAscentDenied (PlayerHolder holder) {
		fluidDenied.accept(holder);
	}
}
