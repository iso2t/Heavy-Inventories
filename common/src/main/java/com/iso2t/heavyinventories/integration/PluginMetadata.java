package com.iso2t.heavyinventories.integration;

import com.iso2t.heavyinventories.api.plugin.HIPlugin;
import org.objectweb.asm.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Reads only the class named by a loader entrypoint or annotation scan, without initializing it.
 */
public record PluginMetadata(HIPlugin.Side side, Set<String> requiredMods) {

	public PluginMetadata {
		requiredMods = Set.copyOf(requiredMods);
	}

	public boolean dependenciesPresent (Predicate<String> loaded) {
		return requiredMods.stream().allMatch(loaded);
	}

	public static Optional<PluginMetadata> read (String className, ClassLoader loader) {
		var required = new HashSet<String>();
		var side = new HIPlugin.Side[] { HIPlugin.Side.COMMON };
		boolean[] annotated = { false };
		try (var input = loader.getResourceAsStream(className.replace('.', '/') + ".class")) {
			if (input == null) throw new IllegalArgumentException("Missing plugin class: " + className);
			new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
				@Override
				public AnnotationVisitor visitAnnotation (String descriptor, boolean visible) {
					if (!descriptor.equals(Type.getDescriptor(HIPlugin.class))) return null;
					annotated[0] = true;
					return new AnnotationVisitor(Opcodes.ASM9) {
						@Override
						public void visitEnum (String name, String descriptor, String value) {
							if (name.equals("value")) side[0] = HIPlugin.Side.valueOf(value);
						}

						@Override
						public AnnotationVisitor visitArray (String name) {
							if (!name.equals("requires")) return null;
							return new AnnotationVisitor(Opcodes.ASM9) {
								@Override
								public void visit (String ignored, Object value) {
									if (!(value instanceof String mod) || !mod.matches("[a-z][a-z0-9_-]*")) throw new IllegalArgumentException("Invalid required mod: " + value);
									required.add(mod);
								}
							};
						}
					};
				}
			}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot inspect plugin " + className, e);
		}
		return annotated[0] ? Optional.of(new PluginMetadata(side[0], required)) : Optional.empty();
	}

}
