package com.iso2t.heavyinventories.server;

import com.iso2t.heavyinventories.config.ServerSettings;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ServerWeightStateTest {
    private static final Identifier STONE = Identifier.parse("minecraft:stone");

    @Test void publishedWeightsAndPacketsAreImmutableSnapshots() {
        var state = new ServerWeightState();
        var input = new HashMap<>(Map.of(STONE, 2.375f));
        state.replace(new ServerSettings(1000), input);
        var firstPackets = state.packets();
        input.put(STONE, 9f);
        assertEquals(2.375f, state.unitWeight(STONE));
        assertEquals(2.375f, firstPackets.getFirst().entries().getFirst().weight());
        assertThrows(UnsupportedOperationException.class, () -> state.weights().clear());
        state.replace(new ServerSettings(2000), input);
        assertEquals(9f, state.unitWeight(STONE));
        assertEquals(2.375f, firstPackets.getFirst().entries().getFirst().weight());
        assertEquals(2, state.revision());
    }

    @Test void invalidCandidateCannotPartiallyReplaceState() {
        var state = new ServerWeightState();
        state.replace(new ServerSettings(1000), Map.of(STONE, 2f));
        var packets = state.packets();
        for (float invalid : new float[]{-1, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> state.replace(new ServerSettings(2000), Map.of(STONE, invalid)));
            assertEquals(1000, state.settings().startingWeight());
            assertEquals(2f, state.unitWeight(STONE));
            assertEquals(1, state.revision());
            assertSame(packets, state.packets());
        }
    }
}
