package com.iso2t.heavyinventories.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTypeTest {

	@Test
	void packetNamesResolveToTheExpectedScreen () {
		assertEquals(ConfigScreens.SettingsType.CLIENT, ConfigScreens.fromString("client").orElseThrow());
		assertEquals(ConfigScreens.SettingsType.SERVER, ConfigScreens.fromString("server").orElseThrow());
		assertEquals(ConfigScreens.SettingsType.COMMON, ConfigScreens.fromString("common").orElseThrow());
	}

	@Test
	void unsupportedPacketNamesDoNotSelectAScreen () {
		for (String type : new String[] { null, "", "unknown", "CLIENT", " server" }) {
			assertTrue(ConfigScreens.fromString(type).isEmpty());
		}
	}

}
