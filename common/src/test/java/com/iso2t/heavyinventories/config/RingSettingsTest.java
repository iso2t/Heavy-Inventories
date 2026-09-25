package com.iso2t.heavyinventories.config;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RingSettingsTest {
	private ClientSettings parse (String json) {
		return ClientSettings.parse(JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	void legacyPreferencesKeepDisabledAndDefaultToSeven () {
		var value = parse("{\"enableGuiOverlay\":false,\"normalTextColor\":123}");
		assertFalse(value.overlay());
		assertEquals(123, value.normal());
		assertEquals(7, value.ringVerticalOffset());
		assertEquals(HudMode.RING, value.hudMode());
	}

	@Test
	void acceptsWholeRangeAndPreservesDisplayMode () {
		for (int offset : new int[] { 0, 7, 8, 12, 64 }) {
			var value = parse("{\"ringVerticalOffset\":" + offset + ",\"hudMode\":\"both\"}");
			assertEquals(offset, value.ringVerticalOffset());
			assertEquals(HudMode.BOTH, value.hudMode());
			assertEquals(value, ClientSettings.parse(value.toJson()));
		}
	}

	@Test
	void invalidEditsCannotPartiallyApply () {
		var original = ClientSettings.current();
		for (String value : new String[] { "-1", "65", "7.5", "\"7\"", "null", "true", "1e99" }) {
			assertThrows(IllegalArgumentException.class, () -> parse("{\"ringVerticalOffset\":" + value + "}").apply());
			assertEquals(original, ClientSettings.current());
		}
		assertThrows(IllegalArgumentException.class, () -> parse("{\"hudMode\":\"invalid\"}"));
	}
}
