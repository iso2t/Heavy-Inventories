package iso2t.hi.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public interface HeavyInventories {

	String NAME = "Heavy Inventories";
	String MODID = "heavyinventories";

	Logger LOGGER = LoggerFactory.getLogger(NAME);

	static HeavyInventories getInstance () {
		return Base.INSTANCE;
	}

	Collection<ServerPlayer> getPlayers ();

	Level getClientLevel ();

	MinecraftServer getCurrentServer ();

}
