package com.iso2t.heavyinventories.test.plugin.mixin;

import com.iso2t.heavyinventories.test.plugin.PluginProbe;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class AbsentHiSmokeMixin {

	@Unique
	private boolean hiFixture$tested;

	@Inject(method = "tickServer", at = @At("TAIL"))
	private void hiFixture$withoutHi (BooleanSupplier haveTime, CallbackInfo ci) {
		if (hiFixture$tested) return;
		hiFixture$tested = true;
		try {
			Class.forName("com.iso2t.heavyinventories.api.plugin.HeavyInventoriesPlugin");
			throw new AssertionError("HI API must be absent from this fixture run");
		} catch (ClassNotFoundException expected) {
		}
		if (PluginProbe.commonRegistrations != 0 || PluginProbe.clientRegistrations != 0) throw new AssertionError("Optional plugin loaded without HI");
		LogUtils.getLogger().info("API PLUGINS ABSENT PASSED: optional consumer starts without HI or its API");
		((MinecraftServer) (Object) this).halt(false);
	}

}
