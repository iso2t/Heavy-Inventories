package com.iso2t.heavyinventories.api.client;

import net.minecraft.resources.Identifier;

/**
 * Client-only entry point. Implementations must have a public no-argument constructor.
 */
public interface HeavyInventoriesClientPlugin {

	Identifier id ();

	/**
	 * Called once before HUD layers are registered. Do not retain the registrar.
	 */
	void register (ClientPluginRegistration registration);

}
