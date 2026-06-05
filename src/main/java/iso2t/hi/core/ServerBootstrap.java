package iso2t.hi.core;

import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = HeavyInventories.MODID, dist = Dist.DEDICATED_SERVER)
public class ServerBootstrap extends Base {

	public ServerBootstrap (IEventBus modEventBus, ModContainer modContainer) {
		super(modEventBus, modContainer);
	}

	@Override
	public Level getClientLevel () {
		return null;
	}

}
