package com.iso2t.heavyinventories.config;

import com.iso2t.easyconfig.api.files.Toml;
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
		var path = directory.resolve("server.toml");
		String old = "startingweight = 123.5\nwalkingmode = \"AT_NINETY_PERCENT\"";
		Files.writeString(path, old);
		assertEquals(EffectsSettings.DEFAULT, ConfigFileManager.readServerConfig(path).effects());
		assertEquals(old, Files.readString(path));
		Files.writeString(path, "[effects]\nlava = false\n[effects.exhaustion]\nmaxmultiplier = 2.25");
		var settings = ConfigFileManager.readServerConfig(path).effects();
		assertFalse(settings.lava());
		assertTrue(settings.water());
		assertEquals(2.25f, settings.exhaustion().maxMultiplier());
		assertEquals(0.01f, settings.exhaustion().walkingCostPerBlock());
		assertEquals(EffectsSettings.DEFAULT.fallDamage(), settings.fallDamage());
		assertEquals(EffectsSettings.DEFAULT.elytra(), settings.elytra());
	}

	@Test
	void allGroupsRoundTripAndNestedUnknownFieldsSurviveSaving () throws Exception {
		var path = directory.resolve("server.toml");
		Files.writeString(path, "ownerNote = \"keep\"\n[effects]\nfuture = 7\n[effects.falldamage]\nnote = \"keep too\"");
		var effects = new EffectsSettings(false, true, new EffectsSettings.Exhaustion(false, 2.25f, 0.025f), new EffectsSettings.FallDamage(false, 83.5f, 137.25f, 3.25f), new EffectsSettings.Swimming(false, 44.5f, 97.5f, 0.125f), new EffectsSettings.Sinking(false, 12.5f, 117.5f, 4.5f), new EffectsSettings.UpwardMovement(true, 98.5f), new EffectsSettings.Knockback(false, 812.25f, 0.75f), new EffectsSettings.Elytra(true, 750, 0.2f, 0.4f));
		var settings = new ServerSettings(456.75f, WalkingMode.AT_NINETY_PERCENT, effects);
		ConfigFileManager.writeServerConfig(path, settings);
		assertEquals(settings, ConfigFileManager.readServerConfig(path));
		var toml = new Toml().read(path);
		assertEquals("keep", toml.get("ownerNote").rawValue());
		assertEquals(7, ((Number) toml.get("effects").get("future").rawValue()).intValue());
		assertEquals("keep too", toml.get("effects").get("falldamage").get("note").rawValue());
	}

	@Test
	void invalidNestedSettingsRejectReadAndWriteWithoutAlteringFile () throws Exception {
		var path = directory.resolve("server.toml");
		for (String toml : new String[] { "effects = 1", "[effects]\nwater = 1", "[effects]\nlava = \"false\"", "[effects]\nswimming = false", "[effects.exhaustion]\nenabled = \"true\"", "[effects.exhaustion]\nwalkingcostperblock = -1", "[effects.exhaustion]\nmaxmultiplier = \"1.5\"", "[effects.exhaustion]\nmaxmultiplier = 0.9", "[effects.falldamage]\nstartpercent = 125", "[effects.swimming]\nminmultiplier = 1.1", "[effects.sinking]\nfullpercent = 89", "[effects.upwardmovement]\nthresholdpercent = 1e100", "[effects.knockback]\nreferenceweight = 0", "[effects.elytra]\nreferenceweight = 0", "[effects.elytra]\nmaxliftreduction = -1", "[effects.elytra]\nmaxrocketreduction = 1.01", "[effects.elytra]\nenabled = \"true\"", "[effects.knockback]\nmaxresistance = 1.01" }) {
			Files.writeString(path, toml);
			assertThrows(IllegalArgumentException.class, () -> ConfigFileManager.readServerConfig(path), toml);
			assertThrows(IllegalArgumentException.class, () -> ConfigFileManager.writeServerConfig(path, ServerSettings.DEFAULT), toml);
			assertEquals(toml, Files.readString(path));
		}
	}

	@Test
	void constructorsRejectNonfiniteAndUnorderedValuesEvenWhenDisabled () {
		for (float value : new float[] { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1 }) {
			assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.Exhaustion(false, value, 0));
			assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.FallDamage(false, 0, value, 2));
			assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.Swimming(false, 0, 100, value));
			assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.Knockback(false, value, 0.4f));
			assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.Elytra(false, value, .15f, .25f));
			assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.Elytra(false, 1000, value, .25f));
			assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.Elytra(false, 1000, .15f, value));
		}
		assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.Sinking(true, 100, 100, 2));
		assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.FallDamage(true, 101, 100, 2));
		assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.UpwardMovement(true, 10_001));
		assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.Elytra(true, 0, .15f, .25f));
		assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.Elytra(true, 1000, 1.01f, .25f));
		assertThrows(IllegalArgumentException.class, () -> new EffectsSettings.Elytra(true, 1000, .15f, 1.01f));
	}
}
