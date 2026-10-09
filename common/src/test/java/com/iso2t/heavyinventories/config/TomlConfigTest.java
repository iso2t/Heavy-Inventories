package com.iso2t.heavyinventories.config;

import com.iso2t.easyconfig.api.ConfigPlatform;
import com.iso2t.heavyinventories.util.MeasuringSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TomlConfigTest {

	@TempDir
	Path directory;

	@Test
	void startupCreatesCommentedTomlWithAllDefaults () throws Exception {
		var previous = ConfigPlatform.configDir();
		var client = ClientSettings.current();
		try {
			ConfigPlatform.configure(directory, modId -> {
			});
			ConfigFileManager.loadClientConfig();
			assertEquals(ClientSettings.DEFAULT, ClientSettings.current());
			assertEquals(ServerSettings.DEFAULT, ConfigFileManager.loadServerConfig());
			String clientFile = Files.readString(directory.resolve("heavyinventories-client.toml"));
			String serverFile = Files.readString(directory.resolve("heavyinventories-server.toml"));
			assertTrue(clientFile.contains("# Display units") && clientFile.contains("ringverticaloffset = 7"));
			assertTrue(serverFile.contains("[effects.elytra]") && serverFile.contains("startingweight = 1000.0"));
			assertFalse(serverFile.contains("revision"));
		} finally {
			ConfigPlatform.configure(previous, modId -> {
			});
			client.apply();
		}
	}

	@Test
	void clientColorsEnumsAndDecimalsRoundTripThroughEasyConfig () throws Exception {
		var path = directory.resolve("client.toml");
		var settings = new ClientSettings(MeasuringSystem.KGS, false, 0x123456, 0xFEDCBA, 0x010203, HudMode.BOTH, 12);
		ConfigFileManager.writeClientConfig(path, settings);
		assertEquals(settings, ConfigFileManager.readClientConfig(path));
		assertTrue(Files.readString(path).contains("measure = \"KGS\""));
		Files.writeString(path, "ringverticaloffset = 12.5");
		assertThrows(IllegalArgumentException.class, () -> ConfigFileManager.writeClientConfig(path, ClientSettings.DEFAULT));
		assertEquals("ringverticaloffset = 12.5", Files.readString(path));
	}

	@Test
	void hudPreferencesRoundTripAndRejectInvalidIdentifiers () throws Exception {
		var path = directory.resolve("hud.toml");
		var settings = new ClientSettings(MeasuringSystem.LBS, true, 1, 2, 3, HudMode.BOTH, 7, "another_mod:ring", "heavyinventories:default");
		ConfigFileManager.writeClientConfig(path, settings);
		assertEquals(settings, ConfigFileManager.readClientConfig(path));
		Files.writeString(path, "ringowner = \"Not an identifier\"");
		assertThrows(IllegalArgumentException.class, () -> ConfigFileManager.readClientConfig(path));
		Files.writeString(path, "ringverticaloffset = 12");
		assertEquals("", ConfigFileManager.readClientConfig(path).ringOwner());
		assertEquals("", ConfigFileManager.readClientConfig(path).numbersOwner());
	}

	@Test
	void jsonIsImportedOnceAndPreservedAsBackup () throws Exception {
		var legacyServer = directory.resolve("server.json");
		var legacyClient = directory.resolve("client.json");
		var server = new ServerSettings(432.5f, WalkingMode.AT_NINETY_PERCENT);
		var client = new ClientSettings(MeasuringSystem.KGS, false, 1, 2, 3, HudMode.BOTH, 12);
		Files.writeString(legacyServer, server.toJson().toString());
		Files.writeString(legacyClient, client.toJson().toString());
		var serverPath = directory.resolve("server.toml");
		var clientPath = directory.resolve("client.toml");
		assertEquals(server, ConfigFileManager.readServerConfig(serverPath));
		assertEquals(client, ConfigFileManager.readClientConfig(clientPath));
		ConfigFileManager.writeServerConfig(serverPath, server);
		ConfigFileManager.writeClientConfig(clientPath, client);
		assertEquals(server.toJson().toString(), Files.readString(legacyServer));
		assertEquals(client.toJson().toString(), Files.readString(legacyClient));
		Files.writeString(legacyServer, "bad legacy file");
		Files.writeString(legacyClient, "bad legacy file");
		assertEquals(server, ConfigFileManager.readServerConfig(serverPath));
		assertEquals(client, ConfigFileManager.readClientConfig(clientPath));
	}

	@Test
	void invalidLegacyFileCannotGenerateReplacementToml () throws Exception {
		var legacy = directory.resolve("server.json");
		Files.writeString(legacy, "{\"startingWeight\":0}");
		var toml = directory.resolve("server.toml");
		assertThrows(IllegalArgumentException.class, () -> ConfigFileManager.writeServerConfig(toml, ServerSettings.DEFAULT));
		assertFalse(Files.exists(toml));
		assertEquals("{\"startingWeight\":0}", Files.readString(legacy));
	}

}
