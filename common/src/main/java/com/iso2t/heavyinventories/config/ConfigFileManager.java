package com.iso2t.heavyinventories.config;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.files.JsonFiles;
import com.iso2t.heavyinventories.platform.Services;

import java.io.IOException;
import java.nio.file.Path;

public final class ConfigFileManager {
	private ConfigFileManager () {
	}

	public static void loadClientConfig () {
		try {
			ClientSettings.parse(JsonFiles.readObject(clientPath())).apply();
		} catch (IOException | IllegalArgumentException e) {
			HeavyInventories.LOGGER.error("Failed to load client config; keeping current preferences: {}", e.getMessage());
		}
	}

	public static void saveClientConfig (ClientSettings settings) throws IOException {
		// Preserve unknown fields. Refuse to replace an existing malformed document.
		var root = JsonFiles.readObject(clientPath());
		ClientSettings.parse(root);
		settings.toJson().entrySet().forEach(entry -> root.add(entry.getKey(), entry.getValue()));
		JsonFiles.writeObject(clientPath(), root);
		settings.apply();
	}

	private static Path clientPath () {
		return Services.PLATFORM.getGameDirectory().resolve("config/heavyinventories-client.json");
	}

	public static ServerSettings readServerConfig (Path path) throws IOException {
		return ServerSettings.parse(JsonFiles.readObject(path));
	}

	public static void writeServerConfig (Path path, ServerSettings settings) throws IOException {
		var root = JsonFiles.readObject(path);
		ServerSettings.parse(root);
		root.addProperty("startingWeight", settings.startingWeight());
		root.addProperty("walkingMode", settings.walkingMode().id());
		JsonFiles.writeObject(path, root);
	}

	public static void loadCommonConfig () {
	}

	public static void saveCommonConfig () {
	}
}
