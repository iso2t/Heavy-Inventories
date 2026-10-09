package com.iso2t.heavyinventories.weight;

import com.iso2t.heavyinventories.api.provider.ContainerContentsProvider;
import com.iso2t.heavyinventories.integration.CommonPlugins;
import com.iso2t.heavyinventories.integration.GameplayProviders;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * Shared aggregation with server-owned or synchronized item definitions supplied by the caller.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StackWeight {

	public static final int   MAX_DEPTH   = 16;
	public static final int   MAX_VISITS  = 4096;
	public static final float TOO_COMPLEX = Float.MAX_VALUE;

	public record Result(float weight, boolean complete) {

	}

	public static Result of (ItemStack stack, ToDoubleFunction<Identifier> definitions) {
		return total(List.of(stack), definitions);
	}

	public static Result of (ItemStack stack, ToDoubleFunction<Identifier> definitions, Level level) {
		return total(List.of(stack), definitions, level);
	}

	public static Result total (List<ItemStack> stacks, ToDoubleFunction<Identifier> definitions) {
		return total(stacks, definitions, null);
	}

	public static Result total (List<ItemStack> stacks, ToDoubleFunction<Identifier> definitions, Level level) {
		return total(stacks, definitions, level, CommonPlugins.INSTANCE.providers());
	}

	public static Result total (List<ItemStack> stacks, ToDoubleFunction<Identifier> definitions, Level level, GameplayProviders providers) {
		GameplayProviders.checkCalculation();
		var calculation = new Calculation(definitions, level, providers);
		double total = 0;
		for (var stack : stacks) {
			total += calculation.visit(stack, 0);
			if (!calculation.complete || !Double.isFinite(total) || total >= TOO_COMPLEX) return new Result(TOO_COMPLEX, false);
		}
		return new Result((float) total, true);
	}

	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	private static final class Calculation {

		private final ToDoubleFunction<Identifier> definitions;
		private final Level                        level;
		private final GameplayProviders            providers;
		private final Set<ItemInstance>            path      = Collections.newSetFromMap(new IdentityHashMap<>());
		private       int                          remaining = MAX_VISITS;
		private       boolean                      complete  = true;

		private double visit (ItemInstance stack, int depth) {
			if (!complete) return 0;
			if (stack == null || --remaining < 0 || depth > MAX_DEPTH || path.contains(stack)) {
				complete = false;
				return 0;
			}
			if (stack.count() <= 0 || stack.typeHolder().value() == Items.AIR) return 0;
			double weight = definitions.applyAsDouble(BuiltInRegistries.ITEM.getKey(stack.typeHolder().value()));
			if (!Double.isFinite(weight) || weight < 0) {
				complete = false;
				return 0;
			}
			path.add(stack);
			try {
				var custom = providers.container(BuiltInRegistries.ITEM.getKey(stack.typeHolder().value()));
				if (custom != null) {
					if (level == null) {
						complete = false;
						return 0;
					}
					var mutable = stack instanceof ItemStack item ? item : ((ItemStackTemplate) stack).create();
					path.add(mutable);
					try (var contents = new Contents(this, depth + 1)) {
						boolean supplied = GameplayProviders.callback(() -> custom.registration().provider().collect(level, mutable, contents));
						if (!supplied) complete = false;
						weight += contents.weight;
					} catch (RuntimeException | LinkageError e) {
						complete = false;
						GameplayProviders.failure("container", custom.id(), e);
					} finally {
						path.remove(mutable);
					}
				} else {
					var container = stack.get(DataComponents.CONTAINER);
					if (container != null) {
						for (var child : container.nonEmptyItems()) {
							weight += visit(child, depth + 1);
							if (!complete) return 0;
						}
					}
					var bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
					if (bundle != null) {
						for (var child : bundle.items()) {
							weight += visit(child, depth + 1);
							if (!complete) return 0;
						}
					}
				}
			} finally {
				path.remove(stack);
			}

			double total = weight * stack.count();
			if (!Double.isFinite(total) || total >= TOO_COMPLEX) complete = false;
			return total;
		}

	}

	private static final class Contents implements ContainerContentsProvider.ContentsSink, AutoCloseable {

		private final Thread      thread = Thread.currentThread();
		private final int         depth;
		private       Calculation calculation;
		private       double      weight;

		private Contents (Calculation calculation, int depth) {
			this.calculation = calculation;
			this.depth = depth;
		}

		@Override
		public boolean accept (ItemStack stack) {
			if (calculation == null || thread != Thread.currentThread()) throw new IllegalStateException("Contents sink is callback-scoped");
			if (!calculation.complete) return false;
			weight += calculation.visit(stack, depth);
			if (!Double.isFinite(weight) || weight >= TOO_COMPLEX) calculation.complete = false;
			return calculation.complete;
		}

		@Override
		public void close () {
			calculation = null;
		}

	}

}
