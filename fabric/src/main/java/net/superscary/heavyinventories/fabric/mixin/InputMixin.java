package net.superscary.heavyinventories.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.superscary.heavyinventories.api.movement.ModifyPlayerMove;
import net.superscary.heavyinventories.api.player.PlayerHolder;
import net.superscary.heavyinventories.fabric.callbacks.PlayerInputCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Input.class)
public abstract class InputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void hi$tick(boolean slowDown, float f, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        var player = client.player;
        if (player == null) return;
        var holder = PlayerHolder.getOrCreate(player);

        ModifyPlayerMove.hook(player, (Input) (Object) this);
    }

}
