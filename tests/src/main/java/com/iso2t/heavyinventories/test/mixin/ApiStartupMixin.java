package com.iso2t.heavyinventories.test.mixin;

import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.test.plugin.ApiWeightChecks;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWeightState.class)
public abstract class ApiStartupMixin {
	@Inject(method = "start", at = @At("HEAD"))
	private static void beforeDefinitions (MinecraftServer server, CallbackInfo ci) {
		ApiWeightChecks.beforeDefinitions(server);
	}
}
