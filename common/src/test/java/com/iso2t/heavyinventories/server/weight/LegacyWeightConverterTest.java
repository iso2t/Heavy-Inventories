package com.iso2t.heavyinventories.server.weight;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class LegacyWeightConverterTest {
    @TempDir Path directory;
    private Path input() throws Exception { return Files.createDirectories(directory.resolve("weights")); }
    private Path output() { return directory.resolve("packs"); }
    private Path file(String namespace, String json) throws Exception {
        return Files.writeString(input().resolve(namespace + ".json"), json);
    }
    private LegacyWeightConverter.Result convert() throws Exception {
        return LegacyWeightConverter.convert(input(), output(), "test-pack", 101, 1);
    }
    @Test void zipPreservesFractionsZeroNestedPathsAndOriginals() throws Exception {
        var source = file("minecraft", """
                {"arrow":{"weight":0.053125,"density":7},"stone":{"weight":0},"dirt":{"density":4}}
                """);
        file("absentmod", "{\"metals/steel_ingot\":{\"weight\":2.375}}");
        byte[] before = Files.readAllBytes(source);
        var result = convert();
        assertEquals(3, result.converted());
        assertEquals(1, result.skipped());
        assertArrayEquals(before, Files.readAllBytes(source));
        try (var zip = new ZipFile(result.file().toFile())) {
            assertEquals(4, zip.size());
            var arrow = zip.getEntry("data/minecraft/heavyinventories/weights/arrow.json");
            try (var reader = new InputStreamReader(zip.getInputStream(arrow), StandardCharsets.UTF_8)) {
                assertEquals(new WeightDefinition.Fixed(0.053125f), WeightDefinition.parse(reader));
            }
            var stone = zip.getEntry("data/minecraft/heavyinventories/weights/stone.json");
            try (var reader = new InputStreamReader(zip.getInputStream(stone), StandardCharsets.UTF_8)) {
                assertEquals(new WeightDefinition.Fixed(0), WeightDefinition.parse(reader));
            }
            assertNotNull(zip.getEntry("data/absentmod/heavyinventories/weights/metals/steel_ingot.json"));
            try (var reader = new InputStreamReader(zip.getInputStream(zip.getEntry("pack.mcmeta")), StandardCharsets.UTF_8)) {
                var pack = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("pack");
                assertEquals("[101,1]", pack.get("min_format").toString());
                assertEquals(pack.get("min_format"), pack.get("max_format"));
            }
        }
    }
    @Test void existingPackIsNeverReplaced() throws Exception {
        file("minecraft", "{\"arrow\":{\"weight\":1}}");
        var first = convert();
        byte[] before = Files.readAllBytes(first.file());
        file("minecraft", "{\"arrow\":{\"weight\":2}}");
        assertThrows(FileAlreadyExistsException.class, this::convert);
        assertArrayEquals(before, Files.readAllBytes(first.file()));
        try (var files = Files.list(output())) { assertEquals(1, files.count()); }
    }
    @Test void invalidInputNeverPublishesValidSubset() throws Exception {
        file("minecraft", "{\"stone\":{\"weight\":2}}");
        var bad = file("zzbad", "{\"item\":{\"weight\":-1}}");
        byte[] before = Files.readAllBytes(bad);
        var failure = assertThrows(IllegalArgumentException.class, this::convert);
        assertTrue(failure.getMessage().contains("zzbad.json"));
        assertArrayEquals(before, Files.readAllBytes(bad));
        assertFalse(Files.exists(output()));
    }
    @Test void malformedDuplicateAndOutOfRangeValuesAreRejected() throws Exception {
        for (String json : new String[]{
                "{", "{\"arrow\":{\"weight\":1},\"arrow\":{\"weight\":2}}",
                "{\"arrow\":{\"weight\":1,\"weight\":2}}", "{\"arrow\":{\"weight\":\"1\"}}",
                "{\"arrow\":{\"weight\":null}}", "{\"arrow\":{\"weight\":1e-100}}",
                "{\"arrow\":{\"weight\":1000000000.01}}", "{\"arrow\":{\"weight\":1}} true",
                "{\"format\":\"heavyinventories:weight_report\",\"items\":{}}"
        }) {
            file("minecraft", json);
            assertThrows(IllegalArgumentException.class, this::convert, json);
            assertFalse(Files.exists(output()));
        }
    }
    @Test void unsafeNamesAndPathsCannotEscapeTheArchive() throws Exception {
        for (String item : new String[]{"../arrow", "/arrow", "metals/../arrow", "metals//arrow", "metals/./arrow"}) {
            file("minecraft", "{\"" + item + "\":{\"weight\":1}}");
            assertThrows(IllegalArgumentException.class, this::convert, item);
        }
        file("minecraft", "{\"arrow\":{\"weight\":1}}");
        for (String name : new String[]{"../escape", ".", "a/b", "A", ""})
            assertThrows(IllegalArgumentException.class, () -> LegacyWeightConverter.convert(input(), output(), name, 101, 1));
        assertFalse(Files.exists(output()));
    }
    @Test void emptyOrMetadataOnlyInputDoesNotPublishAPack() throws Exception {
        assertThrows(IllegalArgumentException.class, this::convert);
        file("minecraft", "{\"stone\":{\"density\":4}}");
        assertThrows(IllegalArgumentException.class, this::convert);
        assertFalse(Files.exists(output()));
    }
}
