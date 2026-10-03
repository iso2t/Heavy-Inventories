package com.iso2t.heavyinventories.api.client;

import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable frame input shared by both elements and XP positioning. Ring and number layouts are HI's original
 * proposals, not another owner's resolved result. Mode flags describe the player's selected mode; incomplete-weight
 * feedback may make the number layout visible in Ring mode.
 */
public record HudContext(Optional<PlayerWeightSnapshot> player, int screenWidth, int screenHeight, boolean visible, boolean ringEnabled, boolean numbersEnabled, HudLayout ring, HudLayout numbers) {
	public HudContext {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(ring, "ring");
		Objects.requireNonNull(numbers, "numbers");
		if (screenWidth < 0 || screenHeight < 0) throw new IllegalArgumentException("Screen dimensions cannot be negative");
		if (numbers.xpOffset() != 0) throw new IllegalArgumentException("Only the ring layout can move XP");
	}
}
