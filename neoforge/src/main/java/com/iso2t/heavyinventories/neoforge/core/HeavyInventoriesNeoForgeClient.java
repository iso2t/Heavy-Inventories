package com.iso2t.heavyinventories.neoforge.core;

import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.neoforge.client.NeoForgeClientHooks;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.bus.api.IEventBus;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HeavyInventoriesNeoForgeClient {

	public static void initialize (IEventBus modEventBus) {
		ConfigFileManager.loadClientConfig();
		NeoForgeClientHooks.register(modEventBus);
	}
}
