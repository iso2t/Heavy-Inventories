package com.iso2t.heavyinventories.api.weight;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Shared aggregation with server-owned or synchronized item definitions supplied by the caller.
 */
public final class StackWeight {

	public static final int   MAX_DEPTH   = 16;
	public static final int   MAX_VISITS  = 4096;
	public static final float TOO_COMPLEX = Float.MAX_VALUE;

	private StackWeight () {
	}

	public record Result(float weight, boolean complete) {
	}

	public static Result of (ItemStack stack, ToDoubleFunction<Identifier> definitions) {
		return total(List.of(stack), definitions);
	}

	public static Result total (List<ItemStack> stacks, ToDoubleFunction<Identifier> definitions) {
		var calculation = new Calculation(definitions);
		double total = 0;
		for (var stack : stacks) {
			if (stack.isEmpty()) continue;
			total += calculation.visit(stack, 0);
			if (!calculation.complete || !Double.isFinite(total) || total >= TOO_COMPLEX) return new Result(TOO_COMPLEX, false);
		}
		return new Result((float) total, true);
	}

	private static final class Calculation {
		private final ToDoubleFunction<Identifier> definitions;
		private       int                          remaining = MAX_VISITS;
		private       boolean                      complete  = true;

		private Calculation (ToDoubleFunction<Identifier> definitions) {
			this.definitions = definitions;
		}

		private double visit (ItemInstance stack, int depth) {
			if (!complete) return 0;
			if (--remaining < 0 || depth > MAX_DEPTH) {
				complete = false;
				return 0;
			}
			if (stack.count() <= 0 || stack.typeHolder().value() == Items.AIR) return 0;
			double weight = definitions.applyAsDouble(BuiltInRegistries.ITEM.getKey(stack.typeHolder().value()));
			if (!Double.isFinite(weight) || weight < 0) {
				complete = false;
				return 0;
			}
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
			return weight * stack.count();
		}
	}
}
