package com.iso2t.heavyinventories.fabric.integration;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.plugin.HIPlugin;
import com.iso2t.heavyinventories.integration.PluginCandidate;
import com.iso2t.heavyinventories.integration.PluginMetadata;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FabricPluginDiscovery {

	public static <T> List<PluginCandidate<T>> find (String entrypoint, Class<T> type, HIPlugin.Side side) {
		var loader = FabricLoader.getInstance();
		var candidates = new ArrayList<PluginCandidate<T>>();
		for (var entry : loader.getEntrypointContainers(entrypoint, type)) {
			var owner = entry.getProvider().getMetadata().getId();
			var name = entry.getDefinition();
			try {
				var metadata = PluginMetadata.read(name, FabricPluginDiscovery.class.getClassLoader());
				if (metadata.isPresent()) {
					if (metadata.get().side() != side) throw new IllegalArgumentException("HI plugin side does not match entrypoint " + entrypoint);
					if (!metadata.get().dependenciesPresent(loader::isModLoaded)) {
						HeavyInventories.LOGGER.debug("Skipping HI plugin {} from {}: required integration mod is absent", name, owner);
						continue;
					}
				}
				candidates.add(new PluginCandidate<>(Set.of(owner), name, entry::getEntrypoint));
			} catch (RuntimeException | LinkageError e) {
				if (side == HIPlugin.Side.COMMON) throw new IllegalStateException("Cannot discover HI plugin " + name + " from " + owner, e);
				HeavyInventories.LOGGER.error("Disabled HI client plugin {} from {} during discovery", name, owner, e);
			}
		}
		return List.copyOf(candidates);
	}

}
