package com.iso2t.heavyinventories.test.plugin;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.client.HudElement;
import com.iso2t.heavyinventories.api.client.HudRegistration;
import com.iso2t.heavyinventories.integration.client.ClientPlugins;
import net.minecraft.resources.Identifier;

public final class ClientPluginScenario {
	public static void verify () {
		if (PluginProbe.commonRegistrations != 1 || PluginProbe.clientRegistrations != 1) throw new AssertionError("Common/client plugins must each register once, including singleplayer");
		if (!ClientPlugins.INSTANCE.registrations().playerChanged().containsKey(Identifier.fromNamespaceAndPath("heavyinventories_lifecycle_test", "b_player"))) throw new AssertionError("Client plugin registration was not committed");
		if (FixtureClientPlugin.weights.player().isEmpty()) throw new AssertionError("Client plugin cannot read synchronized snapshot");
		try {
			FixtureClientPlugin.hud.decorate(Identifier.fromNamespaceAndPath("heavyinventories_lifecycle_test", "late"), HudElement.RING, HudRegistration.Phase.AFTER, (graphics, context, layout) -> {
			});
			throw new AssertionError("HUD registrar remained open");
		} catch (IllegalStateException expected) {
		}
		HeavyInventories.LOGGER.info("API PLUGINS CLIENT PASSED: common/client discovery, integrated-server registration once, synchronized service, frozen HUD registrar");
	}
}
