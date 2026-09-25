package com.iso2t.heavyinventories.neoforge.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.config.ConfigScreens;
import com.iso2t.heavyinventories.neoforge.client.NeoForgeClientHooks;
import com.iso2t.heavyinventories.platform.services.IConfigScreenHelper;

import com.iso2t.heavyinventories.network.PlayerWeightPayload;
import com.iso2t.heavyinventories.network.ItemWeightsPayload;
import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import com.iso2t.heavyinventories.server.ServerConfiguration;

public class NeoForgeConfigScreenHelper implements IConfigScreenHelper {

    public static final Identifier OPEN_CONFIG_PACKET_ID = Identifier.fromNamespaceAndPath(HeavyInventories.MOD_ID, "open_config");

    public static final StreamCodec<FriendlyByteBuf, OpenConfigPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> buf.writeUtf(packet.configType()),
        buf -> new OpenConfigPacket(buf.readUtf())
    );

    public record OpenConfigPacket(String configType) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static final Type<OpenConfigPacket> TYPE = new Type<>(OPEN_CONFIG_PACKET_ID);
    }

    /**
     * Registers the payload type during mod initialization.
     * This event handler is registered statically.
     */
    @EventBusSubscriber(modid = HeavyInventories.MOD_ID)
    public static class NetworkHandler {
        @SubscribeEvent
        public static void registerPayload(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar("3");
            registrar.playToClient(PlayerWeightPayload.TYPE, PlayerWeightPayload.CODEC,
                    (packet, context) -> context.enqueueWork(() -> NeoForgeClientHooks.receiveWeight(packet)));
            registrar.playToClient(ItemWeightsPayload.TYPE, ItemWeightsPayload.CODEC,
                    (packet, context) -> context.enqueueWork(() -> com.iso2t.heavyinventories.client.ClientWeightData.accept(packet)));
            registrar.playToServer(ServerConfigUpdatePayload.TYPE, ServerConfigUpdatePayload.CODEC,
                    (packet, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) ServerConfiguration.update(player, packet);
                    }));

            registrar.playToClient(OpenConfigPacket.TYPE, STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        switch (packet.configType()) {
                            case "client" -> ConfigScreens.openClientConfig();
                            case "server" -> ConfigScreens.openServerConfig();
                            case "common" -> ConfigScreens.openCommonConfig();
                        }
                    });
                });
        }
    }

    @Override
    public void openClientConfig() {
        if (isClientSide()) {
            NeoForgeClientHooks.openConfig("client");
        }
    }

    @Override
    public void openServerConfig() {
        if (isClientSide()) {
            NeoForgeClientHooks.openConfig("server");
        }
    }

    @Override
    public void openCommonConfig() {
        if (isClientSide()) {
            NeoForgeClientHooks.openConfig("common");
        }
    }

    @Override
    public boolean isClientSide() {
        return FMLEnvironment.getDist() == Dist.CLIENT;
    }

    @Override
    public void sendOpenConfigPacket(Object playerId, String configType) {
        if (playerId instanceof ServerPlayer player) {
            OpenConfigPacket packet = new OpenConfigPacket(configType);
            player.connection.send(packet);
        }
    }
}
