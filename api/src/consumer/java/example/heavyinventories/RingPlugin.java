package example.heavyinventories;

import com.iso2t.heavyinventories.api.client.*;
import com.iso2t.heavyinventories.api.plugin.HIPlugin;
import net.minecraft.resources.Identifier;

@HIPlugin(HIPlugin.Side.CLIENT)
public final class RingPlugin implements HeavyInventoriesClientPlugin {
	@Override
	public Identifier id () {
		return Identifier.fromNamespaceAndPath("example", "ring");
	}

	@Override
	public void register (ClientPluginRegistration registration) {
		registration.hud().owner(id(), HudElement.RING, 0, new HudIntegration() {
			@Override
			public HudLayout layout (HudContext context, HudElement element, HudLayout original) {
				var bounds = original.bounds();
				return new HudLayout(new HudBounds(bounds.x() + 24, bounds.y(), bounds.width(), bounds.height()), original.visible(), 0);
			}
		});
	}
}
