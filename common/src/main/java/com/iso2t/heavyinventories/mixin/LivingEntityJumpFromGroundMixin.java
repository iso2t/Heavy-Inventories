package com.iso2t.heavyinventories.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityJumpFromGroundMixin {

    @Inject(method = "jumpFromGround()V", at = @At("HEAD"), cancellable = true)
    private void heavyinventories$preventJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof Player player)) return;
        var holder = PlayerHolder.getOrCreate(player);

        if (holder.preventsGroundJump()) {
            com.iso2t.heavyinventories.api.events.PlayerFeedback.jumpDenied(holder);
            ci.cancel();
        }
    }

}
