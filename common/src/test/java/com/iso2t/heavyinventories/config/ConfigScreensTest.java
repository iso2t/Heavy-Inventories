package com.iso2t.heavyinventories.config;

import com.iso2t.heavyinventories.api.config.ConfigScreens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("removal")
class ConfigScreensTest {

	@AfterEach
	void clearOpener () {
		ConfigScreens.register(null);
	}

	@Test
	void absentOpenerDoesNotOpenScreen () {
		assertFalse(ConfigScreens.open());
	}

	@Test
	void registeredOpenerRunsOnceAndCanBeCleared () {
		var calls = new AtomicInteger();
		ConfigScreens.register(calls::incrementAndGet);
		assertTrue(ConfigScreens.open());
		assertEquals(1, calls.get());

		ConfigScreens.register(null);
		assertFalse(ConfigScreens.open());
		assertEquals(1, calls.get());
	}

	@Test
	void noOpRegistrationDoesNotOpenScreen () {
		ConfigScreens.register(ConfigScreenOpener.NO_OP);
		assertFalse(ConfigScreens.open());
	}
}
