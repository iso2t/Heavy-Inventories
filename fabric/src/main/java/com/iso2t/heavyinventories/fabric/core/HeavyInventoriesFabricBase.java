package com.iso2t.heavyinventories.fabric.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.server.weight.WeightPackReloadListener;
import com.iso2t.heavyinventories.ModBase;
import com.iso2t.heavyinventories.fabric.enchantments.ModEnchantmentEffects;
import com.iso2t.heavyinventories.fabric.hooks.ModHooks;
import com.iso2t.heavyinventories.fabric.platform.FabricConfigScreenHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class HeavyInventoriesFabricBase extends ModBase {

    private volatile MinecraftServer currentServer;

    public HeavyInventoriesFabricBase() {
        super();
        DataResourceLoader.get().registerReloadListener(WeightPackReloadListener.ID, new WeightPackReloadListener());
        ServerLifecycleEvents.SERVER_STARTED.register(ServerWeightState::start);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);

        FabricConfigScreenHelper.registerPayloadType();

        ModHooks.registerHooks();
        ModEnchantmentEffects.register();
    }

    @Nullable
    @Override
    public MinecraftServer getCurrentServer() {
        return currentServer;
    }

    @Override
    public List<String> getModIds() {
        return FabricLoader.getInstance().getAllMods().stream().map(mod -> mod.getMetadata().getId()).toList();
    }

}
