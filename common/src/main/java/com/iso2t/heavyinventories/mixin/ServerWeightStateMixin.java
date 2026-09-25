package com.iso2t.heavyinventories.mixin;

import com.iso2t.heavyinventories.server.ServerStateAccess;
import com.iso2t.heavyinventories.server.ServerWeightState;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecraftServer.class)
public abstract class ServerWeightStateMixin implements ServerStateAccess {
    @Unique private final ServerWeightState heavyinventories$weightState = new ServerWeightState();
    @Override public ServerWeightState heavyinventories$getWeightState() { return heavyinventories$weightState; }
}
