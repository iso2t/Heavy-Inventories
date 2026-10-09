package com.iso2t.heavyinventories.test.plugin;

import com.iso2t.heavyinventories.api.ServerWeights;
import com.iso2t.heavyinventories.api.plugin.HIPlugin;
import com.iso2t.heavyinventories.api.plugin.HeavyInventoriesPlugin;
import com.iso2t.heavyinventories.api.plugin.PluginRegistration;
import net.minecraft.resources.Identifier;

@HIPlugin
public final class FixturePlugin implements HeavyInventoriesPlugin {

	public static PluginRegistration registrar;
	public static ServerWeights      weights;

	@Override
	public Identifier id () {
		return Identifier.fromNamespaceAndPath("heavyinventories_lifecycle_test", "common");
	}

	@Override
	public void register (PluginRegistration registration) {
		if (++PluginProbe.commonRegistrations != 1) throw new IllegalStateException("Common plugin registered twice");
		registrar = registration;
		weights = registration.weights();
		FixtureProviders.register(registration);
		FixtureNotifications.register(registration);
	}

}
