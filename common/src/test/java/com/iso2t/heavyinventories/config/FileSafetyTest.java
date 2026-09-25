package com.iso2t.heavyinventories.config;

import com.iso2t.heavyinventories.api.files.DataType;
import com.iso2t.heavyinventories.api.files.FileValidator;
import com.iso2t.heavyinventories.api.files.JsonFiles;
import com.iso2t.heavyinventories.api.files.WriteFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileSafetyTest {
	@TempDir
	Path directory;

	@Test
	void weightEditPreservesOtherEntriesAndFields () throws Exception {
		var file = directory.resolve("minecraft.json");
		Files.writeString(file, "{\"stone\":{\"weight\":2,\"density\":7},\"dirt\":{\"weight\":3}}");
		var changed = WriteFile.withValue(WriteFile.readWeights(file), "stone", DataType.WEIGHT, 4.25f);
		JsonFiles.writeObject(file, changed);
		var result = WriteFile.readWeights(file);
		assertEquals(4.25f, result.getAsJsonObject("stone").get("weight").getAsFloat());
		assertEquals(7, result.getAsJsonObject("stone").get("density").getAsInt());
		assertEquals(3, result.getAsJsonObject("dirt").get("weight").getAsInt());
	}

	@Test
	void malformedOrInvalidWeightFilesAreNeverReplaced () throws Exception {
		var file = directory.resolve("minecraft.json");
		for (var data : new String[] { "", "{", "null", "[]", "{stone:{weight:2}}", "{} trailing", "{\"removed_item\":{\"weight\":-1}}" }) {
			Files.writeString(file, data);
			assertThrows(IllegalArgumentException.class, () -> {
				var root = WriteFile.withValue(WriteFile.readWeights(file), "stone", DataType.WEIGHT, 5);
				JsonFiles.writeObject(file, root);
			});
			assertEquals(data, Files.readString(file));
		}
	}

	@Test
	void invalidServerFileCannotBeOverwrittenByAnOperatorEdit () throws Exception {
		var file = directory.resolve("server.json");
		Files.writeString(file, "{\"startingWeight\":0}");
		assertThrows(IllegalArgumentException.class, () -> ConfigFileManager.writeServerConfig(file, new ServerSettings(1000)));
		assertEquals("{\"startingWeight\":0}", Files.readString(file));
	}

	@Test
	void failedAtomicReplacementReportsFailureAndCleansTemporaryFiles () throws Exception {
		var target = Files.createDirectories(directory.resolve("target.json"));
		Files.writeString(target.resolve("keep"), "original");
		assertThrows(java.io.IOException.class, () -> JsonFiles.writeObject(target, new com.google.gson.JsonObject()));
		assertEquals("original", Files.readString(target.resolve("keep")));
		try (var files = Files.list(directory)) {
			assertEquals(1, files.count());
		}
	}

	@Test
	void weightFileNamesCannotEscapeTheNamespaceDirectory () {
		for (String value : new String[] { "../test", "weights/../../test", "C:\\test", "a/b", "", "UpperCase" })
			assertThrows(IllegalArgumentException.class, () -> FileValidator.validate(value));
	}
}
