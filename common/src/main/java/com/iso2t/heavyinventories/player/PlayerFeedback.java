package com.iso2t.heavyinventories.player;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Consumer;

/**
 * The physical client installs presentation; dedicated servers keep the no-op handler.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerFeedback {

	private static Consumer<PlayerHolder> jumpDenied  = _ -> {
	};
	private static Consumer<PlayerHolder> fluidDenied = _ -> {
	};

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
