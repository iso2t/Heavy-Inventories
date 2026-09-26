package com.iso2t.heavyinventories.config;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EffectsSettingsTest {
	@TempDir
	Path directory;

	@Test
	void oldAndPartialConfigsUseDefaultsWithoutRewritingFiles () throws Exception {
		var path = directory.resolve("server.json");
		String old = "{\"startingWeight\":123.5,\"walkingMode\":\"at_ninety_percent\"}";
		Files.writeString(path, old);
		assertEquals(EffectsSettings.DEFAULT, ConfigFileManager.readServerConfig(path).effects());
		assertEquals(old, Files.readString(path));
		Files.writeString(path, "{\"effects\":{\"lava\":false,\"exhaustion\":{\"maxMultiplier\":2.25}}}");
		var settings = ConfigFileManager.readServerConfig(path).effects();
		assertFalse(settings.lava());
		assertTrue(settings.water());
		assertEquals(2.25f, settings.exhaustion().maxMultiplier());
		assertEquals(0.01f, settings.exhaustion().walkingCostPerBlock());
		assertEquals(EffectsSettings.DEFAULT.fallDamage(), settings.fallDamage());
	}

	@Test
	void allGroupsRoundTripAndNestedUnknownFieldsSurviveSaving () throws Exception {
		var path = directory.resolve("server.json");
		Files.writeString(path, "{\"ownerNote\":\"keep\",\"effects\":{\"future\":7,\"fallDamage\":{\"note\":\"keep too\"}}}");
		var effects = new EffectsSettings(false, true, new EffectsSettings.Exhaustion(false, 2.25f, 0.025f), new EffectsSettings.FallDamage(false, 83.5f, 137.25f, 3.25f), new EffectsSettings.Swimming(false, 44.5f, 97.5f, 0.125f), new EffectsSettings.Sinking(false, 12.5f, 117.5f, 4.5f), new EffectsSettings.UpwardMovement(true, 98.5f), new EffectsSettings.Knockback(false, 812.25f, 0.75f));
		var settings = new ServerSettings(456.75f, WalkingMode.AT_NINETY_PERCENT, effects);
		ConfigFileManager.writeServerConfig(path, settings);
		assertEquals(settings, ConfigFileManager.readServerConfig(path));
		var json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
		assertEquals("keep", json.get("ownerNote").getAsString());
		assertEquals(7, json.getAsJsonObject("effects").get("future").getAsInt());
		assertEquals("keep too", json.getAsJsonObject("effects").getAsJsonObject("fallDamage").get("note").getAsString());
	}

	@Test
	void invalidNestedSettingsRejectReadAndWriteWithoutAlteringFile () throws Exception {
		var path = directory.resolve("server.json");
		for (String effects : new String[] { "null", "[]", "{\"water\":1}", "{\"lava\":\"false\"}", "{\"swimming\":null}", "{\"exhaustion\":{\"enabled\":\"true\"}}", "{\"exhaustion\":{\"walkingCostPerBlock\":-1}}", "{\"exhaustion\":{\"maxMultiplier\":\"1.5\"}}", "{\"exhaustion\":{\"maxMultiplier\":0.9}}", "{\"fallDamage\":{\"startPercent\":125}}", "{\"swimming\":{\"minMultiplier\":1.1}}", "{\"sinking\":{\"fullPercent\":89}}", "{\"upwardMovement\":{\"thresholdPercent\":1e100}}", "{\"knockback\":{\"referenceWeight\":0}}", "{\"knockback\":{\"maxResistance\":1.01}}" }) {
			String json = "{\"startingWeight\":42,\"effects\":" + effects + "}";
			Files.writeString(path, json);
			assertThrows(IllegalArgumentException.class, () -> ConfigFileManager.readServerConfig(path), json);
			assertThrows(IllegalArgumentException.class, () -> ConfigFileManager.writeServerConfig(path, ServerSettings.DEFAULT), json);
			assertEquals(json, Files.readString(path));
		}
	}

	@Test
	void constructorsRejectNonfiniteAndUnorderedValuesEvenWhenDisabled () {
		for (float value : new float[] { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1 }) {
			assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.Exhaustion(false, value, 0));
			assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.FallDamage(false, 0, value, 2));
			assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.Swimming(false, 0, 100, value));
			assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.Knockback(false, value, 0.4f));
		}
		assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.Sinking(true, 100, 100, 2));
		assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.FallDamage(true, 101, 100, 2));
		assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.UpwardMovement(true, 10_001));
	}
}
