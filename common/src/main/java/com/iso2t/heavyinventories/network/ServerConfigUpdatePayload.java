package com.iso2t.heavyinventories.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ServerConfigUpdatePayload(float startingWeight, long expectedRevision) implements CustomPacketPayload {
    public static final Type<ServerConfigUpdatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("heavyinventories", "edit_server_config"));
    public static final StreamCodec<FriendlyByteBuf, ServerConfigUpdatePayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeFloat(p.startingWeight); buf.writeVarLong(p.expectedRevision); },
            buf -> new ServerConfigUpdatePayload(buf.readFloat(), buf.readVarLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
