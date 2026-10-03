package com.iso2t.heavyinventories.test.plugin;

import com.iso2t.heavyinventories.api.client.ClientPluginRegistration;
import com.iso2t.heavyinventories.api.client.ClientWeights;
import com.iso2t.heavyinventories.api.client.HeavyInventoriesClientPlugin;
import com.iso2t.heavyinventories.api.client.HudRegistration;
import com.iso2t.heavyinventories.api.plugin.HIPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

@HIPlugin(HIPlugin.Side.CLIENT)
public final class FixtureClientPlugin implements HeavyInventoriesClientPlugin {
	private static final Minecraft       CLIENT = Minecraft.getInstance();
	public static        ClientWeights   weights;
	public static        HudRegistration hud;

	@Override
	public Identifier id () {
		return Identifier.fromNamespaceAndPath("heavyinventories_lifecycle_test", "client");
	}

	@Override
	public void register (ClientPluginRegistration registration) {
		if (CLIENT == null || ++PluginProbe.clientRegistrations != 1) throw new IllegalStateException("Invalid client registration");
		weights = registration.weights();
		hud = registration.hud();
		FixtureHud.register(hud);
		FixtureClientNotifications.register(registration);
	}
}
