package example.heavyinventories;

import com.iso2t.heavyinventories.api.ServerWeights;
import com.iso2t.heavyinventories.api.WeightResult;
import com.iso2t.heavyinventories.api.plugin.HIPlugin;
import com.iso2t.heavyinventories.api.plugin.HeavyInventoriesPlugin;
import com.iso2t.heavyinventories.api.plugin.PluginRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

@HIPlugin
public final class WeightPlugin implements HeavyInventoriesPlugin {
	private ServerWeights weights;

	@Override
	public Identifier id () {
		return Identifier.fromNamespaceAndPath("example", "weights");
	}

	@Override
	public void register (PluginRegistration registration) {
		weights = registration.weights();
		registration.capacity(Identifier.fromNamespaceAndPath("example", "bonus"), player -> 25);
	}

	public WeightResult stone (MinecraftServer server) {
		return weights.item(server, Identifier.fromNamespaceAndPath("minecraft", "stone"));
	}
}
