package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.integration.client.ClientNotifications;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class ClientNotificationsMixin {

	@Inject(method = "tick", at = @At("TAIL"))
	private void heavyinventories$notifyPlayer (CallbackInfo ci) {
		ClientNotifications.tick();
	}

}
