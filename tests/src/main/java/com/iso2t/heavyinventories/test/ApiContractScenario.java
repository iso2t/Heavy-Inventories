package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.EncumbranceState;
import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.WeightResult;

/**
 * Common API linkage on a real dedicated server, using classes from the production jar.
 */
public final class ApiContractScenario {

	public static void verify () {
		for (String name : new String[] { "ServerWeights", "WeightSource", "plugin.HeavyInventoriesPlugin", "plugin.PluginRegistration", "plugin.HIPlugin", "provider.InventoryProvider", "provider.ContainerContentsProvider", "provider.CapacityProvider" }) {
			try {
				var type = Class.forName("com.iso2t.heavyinventories.api." + name);
				type.getDeclaredMethods();
				type.getDeclaredConstructors();
			} catch (ReflectiveOperationException | LinkageError e) {
				throw new AssertionError("Common API failed to load on dedicated server: " + name, e);
			}
		}
		var snapshot = new PlayerWeightSnapshot(WeightResult.complete(1250), 1000, 1000, EncumbranceState.OVERLOADED, 0, true, 1, 20);
		if (snapshot.loadRatio().orElseThrow() != 1.25 || WeightResult.unavailable().pounds().isPresent()) {
			throw new AssertionError("API weight result contract failed");
		}
		HeavyInventories.LOGGER.info("API CONTRACTS PASSED: dedicated-server linkage, weight availability, unclamped load ratio");
	}

}
