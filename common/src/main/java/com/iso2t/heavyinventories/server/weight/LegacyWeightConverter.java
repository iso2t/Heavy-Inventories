package com.iso2t.heavyinventories.server.weight;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Explicit conversion of legacy namespace files; never reads dump reports or installs/enables packs.
 */
public final class LegacyWeightConverter {
	private static final Gson JSON           = new GsonBuilder().setPrettyPrinting().create();
	private static final int  MAX_FILE_CHARS = 16 * 1024 * 1024, MAX_TOTAL_CHARS = 64 * 1024 * 1024;

	private LegacyWeightConverter () {
	}

	public record Result(Path file, int converted, int skipped) {
	}

	public static Result convert (Path input, Path output, String name, int major, int minor) throws IOException {
		if (!name.matches("[a-z0-9_-]{1,64}")) throw new IllegalArgumentException("Pack name must use 1–64 lowercase letters, digits, underscores or hyphens");
		if (major < 0 || minor < 0) throw new IllegalArgumentException("Invalid pack version");
		var target = output.toAbsolutePath().normalize().resolve(name + ".zip");
		if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new FileAlreadyExistsException(target.toString(), null, "Output pack already exists; choose a different name");
		if (!Files.isDirectory(input)) throw new IllegalArgumentException("No legacy weight directory: " + input);
		List<Path> files;
		try (var paths = Files.list(input)) {
			files = paths.filter(path -> path.getFileName().toString().endsWith(".json")).limit(1025).sorted().toList();
		}
		if (files.size() > 1024) throw new IllegalArgumentException("Too many legacy namespace files");
		var values = new TreeMap<String, Float>();
		int skipped = 0, totalChars = 0;
		for (var file : files) {
			if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("Expected a regular legacy file: " + file);
			String filename = file.getFileName().toString();
			String namespace = filename.substring(0, filename.length() - 5);
			String text = readBounded(file);
			totalChars += text.length();
			if (totalChars > MAX_TOTAL_CHARS) throw new IllegalArgumentException("Legacy input exceeds total character limit");
			try (var json = new JsonReader(new StringReader(text))) {
				json.setStrictness(Strictness.STRICT);
				json.beginObject();
				var items = new HashSet<String>();
				while (json.hasNext()) {
					String item = json.nextName();
					if (!items.add(item)) throw new IllegalArgumentException("Duplicate item: " + item);
					var id = Identifier.fromNamespaceAndPath(namespace, item);
					String resource = "data/" + id.getNamespace() + "/heavyinventories/weights/" + id.getPath() + ".json";
					// Identifier permits dot segments; archive paths must not.
					if (Arrays.stream((namespace + "/" + item).split("/", -1)).anyMatch(p -> p.isEmpty() || p.equals(".") || p.equals(".."))) throw new IllegalArgumentException("Unsafe item path: " + id);
					json.beginObject();
					var fields = new HashSet<String>();
					Float weight = null;
					while (json.hasNext()) {
						String field = json.nextName();
						if (!fields.add(field)) throw new IllegalArgumentException("Duplicate field for " + id + ": " + field);
						if (field.equals("weight")) {
							if (json.peek() != JsonToken.NUMBER) throw new IllegalArgumentException("Expected numeric weight for " + id);
							var definition = WeightDefinition.parse(new StringReader("{\"weight\":" + json.nextString() + "}"));
							weight = ((WeightDefinition.Fixed) definition).weight();
						} else json.skipValue(); // Density and other legacy metadata are not item weights.
					}
					json.endObject();
					if (weight == null) skipped++;
					else if (values.putIfAbsent(resource, weight) != null) throw new IllegalArgumentException("Duplicate resource: " + resource);
					if (values.size() + skipped > WeightPackData.MAX_DEFINITIONS) throw new IllegalArgumentException("Too many legacy entries");
				}
				json.endObject();
				if (json.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Trailing input");
			} catch (IllegalArgumentException | IllegalStateException | IOException e) {
				throw new IllegalArgumentException("Invalid legacy file " + file + ": " + e.getMessage(), e);
			}
		}
		if (values.isEmpty()) throw new IllegalArgumentException("No explicit legacy weights to convert");
		// Every input has now passed validation. Publish only a completed ZIP, without replacing any output.
		Files.createDirectories(target.getParent());
		var temporary = Files.createTempFile(target.getParent(), "weights-", ".tmp");
		try {
			try (var zip = new ZipOutputStream(Files.newOutputStream(temporary), StandardCharsets.UTF_8)) {
				var version = new JsonArray();
				version.add(major);
				version.add(minor);
				var pack = new JsonObject();
				pack.addProperty("description", "Heavy Inventories converted weight overrides");
				pack.add("min_format", version);
				pack.add("max_format", version.deepCopy());
				var metadata = new JsonObject();
				metadata.add("pack", pack);
				write(zip, "pack.mcmeta", metadata);
				for (var entry : values.entrySet()) {
					var definition = new JsonObject();
					definition.addProperty("weight", entry.getValue());
					write(zip, entry.getKey(), definition);
				}
			}
			Files.move(temporary, target); // Deliberately no REPLACE_EXISTING or ATOMIC_MOVE overwrite semantics.
		} finally {
			Files.deleteIfExists(temporary);
		}
		return new Result(target, values.size(), skipped);
	}

	private static String readBounded (Path file) throws IOException {
		var text = new StringBuilder();
		try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			var buffer = new char[8192];
			int count;
			while ((count = reader.read(buffer)) != -1) {
				if (text.length() + count > MAX_FILE_CHARS) throw new IllegalArgumentException("Legacy file exceeds character limit: " + file);
				text.append(buffer, 0, count);
			}
		}
		return text.toString();
	}

	private static void write (ZipOutputStream zip, String name, JsonObject json) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
		zip.write(JSON.toJson(json).getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}
}
