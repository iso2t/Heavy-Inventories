package com.iso2t.heavyinventories.client;

import com.iso2t.heavyinventories.network.ItemWeightsPayload;
import net.minecraft.resources.Identifier;
import java.util.HashMap;
import java.util.Map;

/** Client-thread-only connection data. Server code never reads or writes this store. */
public final class ClientWeightData {
    private static final DefinitionReceiver DEFINITIONS = new DefinitionReceiver();
    private ClientWeightData() {}
    public static void accept(ItemWeightsPayload payload) { DEFINITIONS.accept(payload); }
    public static Float weight(Identifier item) { return DEFINITIONS.weight(item); }
    public static double unitWeight(Identifier item) {
        var weight = weight(item);
        return weight == null ? Double.NaN : weight;
    }
    public static void clear() { DEFINITIONS.clear(); }

    /** Publishes a complete revision atomically, never a partly received item table. */
    public static final class DefinitionReceiver {
        private Map<Identifier, Float> active = Map.of();
        private final Map<Identifier, Float> pending = new HashMap<>();
        private long revision;
        private long pendingRevision;
        private int nextIndex;
        private int chunks;

        public void accept(ItemWeightsPayload payload) {
            if (payload.revision() < revision || payload.revision() < pendingRevision) return;
            if (payload.index() == 0) {
                pending.clear();
                pendingRevision = payload.revision();
                nextIndex = 0;
                chunks = payload.chunks();
            }
            if (payload.revision() != pendingRevision || payload.index() != nextIndex || payload.chunks() != chunks) return;
            for (var entry : payload.entries()) pending.put(entry.item(), entry.weight());
            nextIndex++;
            if (nextIndex == chunks) {
                active = Map.copyOf(pending);
                revision = pendingRevision;
                pending.clear();
            }
        }

        public Float weight(Identifier item) { return active.get(item); }
        public void clear() {
            active = Map.of();
            pending.clear();
            revision = pendingRevision = 0;
            nextIndex = chunks = 0;
        }
    }
}
