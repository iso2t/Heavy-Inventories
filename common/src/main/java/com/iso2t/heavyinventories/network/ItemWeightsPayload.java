package com.iso2t.heavyinventories.network;

import com.google.common.base.Preconditions;
import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.config.ServerSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Bounded chunks keep large modpack registries below the payload size limit.
 */
public record ItemWeightsPayload(long revision, int index, int chunks, List<Entry> entries) implements CustomPacketPayload {

	public static final int                                              CHUNK_SIZE = 256;
	public static final int                                              MAX_ENTRIES = 100_000;
	public static final int                                              MAX_TABLE_BYTES = 8 * 1024 * 1024;
	public static final int                                              MAX_IDENTIFIER_LENGTH = 256;
	public static final int                                              MAX_CHUNKS = (MAX_ENTRIES + CHUNK_SIZE - 1) / CHUNK_SIZE;
	public static final Type<ItemWeightsPayload>                         TYPE       = new Type<>(HeavyInventories.get("item_weights"));
	public static final StreamCodec<FriendlyByteBuf, ItemWeightsPayload> CODEC      = StreamCodec.of((buf, p) -> {
		buf.writeVarLong(p.revision);
		buf.writeVarInt(p.index);
		buf.writeVarInt(p.chunks);
		buf.writeVarInt(p.entries.size());
		for (var entry : p.entries) {
			buf.writeUtf(entry.item.toString(), MAX_IDENTIFIER_LENGTH);
			buf.writeFloat(entry.weight);
		}
	}, buf -> {
		long revision = buf.readVarLong();
		int index = buf.readVarInt(), chunks = buf.readVarInt(), count = buf.readVarInt();
		validateHeader(revision, index, chunks, count);
		var entries = new ArrayList<Entry>(count);
		for (int i = 0; i < count; i++) entries.add(new Entry(Identifier.parse(buf.readUtf(MAX_IDENTIFIER_LENGTH)), buf.readFloat()));
		return new ItemWeightsPayload(revision, index, chunks, entries);
	});

	public ItemWeightsPayload {
		validateHeader(revision, index, chunks, entries.size());
		entries = List.copyOf(entries);
	}

	private static void validateHeader (long revision, int index, int chunks, int count) {
		Preconditions.checkArgument(revision >= 1 && chunks >= 1 && chunks <= MAX_CHUNKS && index >= 0 && index < chunks && count >= 0 && count <= CHUNK_SIZE, "Invalid definition chunk");
	}

	public record Entry(Identifier item, float weight) {
		public Entry {
			Preconditions.checkArgument(item != null && item.toString().length() <= MAX_IDENTIFIER_LENGTH, "Invalid item weight identifier");
			ServerSettings.validateItemWeight(weight);
		}

		public int sizeBytes () {
			return item.toString().length() + Float.BYTES + 2;
		}
	}

	@Override
	public @NonNull Type<? extends CustomPacketPayload> type () {
		return TYPE;
	}
}
