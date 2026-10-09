package com.iso2t.heavyinventories.config;

import com.iso2t.easyconfig.api.ConfigPlatform;
import com.iso2t.easyconfig.api.files.FileTypes;
import com.iso2t.easyconfig.api.manager.ConfigManager;
import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.util.JsonFiles;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigFileManager {

	public static Path clientPath () {
		return ConfigPlatform.configDir().resolve("heavyinventories-client.toml");
	}

	public static Path serverPath () {
		return ConfigPlatform.configDir().resolve("heavyinventories-server.toml");
	}

	public static void loadClientConfig () {
		try {
			var settings = readClientConfig(clientPath());
			writeClientConfig(clientPath(), settings);
			settings.apply();
		} catch (IOException | IllegalArgumentException e) {
			HeavyInventories.LOGGER.error("Failed to load client config; keeping current preferences: {}", e.getMessage());
		}
	}

	public static void saveClientConfig (ClientSettings settings) throws IOException {
		writeClientConfig(clientPath(), settings);
		settings.apply();
	}

	public static ClientSettings readClientConfig (Path path) throws IOException {
		if (importLegacy(path)) return ClientSettings.parse(JsonFiles.readObject(legacyPath(path)));
		try {
			return new ConfigManager<>(ClientConfig.class, path, FileTypes.TOML).load().settings();
		} catch (IllegalStateException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public static void writeClientConfig (Path path, ClientSettings settings) throws IOException {
		readClientConfig(path);
		try {
			new ConfigManager<>(ClientConfig.class, path, FileTypes.TOML).save(new ClientConfig(settings));
		} catch (IllegalStateException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public static ServerSettings loadServerConfig () throws IOException {
		var settings = readServerConfig(serverPath());
		writeServerConfig(serverPath(), settings);
		return settings;
	}

	public static ServerSettings readServerConfig (Path path) throws IOException {
		if (importLegacy(path)) return ServerSettings.parse(JsonFiles.readObject(legacyPath(path)));
		try {
			return new ConfigManager<>(ServerConfig.class, path, FileTypes.TOML).load().settings();
		} catch (IllegalStateException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public static void writeServerConfig (Path path, ServerSettings settings) throws IOException {
		readServerConfig(path);
		try {
			new ConfigManager<>(ServerConfig.class, path, FileTypes.TOML).save(new ServerConfig(settings, 0));
		} catch (IllegalStateException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	private static boolean importLegacy (Path path) {
		return Files.notExists(path) && Files.exists(legacyPath(path));
	}

	private static Path legacyPath (Path path) {
		return path.resolveSibling(path.getFileName().toString().replaceFirst("\\.toml$", ".json"));
	}

}
