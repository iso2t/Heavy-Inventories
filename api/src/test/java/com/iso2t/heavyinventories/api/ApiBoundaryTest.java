package com.iso2t.heavyinventories.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ApiBoundaryTest {
	@Test
	void commonContractsHaveNoClientOrImplementationDependencies () throws Exception {
		var root = Path.of(WeightResult.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		try (var paths = Files.walk(root)) {
			for (var path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
				String name = root.relativize(path).toString().replace('\\', '/');
				String bytecode = new String(Files.readAllBytes(path), StandardCharsets.ISO_8859_1);
				assertFalse(bytecode.contains("com/iso2t/easyconfig/"), name);
				assertFalse(bytecode.contains("net/fabricmc/"), name);
				assertFalse(bytecode.contains("net/neoforged/"), name);
				String withoutApi = bytecode.replace("com/iso2t/heavyinventories/api/", "");
				assertFalse(withoutApi.contains("com/iso2t/heavyinventories/"), name);
				if (!name.contains("/api/client/")) {
					assertFalse(bytecode.contains("net/minecraft/client/"), name);
					assertFalse(bytecode.contains("com/iso2t/heavyinventories/api/client/"), name);
				}
			}
		}
	}

	@Test
	void commonPublicSignaturesResolveWithoutClientContracts () throws Exception {
		var isolated = new ClassLoader(getClass().getClassLoader()) {
			@Override
			protected Class<?> loadClass (String name, boolean resolve) throws ClassNotFoundException {
				if (name.startsWith("net.minecraft.client.") || name.startsWith("com.iso2t.heavyinventories.api.client.")) {
					throw new ClassNotFoundException("Client class unavailable: " + name);
				}
				if (!name.startsWith("com.iso2t.heavyinventories.api.")) return super.loadClass(name, resolve);
				synchronized (getClassLoadingLock(name)) {
					var loaded = findLoadedClass(name);
					if (loaded == null) {
						try (var input = getResourceAsStream(name.replace('.', '/') + ".class")) {
							if (input == null) throw new ClassNotFoundException(name);
							byte[] bytes = input.readAllBytes();
							loaded = defineClass(name, bytes, 0, bytes.length);
						} catch (IOException e) {
							throw new ClassNotFoundException(name, e);
						}
					}
					if (resolve) resolveClass(loaded);
					return loaded;
				}
			}
		};
		for (String name : new String[] { "ServerWeights", "plugin.HeavyInventoriesPlugin", "plugin.PluginRegistration", "plugin.HIPlugin", "provider.InventoryProvider", "provider.ContainerContentsProvider", "provider.CapacityProvider" }) {
			var type = Class.forName("com.iso2t.heavyinventories.api." + name, false, isolated);
			assertDoesNotThrow(type::getDeclaredMethods, name);
			assertDoesNotThrow(type::getDeclaredConstructors, name);
		}
	}
}
