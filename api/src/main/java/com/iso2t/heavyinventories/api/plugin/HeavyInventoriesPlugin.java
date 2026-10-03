package com.iso2t.heavyinventories.api.plugin;

import net.minecraft.resources.Identifier;

/**
 * Common integration entry point. Implementations must have a public no-argument constructor.
 */
public interface HeavyInventoriesPlugin {
	/**
	 * Stable namespaced ID owned by the integrating mod.
	 */
	Identifier id ();

	/**
	 * Called once per game process, before worlds exist. Do not retain the registrar.
	 */
	void register (PluginRegistration registration);
}
