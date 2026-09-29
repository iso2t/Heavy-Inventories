package com.iso2t.heavyinventories.neoforge.core;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.neoforge.enchantments.ModEnchantmentEffects;
import com.iso2t.heavyinventories.neoforge.hooks.ModHooks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(HeavyInventories.MOD_ID)
public final class HeavyInventoriesNeoForgeBootstrap {

	public HeavyInventoriesNeoForgeBootstrap (IEventBus modEventBus, ModContainer container) {
		NeoForge.EVENT_BUS.addListener(ModHooks::hookReloadListeners);
		NeoForge.EVENT_BUS.addListener(ModHooks::hookServerStart);
		NeoForge.EVENT_BUS.addListener(ModHooks::hookServerTick);
		NeoForge.EVENT_BUS.addListener(ModHooks::hookCommands);
		ModEnchantmentEffects.register(modEventBus);
		if (FMLEnvironment.getDist() == Dist.CLIENT) HeavyInventoriesNeoForgeClient.initialize(modEventBus, container);
	}
}
