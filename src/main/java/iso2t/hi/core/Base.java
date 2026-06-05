package iso2t.hi.core;

import iso2t.hi.client.ClientEvents;
import iso2t.hi.client.WeightHudOverlay;
import iso2t.hi.core.registries.DataAttachments;
import iso2t.hi.server.ServerEvents;
import lombok.Getter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public abstract class Base implements HeavyInventories {

	static HeavyInventories INSTANCE;

	@Getter
	private final IEventBus modEventBus;

	@Getter
	private final ModContainer modContainer;

	public Base (IEventBus modEventBus, ModContainer modContainer) {
		if (INSTANCE != null) throw new IllegalStateException(String.format("%s has already been initialized.", HeavyInventories.NAME));
		INSTANCE = this;

		this.modEventBus = modEventBus;
		this.modContainer = modContainer;

		register();
		//registerEvents();
	}

	private void register () {
		DataAttachments.REGISTRY.register(getModEventBus());
	}

	private void registerEvents() {
		NeoForge.EVENT_BUS.register(ClientEvents.class);
		NeoForge.EVENT_BUS.register(WeightHudOverlay.class);
		NeoForge.EVENT_BUS.register(ServerEvents.class);
	}

	@Override
	public Collection<ServerPlayer> getPlayers () {
		var server = getCurrentServer();

		if (server != null) {
			return server.getPlayerList().getPlayers();
		}

		return Collections.emptyList();
	}

	@Nullable
	@Override
	public MinecraftServer getCurrentServer () {
		return ServerLifecycleHooks.getCurrentServer();
	}

}
