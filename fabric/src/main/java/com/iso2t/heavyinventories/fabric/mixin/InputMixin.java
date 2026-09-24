package com.iso2t.heavyinventories.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import com.iso2t.heavyinventories.api.movement.ModifyPlayerMove;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class InputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void hi$tick(CallbackInfo ci) {
        var client = Minecraft.getInstance();
        var player = client.player;
        if (player == null) return;

        ModifyPlayerMove.hook(player, ((ClientInput) (Object) this).keyPresses);
    }

}
