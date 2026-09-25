package com.iso2t.heavyinventories.network;

import com.iso2t.heavyinventories.config.ServerSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;

/** Bounded chunks keep large modpack registries below the payload size limit. */
public record ItemWeightsPayload(long revision, int index, int chunks, List<Entry> entries) implements CustomPacketPayload {
    public static final int CHUNK_SIZE = 256;
    public static final int MAX_CHUNKS = 4096;
    public static final Type<ItemWeightsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("heavyinventories", "item_weights"));
    public static final StreamCodec<FriendlyByteBuf, ItemWeightsPayload> CODEC = StreamCodec.of((buf, p) -> {
        buf.writeVarLong(p.revision);
        buf.writeVarInt(p.index);
        buf.writeVarInt(p.chunks);
        buf.writeVarInt(p.entries.size());
        for (var entry : p.entries) {
            buf.writeIdentifier(entry.item);
            buf.writeFloat(entry.weight);
        }
    }, buf -> {
        long revision = buf.readVarLong();
        int index = buf.readVarInt(), chunks = buf.readVarInt(), count = buf.readVarInt();
        if (count < 0 || count > CHUNK_SIZE) throw new IllegalArgumentException("Invalid definition chunk size");
        var entries = new ArrayList<Entry>(count);
        for (int i = 0; i < count; i++) entries.add(new Entry(buf.readIdentifier(), buf.readFloat()));
        return new ItemWeightsPayload(revision, index, chunks, entries);
    });

    public ItemWeightsPayload {
        if (revision < 1 || chunks < 1 || chunks > MAX_CHUNKS || index < 0 || index >= chunks || entries.size() > CHUNK_SIZE)
            throw new IllegalArgumentException("Invalid definition chunk");
        entries = List.copyOf(entries);
    }

    public record Entry(Identifier item, float weight) {
        public Entry { ServerSettings.validateItemWeight(weight); }
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
