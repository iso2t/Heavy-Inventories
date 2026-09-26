package com.iso2t.heavyinventories.config;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerSettingsTest {
	@TempDir
	Path directory;

	@Test
	void walkingModeRoundTripsAndOlderConfigsUseTheDefault () throws Exception {
		var file = directory.resolve("server.json");
		var settings = new ServerSettings(12.375f, WalkingMode.AT_NINETY_PERCENT);
		ConfigFileManager.writeServerConfig(file, settings);
		assertEquals(settings, ConfigFileManager.readServerConfig(file));
		Files.writeString(file, "{\"startingWeight\":23.5}");
		assertEquals(ServerSettings.DEFAULT.walkingMode(), ConfigFileManager.readServerConfig(file).walkingMode());
		Files.writeString(file, "{\"walkingMode\":\"at_ninety_percent\"}");
		assertEquals(new ServerSettings(1000, WalkingMode.AT_NINETY_PERCENT), ConfigFileManager.readServerConfig(file));
	}

	@Test
	void invalidWalkingModesAreRejectedWithoutChangingTheFile () throws Exception {
		var file = directory.resolve("server.json");
		for (String value : new String[] { "null", "true", "17", "\"unknown\"" }) {
			String json = "{\"walkingMode\":" + value + "}";
			Files.writeString(file, json);
			assertThrows(IllegalArgumentException.class, () -> ConfigFileManager.readServerConfig(file));
			assertEquals(json, Files.readString(file));
		}
	}

	@Test
	void decimalsSurviveFileRoundTrip () throws Exception {
		var file = directory.resolve("config/server.json");
		assertEquals(ServerSettings.DEFAULT, ConfigFileManager.readServerConfig(file));
		ConfigFileManager.writeServerConfig(file, new ServerSettings(12.375f));
		assertEquals(12.375f, ConfigFileManager.readServerConfig(file).startingWeight());
	}

	@Test
	void rejectsInvalidCapacityAndWeight () {
		for (float value : new float[] { 0, -1, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.MAX_VALUE }) {
			assertThrows(IllegalArgumentException.class, () -> new ServerSettings(value));
		}
		assertEquals(0, ServerSettings.validateItemWeight(0));
		for (float value : new float[] { -1, Float.NaN, Float.POSITIVE_INFINITY, Float.MAX_VALUE }) {
			assertThrows(IllegalArgumentException.class, () -> ServerSettings.validateItemWeight(value));
		}
		assertEquals(Float.MIN_VALUE, new ServerSettings(Float.MIN_VALUE).startingWeight());
		assertEquals(ServerSettings.MAX_VALUE, new ServerSettings(ServerSettings.MAX_VALUE).startingWeight());
	}

	@Test
	void malformedFilesAreRejectedAndPreserved () throws Exception {
		var file = directory.resolve("server.json");
		for (String value : new String[] { "null", "[]", "{", "{\"startingWeight\":null}", "{\"startingWeight\":\"12.5\"}", "{\"startingWeight\":true}", "{\"startingWeight\":1e100}" }) {
			Files.writeString(file, value);
			assertThrows(IllegalArgumentException.class, () -> ConfigFileManager.readServerConfig(file));
			assertEquals(value, Files.readString(file));
		}
		assertEquals(ServerSettings.DEFAULT, ServerSettings.parse(JsonParser.parseString("{}").getAsJsonObject()));
	}

	@Test
	void directoryAtConfigPathIsAnIoFailureAndIsPreserved () throws Exception {
		var target = Files.createDirectory(directory.resolve("server.json"));
		Files.writeString(target.resolve("keep.txt"), "original");
		assertThrows(java.io.IOException.class, () -> ConfigFileManager.writeServerConfig(target, new ServerSettings(20)));
		assertEquals("original", Files.readString(target.resolve("keep.txt")));
	}

	@Test
	void failedReplacementPreservesTargetAndCleansTemporaryFile () throws Exception {
		var target = Files.createDirectory(directory.resolve("server.json"));
		Files.writeString(target.resolve("keep.txt"), "original");
		assertThrows(java.io.IOException.class, () -> com.iso2t.heavyinventories.api.files.JsonFiles.writeObject(target, JsonParser.parseString("{\"startingWeight\":20}").getAsJsonObject()));
		assertEquals("original", Files.readString(target.resolve("keep.txt")));
		try (var children = Files.list(directory)) {
			assertEquals(1, children.count());
		}
	}
}
