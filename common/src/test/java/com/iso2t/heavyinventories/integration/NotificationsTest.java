package com.iso2t.heavyinventories.integration;

import com.iso2t.heavyinventories.api.EncumbranceState;
import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.WeightResult;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationsTest {
	private static PlayerWeightSnapshot snapshot (double weight, long revision, long tick) {
		return new PlayerWeightSnapshot(WeightResult.complete(weight), 1000, 1000, EncumbranceState.NORMAL, 1, true, revision, tick);
	}

	@Test
	void timestampsDoNotCountAsChangesButRevisionsAndValuesDo () {
		assertTrue(Notifications.sameValues(snapshot(1, 2, 3), snapshot(1, 2, 4)));
		assertFalse(Notifications.sameValues(snapshot(1, 2, 3), snapshot(2, 2, 3)));
		assertFalse(Notifications.sameValues(snapshot(1, 2, 3), snapshot(1, 3, 3)));
		assertFalse(Notifications.sameValues(null, snapshot(1, 2, 3)));
		assertTrue(Notifications.sameValues(null, null));
		var incomplete = new PlayerWeightSnapshot(WeightResult.incomplete(), 1000, 1000, EncumbranceState.OVERLOADED, 0, true, 2, 3);
		assertFalse(Notifications.sameValues(snapshot(1, 2, 3), incomplete));
	}

	@Test
	void failedListenerDoesNotStopLaterListenersAndReentryGuardClears () {
		var calls = new ArrayList<String>();
		var listeners = new LinkedHashMap<Identifier, Runnable>();
		listeners.put(Identifier.parse("test:bad"), () -> {
			calls.add("bad");
			Notifications.checkCommit();
		});
		listeners.put(Identifier.parse("test:good"), () -> calls.add("good"));
		var notifications = new Notifications();
		notifications.dispatch("test", listeners, Runnable::run);
		notifications.dispatch("test", listeners, Runnable::run);
		assertEquals(java.util.List.of("bad", "good", "bad", "good"), calls);
		assertDoesNotThrow(Notifications::checkCommit);
		notifications.clear();
		assertDoesNotThrow(() -> notifications.dispatch("test", Map.of(), Runnable::run));
	}
}
