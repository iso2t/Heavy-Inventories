package com.iso2t.heavyinventories.neoforge.integration;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.plugin.HIPlugin;
import com.iso2t.heavyinventories.integration.PluginCandidate;
import com.iso2t.heavyinventories.integration.PluginMetadata;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NeoForgePluginDiscovery {

	public static <T> List<PluginCandidate<T>> find (Class<T> type, HIPlugin.Side side) {
		var mods = ModList.get();
		var candidates = new ArrayList<PluginCandidate<T>>();
		for (var file : mods.getModFiles()) {
			var owners = file.getMods().stream().map(mod -> mod.getModId()).collect(Collectors.toSet());
			for (var annotation : file.getFile().getScanResult().getAnnotations()) {
				if (!annotation.annotationType().equals(Type.getType(HIPlugin.class))) continue;
				var name = annotation.clazz().getClassName();
				try {
					var declaredSide = annotation.annotationData().get("value");
					var pluginSide = declaredSide == null ? HIPlugin.Side.COMMON : HIPlugin.Side.valueOf(((ModAnnotation.EnumHolder) declaredSide).value());
					if (pluginSide != side) continue;
					var metadata = PluginMetadata.read(name, NeoForgePluginDiscovery.class.getClassLoader()).orElseThrow();
					if (metadata.side() != side) continue;
					if (!metadata.dependenciesPresent(mods::isLoaded)) {
						HeavyInventories.LOGGER.debug("Skipping HI plugin {} from {}: required integration mod is absent", name, owners);
						continue;
					}
					candidates.add(new PluginCandidate<>(owners, name, () -> instantiate(name, type)));
				} catch (RuntimeException | LinkageError e) {
					if (side == HIPlugin.Side.COMMON) throw new IllegalStateException("Cannot discover HI plugin " + name + " from " + owners, e);
					HeavyInventories.LOGGER.error("Disabled HI client plugin {} from {} during discovery", name, owners, e);
				}
			}
		}
		return List.copyOf(candidates);
	}

	private static <T> T instantiate (String name, Class<T> type) {
		try {
			return Class.forName(name, true, NeoForgePluginDiscovery.class.getClassLoader()).asSubclass(type).getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("HI plugin must implement " + type.getSimpleName() + " and have a public no-argument constructor: " + name, e);
		}
	}

}
