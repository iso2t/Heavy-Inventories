package com.iso2t.heavyinventories.test.mixin;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerInfo.class)
public interface ClientGameModeTestAccess {

	@Invoker("setGameMode")
	void heavyinventories$setMode (GameType mode);

}
