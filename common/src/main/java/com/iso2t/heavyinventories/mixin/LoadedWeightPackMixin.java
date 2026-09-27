package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.server.weight.WeightPackAccess;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableServerResources.class)
public abstract class LoadedWeightPackMixin {

	/**
	 * New-world creation replaces the resource manager but keeps these loaded recipes and datapack results.
	 */
	@Inject(method = "loadResources", at = @At("RETURN"), cancellable = true)
	private static void heavyinventories$retainLoadedWeights (ResourceManager manager, LayeredRegistryAccess<RegistryLayer> layers, List<Registry.PendingTags<?>> tags, FeatureFlagSet features, Commands.CommandSelection commands, PermissionSet permissions, Executor backgroundExecutor, Executor mainThreadExecutor, CallbackInfoReturnable<CompletableFuture<ReloadableServerResources>> cir) {
		cir.setReturnValue(cir.getReturnValue().thenApply(resources -> {
			((WeightPackAccess) manager).heavyinventories$getWeightPackData().ifPresent(candidate -> ((WeightPackAccess) resources.getRecipeManager()).heavyinventories$setWeightPackData(candidate));
			return resources;
		}));
	}
}
