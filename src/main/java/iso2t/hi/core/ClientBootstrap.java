package iso2t.hi.core;

import iso2t.hi.client.ClientEvents;
import iso2t.hi.client.WeightHudOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = HeavyInventories.MODID, dist = Dist.CLIENT)
public class ClientBootstrap extends Base {

	public ClientBootstrap (IEventBus modEventBus, ModContainer modContainer) {
		super(modEventBus, modContainer);
		//registerClientEvents();
	}

	private void registerClientEvents () {
		NeoForge.EVENT_BUS.register(ClientEvents.class);
		NeoForge.EVENT_BUS.register(WeightHudOverlay.class);
	}

	@Override
	public Level getClientLevel () {
		return Minecraft.getInstance().level;
	}

}
