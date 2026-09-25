package com.iso2t.heavyinventories.platform;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.platform.services.IConfigScreenHelper;
import com.iso2t.heavyinventories.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public class Services {

	public static final IPlatformHelper     PLATFORM      = load(IPlatformHelper.class);
	public static final IConfigScreenHelper CONFIG_SCREEN = load(IConfigScreenHelper.class);

	public static <T> T load (Class<T> clazz) {

		final T loadedService = ServiceLoader.load(clazz).findFirst().orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
		HeavyInventories.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
		return loadedService;
	}
}
