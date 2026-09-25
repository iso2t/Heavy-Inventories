package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.server.ServerStateAccess;
import com.iso2t.heavyinventories.server.ServerWeightState;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftServer.class)
public abstract class ServerWeightStateMixin implements ServerStateAccess {
    @Unique private final ServerWeightState heavyinventories$weightState = new ServerWeightState();
    @Override public ServerWeightState heavyinventories$getWeightState() { return heavyinventories$weightState; }
    /** Run only after the whole vanilla reload succeeds, with finalized recipes/tags and the active resource manager. */
    @Inject(method = "reloadResources", at = @At("RETURN"), cancellable = true)
    private void heavyinventories$afterReload(Collection<String> packs,
            CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        var server = (MinecraftServer) (Object) this;
        cir.setReturnValue(cir.getReturnValue().thenRunAsync(() -> heavyinventories$weightState.reloadDatapacks(server), server));
    }
}
