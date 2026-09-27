package com.iso2t.heavyinventories.api.files;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iso2t.heavyinventories.HeavyInventories;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReadFile {

	public static float get (ItemLike item, DataType type) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item.asItem());
		return readFromFile(id.getNamespace(), item, type);
	}

	/**
	 * Reads a specific property (weight or density) for a given item from the JSON file.
	 *
	 * @param fileName The registry namespace of the legacy weight file.
	 * @param item     The Minecraft ItemLike.
	 * @param type     The property to read.
	 * @return The stored value, or 0.1 when unavailable.
	 */
	public static float readFromFile (String fileName, ItemLike item, DataType type) {
		File file = new File(FileValidator.validate(fileName).toString());
		if (!file.exists()) {
			return 0.1f;
		}

		try (FileReader reader = new FileReader(file)) {
			var jsonElement = JsonParser.parseReader(reader);
			if (jsonElement == null || jsonElement.isJsonNull()) {
				return 0.1f;
			}

			JsonObject root = jsonElement.getAsJsonObject();

			Identifier id = BuiltInRegistries.ITEM.getKey(item.asItem());

			String field = id.getPath();

			if (root.has(field)) {
				JsonObject fieldObj = root.getAsJsonObject(field);
				String property = type.name().toLowerCase(Locale.ROOT);

				if (fieldObj.has(property)) {
					return fieldObj.get(property).getAsFloat();
				}
			}
		} catch (IOException e) {
			HeavyInventories.LOGGER.warn("Failed to read file: {}!", fileName);
		} catch (Exception e) {
			HeavyInventories.LOGGER.warn("Failed to parse JSON file: {}! Error: {}", fileName, e.getMessage());
		}

		return 0.1f;
	}
}
