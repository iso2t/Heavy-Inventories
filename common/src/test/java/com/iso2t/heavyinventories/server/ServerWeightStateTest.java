package com.iso2t.heavyinventories.server;

import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class ServerWeightStateTest {
    @TempDir Path directory;
    private static final Identifier STONE = Identifier.parse("minecraft:stone");

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void legacyOverridesPreserveDecimalsIndependentOfLocale() throws Exception {
        Files.writeString(directory.resolve("minecraft.json"), "{\"stone\":{\"weight\":2.375}}");
        var original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            var weights = ServerWeightState.loadWeights(directory);
            assertEquals(2.375f, weights.get(STONE));
            assertEquals(0.1f, weights.get(Identifier.parse("minecraft:dirt")));
            var overrides = ServerWeightState.loadOverrides(directory);
            assertEquals(2.375f, overrides.get(STONE));
            assertFalse(overrides.containsKey(Identifier.parse("minecraft:dirt")),
                    "Fallback values must not become explicit recipe anchors");
        } finally { Locale.setDefault(original); }
    }

    @Test void malformedOverridesAreRejectedWithoutReplacingFiles() throws Exception {
        var file = directory.resolve("minecraft.json");
        for (String data : new String[]{"[1]", "{", "{\"stone\":3}", "{\"stone\":{\"weight\":-1}}",
                "{\"stone\":{\"weight\":1e100}}", "{\"stone\":{\"weight\":\"2\"}}"}) {
            Files.writeString(file, data);
            assertThrows(IllegalArgumentException.class, () -> ServerWeightState.loadWeights(directory));
            assertEquals(data, Files.readString(file));
        }
    }
}
