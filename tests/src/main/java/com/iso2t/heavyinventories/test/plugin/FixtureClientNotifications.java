package com.iso2t.heavyinventories.test.plugin;

import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.client.ClientPluginRegistration;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class FixtureClientNotifications {
	public static int changes, failures, unavailable, ready;
	public static  Optional<PlayerWeightSnapshot> last = Optional.empty();
	private static LocalPlayer                    entity;

	public static void register (ClientPluginRegistration registration) {
		registration.onPlayerChanged(Identifier.fromNamespaceAndPath("heavyinventories_lifecycle_test", "a_failed_player"), snapshot -> {
			failures++;
			throw new IllegalStateException("Expected client listener failure");
		});
		registration.onPlayerChanged(Identifier.fromNamespaceAndPath("heavyinventories_lifecycle_test", "b_player"), snapshot -> {
			var client = Minecraft.getInstance();
			require(client.isSameThread(), "Client listener ran off-thread");
			require(FixtureClientPlugin.weights.player().equals(snapshot), "Client listener received incoherent state");
			if (snapshot.isEmpty()) {
				require(last.isPresent(), "Duplicate empty notification");
				unavailable++;
				entity = null;
				LogUtils.getLogger().info("API NOTIFICATIONS CLIENT UNAVAILABLE PASSED: cleared published state");
			} else {
				if (last.isEmpty()) ready++;
				else if (entity == client.player) require(!same(last.orElseThrow(), snapshot.orElseThrow()), "Unchanged client values emitted an event");
				entity = client.player;
			}
			last = snapshot;
			changes++;
			require(failures == changes, "A failed listener blocked or duplicated the healthy listener: failures=" + failures + ", changes=" + changes);
		});
	}

	public static boolean current () {
		var current = FixtureClientPlugin.weights.player();
		return entity == Minecraft.getInstance().player && current.isPresent() && last.isPresent() && same(current.orElseThrow(), last.orElseThrow());
	}

	public static void verify () {
		require(changes > 0 && ready > 0 && failures == changes && current(), "Missing current client notification");
		LogUtils.getLogger().info("API NOTIFICATIONS CLIENT PASSED: initial-ready, coherent synchronized values, unchanged-tick suppression and listener isolation");
	}

	private static boolean same (PlayerWeightSnapshot first, PlayerWeightSnapshot next) {
		return first.equals(new PlayerWeightSnapshot(next.carriedWeight(), next.baseCapacity(), next.capacity(), next.state(), next.walkingMultiplier(), next.effectsApply(), next.revision(), first.tick()));
	}

	private static void require (boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
}
