package com.iso2t.heavyinventories.neoforge.core;

import com.iso2t.heavyinventories.client.ClientConfigScreen;
import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.neoforge.client.NeoForgeClientHooks;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HeavyInventoriesNeoForgeClient {

	public static void initialize (IEventBus modEventBus, ModContainer container) {
		container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> ClientConfigScreen.create(parent));
		ConfigFileManager.loadClientConfig();
		NeoForgeClientHooks.register(modEventBus);
	}
}
