package com.iso2t.heavyinventories.test.plugin;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.WeightResult;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

public final class PluginScenario {
	public static void server (MinecraftServer server) {
		require(PluginProbe.commonRegistrations == 1, "Common plugin must register exactly once");
		require(PluginProbe.clientRegistrations == 0, "Client plugin loaded on dedicated server");
		require(FixturePlugin.weights.item(server, Identifier.withDefaultNamespace("stone")).pounds().orElseThrow() > 0, "Plugin must read loaded weights");
		require(FixturePlugin.weights.item(server, Identifier.withDefaultNamespace("hi_unknown_fixture")).status() == WeightResult.Status.UNKNOWN_ITEM, "Unknown item status");
		try {
			FixturePlugin.registrar.capacity(Identifier.fromNamespaceAndPath("heavyinventories_lifecycle_test", "late"), player -> 1);
			throw new AssertionError("Registrar remained open");
		} catch (IllegalStateException expected) {
		}
		HeavyInventories.LOGGER.info("API PLUGINS SERVER PASSED: discovery, once-only registration, client isolation, missing dependency, frozen registrar, query service");
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
