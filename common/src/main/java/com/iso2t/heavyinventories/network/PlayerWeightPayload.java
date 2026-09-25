package com.iso2t.heavyinventories.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlayerWeightPayload(int entityId, Identifier dimension, float weight, float baseCapacity,
        float bracing, float reinforced, float surefooted, boolean encumbered, boolean overEncumbered, boolean canEdit, long revision)
        implements CustomPacketPayload {
    public static final Type<PlayerWeightPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("heavyinventories", "player_weight"));
    public static final StreamCodec<FriendlyByteBuf, PlayerWeightPayload> CODEC = StreamCodec.of((buf, p) -> {
        buf.writeVarInt(p.entityId);
        buf.writeIdentifier(p.dimension);
        buf.writeFloat(p.weight);
        buf.writeFloat(p.baseCapacity);
        buf.writeFloat(p.bracing);
        buf.writeFloat(p.reinforced);
        buf.writeFloat(p.surefooted);
        buf.writeBoolean(p.encumbered);
        buf.writeBoolean(p.overEncumbered);
        buf.writeBoolean(p.canEdit);
        buf.writeVarLong(p.revision);
    }, buf -> new PlayerWeightPayload(buf.readVarInt(), buf.readIdentifier(), buf.readFloat(), buf.readFloat(),
            buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readVarLong()));

    public PlayerWeightPayload {
        if (revision < 1 || !Float.isFinite(weight) || weight < 0 || !Float.isFinite(baseCapacity) || baseCapacity <= 0
                || !Float.isFinite(bracing) || bracing < 0 || !Float.isFinite(reinforced) || reinforced < 0
                || !Float.isFinite(baseCapacity + bracing + reinforced)
                || !Float.isFinite(surefooted) || surefooted < 0 || surefooted > 1)
            throw new IllegalArgumentException("Invalid player weight snapshot");
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
