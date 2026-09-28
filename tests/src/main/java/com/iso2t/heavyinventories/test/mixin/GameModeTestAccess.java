package com.iso2t.heavyinventories.test.mixin;

import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerPlayerGameMode.class)
public interface GameModeTestAccess {
	@Invoker("setGameModeForPlayer")
	void heavyinventories$setMode (GameType mode, GameType previous);
}
