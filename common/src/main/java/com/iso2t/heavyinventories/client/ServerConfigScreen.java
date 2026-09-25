package com.iso2t.heavyinventories.client;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Displays the current server's values; saving is a permission-checked request to that server. */
public final class ServerConfigScreen {
    private ServerConfigScreen() {}
    public static ConfigBuilder create(java.util.function.Consumer<ServerConfigUpdatePayload> send) {
        var builder = ConfigBuilder.create().setParentScreen(null)
                .setTitle(Component.translatable("title.heavyinventories.config.server"));
        var category = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.general"));
        var entries = builder.entryBuilder();
        var player = Minecraft.getInstance().player;
        if (player == null || !PlayerHolder.getOrCreate(player).hasServerState()) {
            category.addEntry(entries.startTextDescription(Component.translatable("config.heavyinventories.unavailable")).build());
            return builder;
        }
        var holder = PlayerHolder.getOrCreate(player);
        float initial = holder.getBaseMaxWeight();
        long revision = holder.serverRevision();
        float[] edited = {initial};
        var initialMode = holder.walkingMode();
        WalkingMode[] editedMode = {initialMode};
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
        var modeField = entries.startEnumSelector(Component.translatable("option.heavyinventories.walking_mode"), WalkingMode.class, initialMode)
                .setDefaultValue(ServerSettings.DEFAULT.walkingMode())
                .setEnumNameProvider(mode -> Component.translatable("option.heavyinventories.walking_mode." + ((WalkingMode) mode).id()))
                .setTooltip(Component.translatable("option.heavyinventories.walking_mode.tooltip"))
                .setSaveConsumer(mode -> editedMode[0] = mode).build();
        modeField.setEditable(holder.canEditServerConfig());
        category.addEntry(modeField);
        category.addEntry(entries.startTextDescription(Component.translatable(
                holder.canEditServerConfig() ? "config.heavyinventories.edit_help" : "config.heavyinventories.read_only")).build());
        builder.setSavingRunnable(() -> {
            if (edited[0] == initial && editedMode[0] == initialMode) return;
            var request = new ServerConfigUpdatePayload(edited[0], editedMode[0].id(), revision);
            send.accept(request);
        });
        return builder;
    }

}
