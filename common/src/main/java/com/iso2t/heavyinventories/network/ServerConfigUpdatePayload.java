package com.iso2t.heavyinventories.network;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.config.EffectsSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record ServerConfigUpdatePayload(float startingWeight, String walkingMode, EffectsSettings effects, long expectedRevision) implements CustomPacketPayload {
	public static final Type<ServerConfigUpdatePayload>                         TYPE  = new Type<>(Identifier.fromNamespaceAndPath(HeavyInventories.MOD_ID, "edit_server_config_v3"));
	public static final StreamCodec<FriendlyByteBuf, ServerConfigUpdatePayload> CODEC = StreamCodec.of((buf, p) -> {
		buf.writeFloat(p.startingWeight);
		buf.writeUtf(p.walkingMode, 32);
		EffectsSettings.CODEC.encode(buf, p.effects);
		buf.writeVarLong(p.expectedRevision);
	}, buf -> new ServerConfigUpdatePayload(buf.readFloat(), buf.readUtf(32), EffectsSettings.CODEC.decode(buf), buf.readVarLong()));

	public ServerConfigUpdatePayload (ServerSettings settings, long expectedRevision) {
		this(settings.startingWeight(), settings.walkingMode().id(), settings.effects(), expectedRevision);
	}

	public ServerConfigUpdatePayload (float startingWeight, String walkingMode, long expectedRevision) {
		this(startingWeight, walkingMode, EffectsSettings.DEFAULT, expectedRevision);
	}

	public ServerConfigUpdatePayload (float startingWeight, long expectedRevision) {
		this(startingWeight, ServerSettings.DEFAULT.walkingMode().id(), expectedRevision);
	}

	@Override
	public @NonNull Type<? extends CustomPacketPayload> type () {
		return TYPE;
	}
}
