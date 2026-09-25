package com.iso2t.heavyinventories.neoforge.core;

import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.neoforge.client.NeoForgeClientHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

public class HeavyInventoriesNeoForgeClient extends HeavyInventoriesNeoForgeBase {

	public HeavyInventoriesNeoForgeClient (ModContainer modContainer, IEventBus modEventBus) {
		super(modContainer, modEventBus);

		ConfigFileManager.loadClientConfig();

		NeoForgeClientHooks.register(modEventBus);
	}

	@Override
	public Level getClientLevel () {
		return Minecraft.getInstance().level;
	}

}
