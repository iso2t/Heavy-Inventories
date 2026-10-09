package com.iso2t.heavyinventories.test.mixin;

import com.iso2t.heavyinventories.HeavyInventories;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Exercises the actual new-world resource handoff using the disposable client's existing save.
 */
@Mixin(WorldOpenFlows.class)
public abstract class NewWorldWeightSmokeMixin {

	@Inject(method = "openWorldDoLoad", at = @At("HEAD"), cancellable = true)
	private void heavyinventories$newWorldHandoff (LevelStorageSource.LevelStorageAccess access, WorldStem stem, PackRepository packs, CallbackInfo ci) {
		if (!Boolean.getBoolean("heavyinventories.test.newWorldHandoff")) return;
		stem.close();
		((WorldOpenFlows) (Object) this).createLevelFromExistingSettings(access, stem.dataPackResources(), stem.registries(), stem.worldDataAndGenSettings(), Optional.empty());
		HeavyInventories.LOGGER.info("NEW WORLD HANDOFF EXERCISED: vanilla replaced the resource manager while retaining loaded server data");
		ci.cancel();
	}

}
