package com.iso2t.heavyinventories.config;

import com.iso2t.heavyinventories.api.files.JsonFiles;
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
	void malformedJsonCannotBeReadAsAnEmptyDocument () throws Exception {
		var file = directory.resolve("config.json");
		for (var data : new String[] { "", "{", "null", "[]", "{startingWeight:2}", "{} trailing" }) {
			Files.writeString(file, data);
			assertThrows(IllegalArgumentException.class, () -> JsonFiles.readObject(file));
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

}
