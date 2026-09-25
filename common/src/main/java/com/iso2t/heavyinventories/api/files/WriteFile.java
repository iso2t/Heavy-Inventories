package com.iso2t.heavyinventories.api.files;

import com.google.gson.JsonObject;
import com.iso2t.heavyinventories.config.ServerSettings;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/** Validated edits preserve unknown fields and report failures to their caller. */
public final class WriteFile {
    private WriteFile() {}

    public static JsonObject readWeights(Path path) throws IOException {
        var root = JsonFiles.readObject(path);
        for (var entry : root.entrySet()) {
            if (!entry.getValue().isJsonObject())
                throw new IllegalArgumentException("Invalid weight entry " + entry.getKey() + " in " + path);
            var weight = entry.getValue().getAsJsonObject().get("weight");
            if (weight != null) {
                if (!weight.isJsonPrimitive() || !weight.getAsJsonPrimitive().isNumber())
                    throw new IllegalArgumentException("Invalid weight value " + entry.getKey() + " in " + path);
                ServerSettings.validateItemWeight(weight.getAsFloat());
            }
        }
        return root;
    }

    public static JsonObject withValue(JsonObject root, String field, DataType type, float value) {
        ServerSettings.validateItemWeight(value);
        var result = root.deepCopy();
        var entry = result.has(field) ? result.getAsJsonObject(field) : new JsonObject();
        entry.addProperty(type.name().toLowerCase(Locale.ROOT), value);
        result.add(field, entry);
        return result;
    }

    public static void writeToFile(String namespace, String field, DataType type, float value) throws IOException {
        Path path = FileValidator.validate(namespace);
        JsonFiles.writeObject(path, withValue(readWeights(path), field, type, value));
    }
}
