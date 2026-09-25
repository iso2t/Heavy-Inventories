package com.iso2t.heavyinventories.server.weight;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.iso2t.heavyinventories.config.ServerSettings;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;

/**
 * One datapack item definition. Inference is distinct from an explicit zero.
 */
public sealed interface WeightDefinition {

	int MAX_DOCUMENT_CHARS = 4096;

	record Fixed(float weight) implements WeightDefinition {
		public Fixed {
			ServerSettings.validateItemWeight(weight);
		}
	}

	enum Infer implements WeightDefinition {
		INSTANCE
	}

	static WeightDefinition parse (Reader source) throws IOException {
		// Bound allocation and numeric parsing before handing input to Gson/BigDecimal.
		var text = new StringBuilder();
		var buffer = new char[512];
		int count;
		while ((count = source.read(buffer)) != -1) {
			if (text.length() + count > MAX_DOCUMENT_CHARS) throw new IllegalArgumentException("Weight definition exceeds " + MAX_DOCUMENT_CHARS + " characters");
			text.append(buffer, 0, count);
		}
		try (var json = new JsonReader(new StringReader(text.toString()))) {
			json.setStrictness(Strictness.STRICT);
			if (json.peek() != JsonToken.BEGIN_OBJECT) throw invalid();
			json.beginObject();
			if (!json.hasNext()) throw invalid();
			var field = json.nextName();
			WeightDefinition definition;
			switch (field) {
				case "weight" -> {
					if (json.peek() != JsonToken.NUMBER) throw invalid();
					var decimal = new BigDecimal(json.nextString());
					if (decimal.signum() < 0 || decimal.compareTo(BigDecimal.valueOf((long) ServerSettings.MAX_VALUE)) > 0) throw new IllegalArgumentException("weight must be between 0 and 1000000000 pounds");
					float value = decimal.floatValue();
					if (decimal.signum() > 0 && value == 0) throw new IllegalArgumentException("Positive weight is too small to represent; use explicit zero if intended");
					definition = new Fixed(value);
				}
				case "infer" -> {
					if (json.peek() != JsonToken.BOOLEAN || !json.nextBoolean()) throw invalid();
					definition = Infer.INSTANCE;
				}
				default -> throw new IllegalArgumentException("Unknown weight-definition field: " + field);
			}
			if (json.hasNext()) {
				var extra = json.nextName();
				throw new IllegalArgumentException(extra.equals(field) ? "Duplicate weight-definition field: " + extra : "Expected exactly one field; unexpected field: " + extra);
			}
			json.endObject();
			if (json.peek() != JsonToken.END_DOCUMENT) throw invalid();
			return definition;
		}
	}

	private static IllegalArgumentException invalid () {
		return new IllegalArgumentException("Expected exactly {\"weight\": <number>} or {\"infer\": true}");
	}
}
