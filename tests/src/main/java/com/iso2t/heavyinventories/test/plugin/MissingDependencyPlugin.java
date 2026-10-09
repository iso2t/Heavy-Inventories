package com.iso2t.heavyinventories.test.plugin;

import com.iso2t.heavyinventories.api.plugin.HIPlugin;
import com.iso2t.heavyinventories.api.plugin.HeavyInventoriesPlugin;
import com.iso2t.heavyinventories.api.plugin.PluginRegistration;
import net.minecraft.resources.Identifier;

@HIPlugin(requires = "hi_missing_fixture_dependency")
public final class MissingDependencyPlugin implements HeavyInventoriesPlugin {

	static {
		if (true) throw new AssertionError("Missing dependency plugin must not initialize");
	}

	@Override
	public Identifier id () {
		throw new AssertionError("Must not load");
	}

	@Override
	public void register (PluginRegistration registration) {
		throw new AssertionError("Must not register");
	}

}
