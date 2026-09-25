package com.iso2t.heavyinventories.api.files;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Strict reads and complete-file replacement. A failed read never becomes an empty document.
 */
public final class JsonFiles {
	private static final Gson GSON = new GsonBuilder().setStrictness(Strictness.STRICT).setPrettyPrinting().create();

	private JsonFiles () {
	}

	public static JsonObject readObject (Path path) throws IOException {
		if (Files.notExists(path)) return new JsonObject();
		// Keep filesystem failures outside Gson, which wraps reader IOExceptions differently by platform.
		var json = Files.readString(path, StandardCharsets.UTF_8);
		try {
			var root = GSON.fromJson(json, JsonObject.class);
			if (root == null) throw new IllegalArgumentException("Expected a JSON object in " + path);
			return root;
		} catch (com.google.gson.JsonParseException | IllegalStateException e) {
			throw new IllegalArgumentException("Invalid JSON in " + path + ": " + e.getMessage(), e);
		}
	}

	public static void writeObject (Path path, JsonObject root) throws IOException {
		Path target = path.toAbsolutePath().normalize();
		Files.createDirectories(target.getParent());
		Path temp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
		try {
			try (var writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
			try {
				Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temp);
		}
	}
}
