package com.iso2t.heavyinventories.test.mixin;

import com.iso2t.heavyinventories.server.weight.WeightPackData;
import com.iso2t.heavyinventories.server.weight.WeightPackReloadListener;
import com.iso2t.heavyinventories.test.DatapackLoadingScenario;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-jar only: fails the resource reload after a valid weight candidate has been staged. */
@Mixin(WeightPackReloadListener.class)
public abstract class ReloadFailureTestMixin {
    @Inject(method = "apply(Lcom/iso2t/heavyinventories/server/weight/WeightPackData$Result;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    private void heavyinventories$failReload(WeightPackData.Result result, ResourceManager manager,
                                           ProfilerFiller profiler, CallbackInfo ci) {
        if (DatapackLoadingScenario.failReload) throw new IllegalStateException("Intentional test-only reload failure");
    }
}
