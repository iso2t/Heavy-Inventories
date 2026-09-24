package com.iso2t.heavyinventories.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import com.iso2t.heavyinventories.fabric.callbacks.PlayerCraftCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
abstract class CraftingResultSlotMixin {

    @Inject(method = "removed", at = @At("TAIL"))
    private void hi$afterCraft(Player player, CallbackInfo ci) {
        if (!player.level().isClientSide() && player instanceof ServerPlayer sp) {
            PlayerCraftCallback.EVENT.invoker().onCrafted(sp, ((CraftingMenu) (Object) this).getResultSlot().getItem());
        }
    }
}
