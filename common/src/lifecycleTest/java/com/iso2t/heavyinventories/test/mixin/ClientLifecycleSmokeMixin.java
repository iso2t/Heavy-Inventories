package com.iso2t.heavyinventories.test.mixin;

import com.iso2t.heavyinventories.test.ClientLifecycleScenario;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class ClientLifecycleSmokeMixin {
	@Unique
	private final ClientLifecycleScenario heavyinventories$scenario = new ClientLifecycleScenario();

	@Inject(method = "tick", at = @At("TAIL"))
	private void heavyinventories$testClient (CallbackInfo ci) {
		heavyinventories$scenario.tick((Minecraft) (Object) this);
	}
}
