package com.iso2t.heavyinventories.fabric.core;

import com.iso2t.heavyinventories.fabric.enchantments.ModEnchantmentEffects;
import com.iso2t.heavyinventories.fabric.hooks.ModHooks;
import com.iso2t.heavyinventories.fabric.platform.FabricConfigScreenHelper;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.server.weight.WeightPackReloadListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;

public final class HeavyInventoriesFabric implements ModInitializer {

	@Override
	public void onInitialize () {
		DataResourceLoader.get().registerReloadListener(WeightPackReloadListener.ID, new WeightPackReloadListener());
		ServerLifecycleEvents.SERVER_STARTED.register(ServerWeightState::start);
		FabricConfigScreenHelper.registerPayloads();
		ModHooks.registerHooks();
		ModEnchantmentEffects.register();
	}
}
