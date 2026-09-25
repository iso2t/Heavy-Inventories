package com.iso2t.heavyinventories.server.weight;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeightDefinitionTest {
    private static WeightDefinition parse(String json) throws Exception {
        return WeightDefinition.parse(new StringReader(json));
    }

    @Test void fixedZeroFractionsAndInferenceStayDistinct() throws Exception {
        assertEquals(new WeightDefinition.Fixed(0), parse("{\"weight\":0}"));
        assertEquals(new WeightDefinition.Fixed(0.053125f), parse("{\"weight\":0.053125}"));
        assertEquals(new WeightDefinition.Fixed(0.053125f), parse("{\"weight\":5.3125e-2}"));
        assertEquals(WeightDefinition.Infer.INSTANCE, parse("{\"infer\":true}"));
        assertEquals(new WeightDefinition.Fixed(1_000_000_000), parse("{\"weight\":1000000000}"));
    }

    @Test void strictSchemaRejectsAmbiguousAndMalformedDocuments() {
        for (var json : new String[]{"", "null", "[]", "{}", "{", "{weight:1}", "{'weight':1}",
                "{\"weight\":null}", "{\"weight\":\"1\"}", "{\"weight\":true}", "{\"weight\":[]}",
                "{\"weight\":1,\"weight\":2}", "{\"infer\":true,\"infer\":true}",
                "{\"weight\":1,\"infer\":true}", "{\"infer\":true,\"weight\":1}",
                "{\"infer\":false}", "{\"infer\":\"true\"}", "{\"weights\":1}",
                "{\"weight\":1,\"note\":\"typo\"}", "{\"weight\":1,}", "{\"weight\":1} {}",
                "/* comment */ {\"weight\":1}", "{\"weight\":01}", "{\"weight\":NaN}", "{\"weight\":Infinity}"}) {
            assertThrows(Exception.class, () -> parse(json), json);
        }
    }

    @Test void validatesDecimalsBeforeFloatRoundingAndRejectsUnderflow() throws Exception {
        for (var value : new String[]{"-1", "1000000000.01", "1e100", "1e-100", "1e-2147483648"})
            assertThrows(IllegalArgumentException.class, () -> parse("{\"weight\":" + value + "}"), value);
        assertEquals(new WeightDefinition.Fixed(Float.MIN_VALUE), parse("{\"weight\":1.4e-45}"));
    }

    @Test void documentLimitBoundsOversizedNumericAndWhitespaceInput() {
        assertThrows(IllegalArgumentException.class, () -> parse(" ".repeat(WeightDefinition.MAX_DOCUMENT_CHARS) + "{\"weight\":1}"));
        assertThrows(IllegalArgumentException.class, () -> parse("{\"weight\":0." + "0".repeat(WeightDefinition.MAX_DOCUMENT_CHARS) + "1}"));
    }
}
