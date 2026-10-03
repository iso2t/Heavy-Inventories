package com.iso2t.heavyinventories.integration;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class Notifications {
	private static final ThreadLocal<Boolean> DISPATCHING = ThreadLocal.withInitial(() -> false);
	private final        Map<String, Long>    nextReport  = new HashMap<>();

	public static boolean sameValues (PlayerWeightSnapshot first, PlayerWeightSnapshot second) {
		if (first == second) return true;
		return first != null && second != null && first.carriedWeight().equals(second.carriedWeight()) && first.baseCapacity() == second.baseCapacity() && first.capacity() == second.capacity() && first.state() == second.state() && first.walkingMultiplier() == second.walkingMultiplier() && first.effectsApply() == second.effectsApply() && first.revision() == second.revision();
	}

	public static void checkCommit () {
		if (DISPATCHING.get()) throw new IllegalStateException("HI listeners cannot commit another update; invalidate the player for a later update");
	}

	public <T> void dispatch (String kind, Map<Identifier, T> listeners, Consumer<T> invoke) {
		checkCommit();
		DISPATCHING.set(true);
		try {
			listeners.forEach((id, listener) -> {
				try {
					invoke.accept(listener);
				} catch (RuntimeException | LinkageError error) {
					String key = kind + ":" + id;
					long now = System.nanoTime();
					var next = nextReport.get(key);
					if (next == null || now - next >= 0) {
						HeavyInventories.LOGGER.error("HI {} listener {} failed; committed state is unchanged", kind, id, error);
						nextReport.put(key, now + 60_000_000_000L);
					}
				}
			});
		} finally {
			DISPATCHING.remove();
		}
	}

	public void clear () {
		nextReport.clear();
	}
}
