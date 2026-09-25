package com.iso2t.heavyinventories.config;

import com.google.gson.JsonParser;
import com.iso2t.heavyinventories.api.util.MeasuringSystem;
import com.iso2t.heavyinventories.client.WeightDisplay;
import org.junit.jupiter.api.Test;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class DisplaySettingsTest {
    @Test void unitConversionChangesDisplayOnly() {
        assertEquals(45.359237, MeasuringSystem.KGS.fromStored(100), 0.0000001);
        assertEquals(100, MeasuringSystem.LBS.fromStored(100));
        assertEquals(100, MeasuringSystem.NONE.fromStored(100));
        var locale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            assertEquals("45.36 kg", WeightDisplay.weight(100, MeasuringSystem.KGS));
            assertEquals("0.1 lbs", WeightDisplay.weight(0.1f, MeasuringSystem.LBS));
            assertEquals("<0.01 kg", WeightDisplay.weight(0.001, MeasuringSystem.KGS));
            assertEquals("100", WeightDisplay.weight(100, MeasuringSystem.NONE));
            assertEquals("<0.01", WeightDisplay.weight(0.005, MeasuringSystem.NONE));
            Locale.setDefault(Locale.GERMANY);
            assertEquals("45,36 kg", WeightDisplay.weight(100, MeasuringSystem.KGS));
        } finally { Locale.setDefault(locale); }
    }

    @Test void clientPreferencesRoundTripDistinctColors() {
        var expected = new ClientSettings(MeasuringSystem.KGS, false, 0x123456, 0xABCDEF, 0x010203);
        assertEquals(expected, ClientSettings.parse(expected.toJson()));
    }

    @Test void invalidClientFieldsCannotPartiallyApply() {
        var original = ClientSettings.current();
        for (String json : new String[]{"{\"enableGuiOverlay\":null}", "{\"normalTextColor\":1.5}",
                "{\"normalTextColor\":-1}", "{\"normalTextColor\":16777216}", "{\"weightMeasure\":\"invalid\"}",
                "{\"weightMeasure\":\"KGS\",\"encumberedTextColor\":[]}"}) {
            assertThrows(IllegalArgumentException.class, () -> ClientSettings.parse(JsonParser.parseString(json).getAsJsonObject()).apply());
            assertEquals(original, ClientSettings.current());
        }
    }
}
