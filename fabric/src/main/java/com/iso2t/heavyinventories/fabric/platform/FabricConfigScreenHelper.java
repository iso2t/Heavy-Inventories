package com.iso2t.heavyinventories.fabric.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.fabric.client.FabricClientHooks;
import com.iso2t.heavyinventories.platform.services.IConfigScreenHelper;

public class FabricConfigScreenHelper implements IConfigScreenHelper {

    public static final Identifier OPEN_CONFIG_PACKET_ID = Identifier.fromNamespaceAndPath(HeavyInventories.MOD_ID, "open_config");
    
    public static final StreamCodec<FriendlyByteBuf, OpenConfigPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> buf.writeUtf(packet.configType()),
        buf -> new OpenConfigPacket(buf.readUtf())
    );

    public static final CustomPacketPayload.Type<OpenConfigPacket> OPEN_CONFIG_PACKET_TYPE = 
        new CustomPacketPayload.Type<>(OPEN_CONFIG_PACKET_ID);

    public record OpenConfigPacket(String configType) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return OPEN_CONFIG_PACKET_TYPE;
        }
    }

    private static boolean payloadRegistered = false;


    /**
     * Call this during mod initialization to register the payload type.
     * This must be called from HeavyInventoriesFabricBase during mod initialization,
     * before any network communication happens.
     */
    public static void registerPayloadType() {
        if (!payloadRegistered) {
            PayloadTypeRegistry.clientboundPlay().register(OPEN_CONFIG_PACKET_TYPE, STREAM_CODEC);
            payloadRegistered = true;
            
        }
    }

    @Override
    public void openClientConfig() {
        if (isClientSide()) {
            FabricClientHooks.openConfig("client");
        }
    }

    @Override
    public void openServerConfig() {
        if (isClientSide()) {
            FabricClientHooks.openConfig("server");
        }
    }

    @Override
    public void openCommonConfig() {
        if (isClientSide()) {
            FabricClientHooks.openConfig("common");
        }
    }

    @Override
    public boolean isClientSide() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public void sendOpenConfigPacket(Object playerId, String configType) {
        if (playerId instanceof ServerPlayer player) {
            OpenConfigPacket packet = new OpenConfigPacket(configType);
            ServerPlayNetworking.send(player, packet);
        }
    }
}
