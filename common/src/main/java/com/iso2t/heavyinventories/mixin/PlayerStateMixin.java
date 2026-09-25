package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.api.player.PlayerStateAccess;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Entity ownership makes respawn, disconnect, and world shutdown cleanup automatic.
 */
@Mixin(Player.class)
public abstract class PlayerStateMixin implements PlayerStateAccess {

	@Unique
	private PlayerHolder heavyinventories$holder;

	@Override
	public PlayerHolder heavyinventories$getHolder () {
		if (heavyinventories$holder == null) {
			heavyinventories$holder = new PlayerHolder((Player) (Object) this);
		}
		return heavyinventories$holder;
	}
}
