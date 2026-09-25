package com.iso2t.heavyinventories.neoforge.config;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Displays the current server's values; saving is a permission-checked request to that server. */
public class ModServerConfig {
    private static ConfigBuilder builder;

    public static void init() {
        builder = ConfigBuilder.create().setParentScreen(null)
                .setTitle(Component.translatable("title.heavyinventories.config.server"));
        var category = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.general"));
        var entries = builder.entryBuilder();
        var player = Minecraft.getInstance().player;
        if (player == null || !PlayerHolder.getOrCreate(player).hasServerState()) {
            category.addEntry(entries.startTextDescription(Component.translatable("config.heavyinventories.unavailable")).build());
            return;
        }
        var holder = PlayerHolder.getOrCreate(player);
        float initial = holder.getBaseMaxWeight();
        long revision = holder.serverRevision();
        float[] edited = {initial};
        var field = entries.startFloatField(Component.translatable("option.heavyinventories.starting_max_weight"), initial)
                .setDefaultValue(ServerSettings.DEFAULT.startingWeight())
                .setMin(Float.MIN_VALUE).setMax(ServerSettings.MAX_VALUE)
                .setErrorSupplier(value -> {
                    try { new ServerSettings(value); return java.util.Optional.empty(); }
                    catch (IllegalArgumentException e) { return java.util.Optional.of(Component.literal(e.getMessage())); }
                })
                .setSaveConsumer(value -> edited[0] = value).build();
        field.setEditable(holder.canEditServerConfig());
        category.addEntry(field);
        category.addEntry(entries.startTextDescription(Component.translatable(
                holder.canEditServerConfig() ? "config.heavyinventories.edit_help" : "config.heavyinventories.read_only")).build());
        builder.setSavingRunnable(() -> {
            if (edited[0] == initial) return;
            var request = new ServerConfigUpdatePayload(edited[0], revision);
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) connection.send(new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(request));
        });
    }

    public static ConfigBuilder getBuilder() {
        if (builder == null) init();
        return builder;
    }
}
