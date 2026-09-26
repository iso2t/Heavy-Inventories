package com.iso2t.heavyinventories.network;

import com.iso2t.heavyinventories.api.player.EncumbranceEffects;
import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.config.EffectsSettings;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import com.iso2t.heavyinventories.server.ServerWeightState;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WeightProtocolTest {
	private static final Identifier STONE = Identifier.parse("minecraft:stone");
	private static final Identifier DIRT  = Identifier.parse("minecraft:dirt");

	@Test
	void codecsPreserveFractionalValuesAndState () {
		var state = new PlayerWeightPayload(23, Identifier.parse("minecraft:overworld"), 16.5f, 10.5f, 1.05f, 0.525f, 2.1f, 0.4f, com.iso2t.heavyinventories.config.WalkingMode.AT_NINETY_PERCENT, false, true, true, 72);
		assertEquals(state, roundTrip(PlayerWeightPayload.CODEC, state));
		var definitions = chunk(72, 0, 1, STONE, 2.125f);
		assertEquals(definitions, roundTrip(ItemWeightsPayload.CODEC, definitions));
		var edit = new ServerConfigUpdatePayload(1000.25f, "at_ninety_percent", 72);
		assertEquals(edit, roundTrip(ServerConfigUpdatePayload.CODEC, edit));
	}

	@Test
	void nondefaultEffectsSurviveBothDirectionsAndCalculateIdentically () {
		var d = EffectsSettings.DEFAULT;
		var effects = new EffectsSettings(false, true, new EffectsSettings.Exhaustion(false, 2.25f, 0.02f), new EffectsSettings.FallDamage(true, 67.5f, 140, 4.25f), d.swimming(), d.sinking(), new EffectsSettings.UpwardMovement(true, 98.25f), new EffectsSettings.Knockback(true, 456.25f, 0.75f));
		var request = new ServerConfigUpdatePayload(new ServerSettings(500, WalkingMode.AT_NINETY_PERCENT, effects), 12);
		assertEquals(request, roundTrip(ServerConfigUpdatePayload.CODEC, request));
		var packet = new PlayerWeightPayload(7, Identifier.parse("minecraft:overworld"), 550, 500, 0, 0, 0, 0, WalkingMode.AT_NINETY_PERCENT, false, true, false, 12, effects);
		var decoded = roundTrip(PlayerWeightPayload.CODEC, packet);
		assertEquals(packet, decoded);
		assertEquals(EncumbranceEffects.calculate(550, 500, packet.walkingMode(), effects, EncumbranceEffects.Fluid.LAVA, false, false), EncumbranceEffects.calculate(decoded.weight(), decoded.baseCapacity(), decoded.walkingMode(), decoded.effects(), EncumbranceEffects.Fluid.LAVA, false, false));
	}

	@Test
	void effectDecoderRejectsMalformedAndOversizedSettings () {
		for (String json : new String[] { "{\"knockback\":{\"referenceWeight\":0}}", "{\"fallDamage\":{\"startPercent\":200}}", "{\"water\":\"true\"}", " ".repeat(8193) }) {
			var buf = new FriendlyByteBuf(Unpooled.buffer());
			try {
				buf.writeUtf(json);
				assertThrows(RuntimeException.class, () -> EffectsSettings.CODEC.decode(buf));
			} finally {
				buf.release();
			}
		}
	}

	@Test
	void definitionsPublishOnlyCompleteRevisionsAndClearBetweenConnections () {
		var receiver = new ClientWeightData.DefinitionReceiver();
		receiver.accept(chunk(1, 0, 1, STONE, 2));
		receiver.accept(chunk(2, 0, 2, STONE, 3));
		assertEquals(2f, receiver.weight(STONE));
		assertNull(receiver.weight(DIRT));
		receiver.accept(chunk(1, 0, 1, STONE, 999));
		receiver.accept(chunk(2, 1, 2, DIRT, 4));
		assertEquals(3f, receiver.weight(STONE));
		assertEquals(4f, receiver.weight(DIRT));
		receiver.accept(chunk(3, 1, 2, STONE, 999));
		assertEquals(3f, receiver.weight(STONE));
		receiver.clear();
		assertNull(receiver.weight(STONE));
		receiver.accept(chunk(1, 0, 1, STONE, 5));
		assertEquals(5f, receiver.weight(STONE));
	}

	@Test
	void decoderRejectsOversizedChunksBeforeAllocatingEntries () {
		var buf = new FriendlyByteBuf(Unpooled.buffer());
		try {
			buf.writeVarLong(1).writeVarInt(0).writeVarInt(1).writeVarInt(ItemWeightsPayload.CHUNK_SIZE + 1);
			assertThrows(IllegalArgumentException.class, () -> ItemWeightsPayload.CODEC.decode(buf));
		} finally {
			buf.release();
		}
		assertThrows(IllegalArgumentException.class, () -> chunk(1, 0, 1, STONE, Float.NaN));
		assertThrows(IllegalArgumentException.class, () -> chunk(1, 1, 1, STONE, 1));
	}

	@Test
	void rejectedDefinitionsPreserveActiveServerRevision () {
		var state = new ServerWeightState();
		state.replace(new ServerSettings(10.5f), Map.of(STONE, 2f));
		var packets = state.packets();
		assertThrows(IllegalArgumentException.class, () -> state.replace(new ServerSettings(20), Map.of(STONE, -1f)));
		assertEquals(1, state.revision());
		assertEquals(10.5f, state.settings().startingWeight());
		assertEquals(2f, state.weights().get(STONE));
		assertEquals(packets, state.packets());
	}

	private static ItemWeightsPayload chunk (long revision, int index, int count, Identifier id, float weight) {
		return new ItemWeightsPayload(revision, index, count, List.of(new ItemWeightsPayload.Entry(id, weight)));
	}

	private static <T> T roundTrip (StreamCodec<FriendlyByteBuf, T> codec, T value) {
		var buf = new FriendlyByteBuf(Unpooled.buffer());
		try {
			codec.encode(buf, value);
			T result = codec.decode(buf);
			assertEquals(0, buf.readableBytes());
			return result;
		} finally {
			buf.release();
		}
	}
}
