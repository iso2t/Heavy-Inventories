package com.iso2t.heavyinventories.api.player;

import com.iso2t.heavyinventories.server.ServerWeightState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Adds exhaustion only at vanilla's server movement/jump call sites, never to unrelated food costs.
 */
public final class PlayerExhaustion {
	private PlayerExhaustion () {
	}

	public static float movement (ServerPlayer player, float vanillaCost, double dx, double dy, double dz) {
		var holder = PlayerHolder.getOrCreate(player);
		var settings = ServerWeightState.of(player.level().getServer()).settings();
		if (!settings.effects().exhaustion().enabled() || holder.movementExempt() || holder.movementExhaustionSuppressed()) return vanillaCost;
		boolean submerged = player.isSwimming() || player.isEyeInFluid(FluidTags.WATER);
		boolean water = submerged || player.isInWater();
		boolean walking = !water && !player.isInLava() && player.onGround() && !player.onClimbable() && !player.isSprinting();
		if (!water && !walking && !(player.onGround() && player.isSprinting() && !player.onClimbable())) return vanillaCost;
		if (water && hasExternalFluidMotion(player)) {
			holder.suppressMovementExhaustion();
			return vanillaCost;
		}
		var input = player.getLastClientInput();
		var intent = player.getLastClientMoveIntent();
		if (submerged) {
			// Swimming posture follows the view direction; upright ascent/descent uses jump/sneak.
			double vertical = input.jump() == input.shift() ? 0 : input.jump() ? 1 : -1;
			if (player.isSwimming() && input.forward() != input.backward()) {
				var look = player.getLookAngle();
				double forward = (input.forward() ? 1 : -1) / (input.left() != input.right() ? Math.sqrt(2) : 1);
				intent = intent.add(look.subtract(Vec3.directionFromRotation(0, player.getYRot())).scale(forward));
			}
			intent = intent.add(0, vertical, 0);
		}
		if (intent.lengthSqr() < 1.0E-8) return vanillaCost;
		var movement = new Vec3(dx, submerged ? dy : 0, dz);
		double distance = movement.length();
		double voluntary = voluntaryDistance(movement, intent);
		if (voluntary <= 0) return vanillaCost;
		var effects = EncumbranceEffects.calculate(holder.getWeight(), holder.getMaxWeight(), settings.walkingMode(), settings.effects(), EncumbranceEffects.Fluid.NONE, false, false);
		return vanillaCost + extraMovementCost(vanillaCost, distance, voluntary, effects.exhaustionMultiplier(), walking ? effects.walkingCostPerBlock() : 0);
	}

	public static float jump (ServerPlayer player, float vanillaCost, double previousVerticalSpeed) {
		var holder = PlayerHolder.getOrCreate(player);
		var settings = ServerWeightState.of(player.level().getServer()).settings();
		if (!settings.effects().exhaustion().enabled() || holder.movementExempt() || player.getDeltaMovement().y <= previousVerticalSpeed) return vanillaCost;
		var effects = EncumbranceEffects.calculate(holder.getWeight(), holder.getMaxWeight(), settings.walkingMode(), settings.effects(), EncumbranceEffects.Fluid.NONE, false, false);
		return vanillaCost * effects.exhaustionMultiplier();
	}

	/**
	 * Only progress along active input counts. Sideways/backward drift and idle transport add no cost.
	 */
	public static double voluntaryDistance (Vec3 movement, Vec3 intent) {
		if (!movement.isFinite() || !intent.isFinite() || intent.lengthSqr() < 1.0E-8) return 0;
		return Math.clamp(movement.dot(intent.normalize()), 0, movement.length());
	}

	public static float extraMovementCost (float vanillaCost, double distance, double voluntaryDistance, float multiplier, float walkingCost) {
		if (!Double.isFinite(distance) || !Double.isFinite(voluntaryDistance) || distance <= 0 || voluntaryDistance <= 0) return 0;
		double fraction = Math.clamp(voluntaryDistance / distance, 0, 1);
		return vanillaCost > 0 ? (float) (vanillaCost * (multiplier - 1) * fraction) : (float) (walkingCost * distance * fraction);
	}

	private static boolean hasExternalFluidMotion (ServerPlayer player) {
		var box = player.getBoundingBox().deflate(0.001);
		for (var pos : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ), Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
			var state = player.level().getBlockState(pos);
			if (state.is(Blocks.BUBBLE_COLUMN)) return true;
			var fluid = state.getFluidState();
			if (!fluid.isEmpty() && fluid.getFlow(player.level(), pos).lengthSqr() > 1.0E-8) return true;
		}
		return false;
	}
}
