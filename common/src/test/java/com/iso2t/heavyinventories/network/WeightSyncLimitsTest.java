package com.iso2t.heavyinventories.network;

import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.server.ServerWeightState;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WeightSyncLimitsTest {

	private static final Identifier               STONE   = Identifier.parse("minecraft:stone");
	private static final Identifier               DIRT    = Identifier.parse("minecraft:dirt");
	private static final ItemWeightsPayload.Entry OLD     = new ItemWeightsPayload.Entry(STONE, 2);
	private static final ItemWeightsPayload.Entry UPDATED = new ItemWeightsPayload.Entry(STONE, 3);
	private static final ItemWeightsPayload.Entry OTHER   = new ItemWeightsPayload.Entry(DIRT, 4);

	@Test
	void decoderRejectsLargeIdentifiersAndInvalidHeadersBeforeReadingEntries () {
		var buffer = new FriendlyByteBuf(Unpooled.buffer());
		try {
			buffer.writeVarLong(1).writeVarInt(0).writeVarInt(1).writeVarInt(1);
			buffer.writeUtf("audit:" + "x".repeat(30_000));
			buffer.writeFloat(1);
			assertThrows(RuntimeException.class, () -> ItemWeightsPayload.CODEC.decode(buffer));
			for (int[] header : new int[][] { { 0, 0, 1, 1 }, { 1, 1, 1, 1 }, { 1, 0, ItemWeightsPayload.MAX_CHUNKS + 1, 1 }, { 1, 0, 1, -1 }, { 1, 0, 1, 257 } }) {
				buffer.clear();
				buffer.writeVarLong(header[0]).writeVarInt(header[1]).writeVarInt(header[2]).writeVarInt(header[3]);
				assertThrows(IllegalArgumentException.class, () -> ItemWeightsPayload.CODEC.decode(buffer));
			}
			buffer.clear();
			var id = Identifier.parse("audit:" + "x".repeat(ItemWeightsPayload.MAX_IDENTIFIER_LENGTH - 6));
			var packet = packet(1, 0, 1, new ItemWeightsPayload.Entry(id, 1));
			ItemWeightsPayload.CODEC.encode(buffer, packet);
			assertEquals(packet, ItemWeightsPayload.CODEC.decode(buffer));
			assertThrows(IllegalArgumentException.class, () -> new ItemWeightsPayload.Entry(Identifier.parse(id + "x"), 1));
		} finally {
			buffer.release();
		}
	}

	@Test
	void invalidTransfersDiscardPendingDataAndCannotRestartTheSameRevision () {
		var unknown = new ItemWeightsPayload.Entry(Identifier.parse("audit:unknown"), 1);
		for (var invalid : List.of(packet(2, 1, 2, unknown), packet(2, 1, 2, UPDATED), packet(2, 1, 3, OTHER), packet(2, 0, 2, OTHER))) {
			var receiver = new ClientWeightData.DefinitionReceiver(id -> id.equals(STONE) || id.equals(DIRT));
			receiver.accept(packet(1, 0, 1, OLD));
			receiver.accept(packet(2, 0, 2, UPDATED));
			receiver.accept(invalid);
			receiver.accept(packet(2, 1, 2, OTHER));
			receiver.accept(packet(2, 0, 1, UPDATED));
			assertEquals(2f, receiver.weight(STONE));
			assertNull(receiver.weight(DIRT));
			receiver.accept(packet(3, 0, 1, OTHER));
			assertEquals(4f, receiver.weight(DIRT));
			assertNull(receiver.weight(STONE));
			receiver.accept(packet(3, 0, 1, OLD));
			assertEquals(4f, receiver.weight(DIRT));
		}
	}

	@Test
	void duplicateEntriesWithinAChunkRejectTheWholeRevision () {
		var receiver = new ClientWeightData.DefinitionReceiver(id -> true);
		receiver.accept(packet(1, 0, 1, OLD));
		receiver.accept(packet(2, 0, 1, OTHER, OTHER));
		assertEquals(2f, receiver.weight(STONE));
		assertNull(receiver.weight(DIRT));
	}

	@Test
	void aggregateLimitsApplyToSenderAndReceiverWithoutReplacingActiveState () {
		int fullEntryBytes = ItemWeightsPayload.MAX_IDENTIFIER_LENGTH + Float.BYTES + 2;
		for (int[] limit : new int[][] { { ItemWeightsPayload.MAX_ENTRIES + 1, 0 }, { ItemWeightsPayload.MAX_TABLE_BYTES / fullEntryBytes + 1, ItemWeightsPayload.MAX_IDENTIFIER_LENGTH } }) {
			var values = definitions(limit[0], limit[1]);
			var server = new ServerWeightState();
			server.replace(new ServerSettings(1000), Map.of(STONE, 2f));
			var packets = server.packets();
			assertThrows(IllegalArgumentException.class, () -> server.replace(new ServerSettings(2000), values));
			assertEquals(1, server.revision());
			assertEquals(1000, server.settings().startingWeight());
			assertEquals(packets, server.packets());
			var receiver = new ClientWeightData.DefinitionReceiver(id -> true);
			receiver.accept(packet(1, 0, 1, OLD));
			var entries = values.entrySet().stream().map(e -> new ItemWeightsPayload.Entry(e.getKey(), e.getValue())).toList();
			int chunks = (entries.size() + ItemWeightsPayload.CHUNK_SIZE - 1) / ItemWeightsPayload.CHUNK_SIZE;
			for (int index = 0; index < chunks; index++) {
				int from = index * ItemWeightsPayload.CHUNK_SIZE;
				receiver.accept(new ItemWeightsPayload(2, index, chunks, entries.subList(from, Math.min(from + ItemWeightsPayload.CHUNK_SIZE, entries.size()))));
			}
			assertEquals(2f, receiver.weight(STONE));
			assertNull(receiver.weight(entries.getFirst().item()));
			receiver.accept(packet(3, 0, 1, OTHER));
			assertEquals(4f, receiver.weight(DIRT));
		}
	}

	@Test
	void maximumEntryCountAndEmptyTablesStillSynchronize () {
		var values = definitions(ItemWeightsPayload.MAX_ENTRIES, 0);
		var state = new ServerWeightState();
		state.replace(new ServerSettings(1000), values);
		var receiver = new ClientWeightData.DefinitionReceiver(values::containsKey);
		state.packets().forEach(receiver::accept);
		values.forEach((id, weight) -> assertEquals(weight, receiver.weight(id)));
		receiver.accept(new ItemWeightsPayload(2, 0, 1, List.of()));
		assertNull(receiver.weight(values.keySet().iterator().next()));
		receiver.clear();
		state.packets().forEach(receiver::accept);
		assertEquals(1f, receiver.weight(values.keySet().iterator().next()));
	}

	private static Map<Identifier, Float> definitions (int count, int length) {
		var values = new HashMap<Identifier, Float>();
		for (int i = 0; i < count; i++) {
			var id = "audit:item_" + i;
			values.put(Identifier.parse(id + "x".repeat(Math.max(0, length - id.length()))), 1f);
		}
		return values;
	}

	private static ItemWeightsPayload packet (long revision, int index, int chunks, ItemWeightsPayload.Entry... entries) {
		return new ItemWeightsPayload(revision, index, chunks, List.of(entries));
	}
}
