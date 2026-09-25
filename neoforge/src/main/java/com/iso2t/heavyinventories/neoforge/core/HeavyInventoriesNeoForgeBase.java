package com.iso2t.heavyinventories.neoforge.core;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforgespi.language.IModInfo;

import com.iso2t.heavyinventories.ModBase;
import com.iso2t.heavyinventories.neoforge.enchantments.ModEnchantmentEffects;
import com.iso2t.heavyinventories.neoforge.hooks.ModHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class HeavyInventoriesNeoForgeBase extends ModBase {

    public HeavyInventoriesNeoForgeBase(ModContainer modContainer, IEventBus modEventBus) {
        super();
        NeoForge.EVENT_BUS.addListener(ModHooks::hookServerStart);

        NeoForge.EVENT_BUS.addListener(ModHooks::hookServerTick);

        NeoForge.EVENT_BUS.addListener(ModHooks::hookCommands);


        ModEnchantmentEffects.register(modEventBus);

    }

    @Nullable
    @Override
    public MinecraftServer getCurrentServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    @Override
    public List<String> getModIds() {
        return ModList.get().getMods().stream().map(IModInfo::getModId).toList();
    }

}
