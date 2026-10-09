package com.iso2t.heavyinventories.network;

import com.iso2t.heavyinventories.HeavyInventories;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public record OpenConfigPayload(String configType) implements CustomPacketPayload {

	public static final Type<OpenConfigPayload>                         TYPE  = new Type<>(HeavyInventories.get("open_config"));
	public static final StreamCodec<FriendlyByteBuf, OpenConfigPayload> CODEC = StreamCodec.of((buf, packet) -> buf.writeUtf(packet.configType()), buf -> new OpenConfigPayload(buf.readUtf()));

	@Override
	public @NonNull Type<? extends CustomPacketPayload> type () {
		return TYPE;
	}

}
