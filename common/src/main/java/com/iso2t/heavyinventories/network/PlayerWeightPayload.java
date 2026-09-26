package com.iso2t.heavyinventories.network;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.config.EffectsSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record PlayerWeightPayload(int entityId, Identifier dimension, float weight, float baseCapacity, float bracing, float reinforced, float strength, float walkingMultiplier, WalkingMode walkingMode, boolean encumbered,
								  boolean overEncumbered, boolean canEdit, long revision, EffectsSettings effects) implements CustomPacketPayload {
	public static final Type<PlayerWeightPayload>                         TYPE  = new Type<>(Identifier.fromNamespaceAndPath(HeavyInventories.MOD_ID, "player_weight_v3"));
	public static final StreamCodec<FriendlyByteBuf, PlayerWeightPayload> CODEC = StreamCodec.of((buf, p) -> {
		buf.writeVarInt(p.entityId);
		buf.writeIdentifier(p.dimension);
		buf.writeFloat(p.weight);
		buf.writeFloat(p.baseCapacity);
		buf.writeFloat(p.bracing);
		buf.writeFloat(p.reinforced);
		buf.writeFloat(p.strength);
		buf.writeFloat(p.walkingMultiplier);
		buf.writeEnum(p.walkingMode);
		buf.writeBoolean(p.encumbered);
		buf.writeBoolean(p.overEncumbered);
		buf.writeBoolean(p.canEdit);
		buf.writeVarLong(p.revision);
		EffectsSettings.CODEC.encode(buf, p.effects);
	}, buf -> new PlayerWeightPayload(buf.readVarInt(), buf.readIdentifier(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readEnum(WalkingMode.class), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readVarLong(), EffectsSettings.CODEC.decode(buf)));

	public PlayerWeightPayload (int entityId, Identifier dimension, float weight, float baseCapacity, float bracing, float reinforced, float strength, float walkingMultiplier, WalkingMode walkingMode, boolean encumbered, boolean overEncumbered, boolean canEdit, long revision) {
		this(entityId, dimension, weight, baseCapacity, bracing, reinforced, strength, walkingMultiplier, walkingMode, encumbered, overEncumbered, canEdit, revision, EffectsSettings.DEFAULT);
	}

	public PlayerWeightPayload {
		if (effects == null) throw new IllegalArgumentException("Missing server effect settings");
		if (revision < 1 || !Float.isFinite(weight) || weight < 0 || !Float.isFinite(baseCapacity) || baseCapacity <= 0 || !Float.isFinite(bracing) || bracing < 0 || !Float.isFinite(reinforced) || reinforced < 0 || !Float.isFinite(strength) || strength < 0 || !Float.isFinite(baseCapacity + bracing + reinforced + strength) || walkingMode == null || !Float.isFinite(walkingMultiplier) || walkingMultiplier < 0 || walkingMultiplier > 1)
			throw new IllegalArgumentException("Invalid player weight snapshot");
	}

	@Override
	public @NonNull Type<? extends CustomPacketPayload> type () {
		return TYPE;
	}
}
