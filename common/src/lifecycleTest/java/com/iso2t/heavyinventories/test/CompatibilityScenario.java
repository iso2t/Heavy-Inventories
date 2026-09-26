package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.events.PlayerEvents;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.test.mixin.FluidTestAccess;
import com.iso2t.heavyinventories.test.mixin.FluidTravelTestAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Runs after MovementScenario's deterministic capacity and item definitions are installed.
 */
public final class CompatibilityScenario {
	public static void run (ServerPlayer player) {
		var holder = PlayerHolder.getOrCreate(player);
		player.removeAllEffects();
		// Fast scripted respawns may precede the client's first full movement tick.
		if (((FluidTestAccess) player).heavyinventories$isFirstTick()) player.baseTick();
		player.getInventory().setItem(0, new ItemStack(Items.STONE, 15));
		player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 1, 1));
		PlayerEvents.onPlayerTick(player);
		require(holder.getMaxWeight() == 1200, "Short Strength effect was not applied");
		((FluidTravelTestAccess) player).heavyinventories$tickEffects();
		PlayerEvents.onPlayerTick(player);
		require(!player.hasEffect(MobEffects.STRENGTH) && holder.getMaxWeight() == 1000, "Strength expiry retained capacity");

		var boat = EntityType.OAK_BOAT.create(player.level(), EntitySpawnReason.COMMAND);
		require(boat != null, "Boat creation failed");
		boat.setPos(player.position());
		try {
			require(player.startRiding(boat, true, false), "Test player did not mount");
			MovementScenario.checkImpulse(player, 1);
			require(!holder.preventsGroundJump(), "Mounted player got ground-jump penalty");
			var food = (com.iso2t.heavyinventories.test.mixin.FoodDataTestAccess) player.getFoodData();
			float beforeRiding = food.heavyinventories$getExhaustion();
			player.checkMovementStatistics(0, 0, 10);
			require(food.heavyinventories$getExhaustion() == beforeRiding, "Riding consumed movement exhaustion");
		} finally {
			player.stopRiding();
			boat.discard();
		}
		MovementScenario.checkImpulse(player, 0.2);

		double baseSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
		player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 0));
		double boosted = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
		require(boosted > baseSpeed, "Speed effect did not modify vanilla movement");
		checkSpeedInput(player, boosted, holder.getWalkingMultiplier());
		player.removeEffect(MobEffects.SPEED);
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0));
		double slowed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
		require(slowed < baseSpeed, "Slowness effect did not modify vanilla movement");
		checkSpeedInput(player, slowed, holder.getWalkingMultiplier());
		player.removeEffect(MobEffects.SLOWNESS);
		checkDeepLava(player);
		player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 1200, 1));
		PlayerEvents.onPlayerTick(player);
		HeavyInventories.LOGGER.info("BASELINE COMPATIBILITY PASSED: Strength expiry, mounted/dismounted movement, Speed/Slowness composition, placed deep-lava physics and Slow Falling");
	}

	private static void checkSpeedInput (ServerPlayer player, double speed, double multiplier) {
		var motion = player.getDeltaMovement();
		player.setDeltaMovement(Vec3.ZERO);
		player.moveRelative((float) speed, new Vec3(1, 0, 1));
		require(Math.abs(player.getDeltaMovement().horizontalDistance() - speed * multiplier) < 0.000001, "Encumbrance replaced vanilla speed modifier");
		player.setDeltaMovement(motion);
	}

	private static void checkDeepLava (ServerPlayer player) {
		var level = player.level();
		var originalPosition = player.position();
		// Test at build height inside the already loaded player chunk. Restore every touched block.
		var center = new BlockPos((player.getBlockX() >> 4) * 16 + 8, level.getMaxY() - 12, (player.getBlockZ() >> 4) * 16 + 8);
		var original = new java.util.LinkedHashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
		var access = (FluidTestAccess) player;
		var travel = (FluidTravelTestAccess) player;
		try {
			for (var pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 2, 1))) {
				var key = pos.immutable();
				original.put(key, level.getBlockState(key));
				level.setBlock(key, Blocks.LAVA.defaultBlockState(), 2);
			}
			player.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
			access.heavyinventories$updateFluid();
			require(player.isInLava() && player.getFluidHeight(net.minecraft.tags.FluidTags.LAVA) > 0.4, "Test did not enter deep lava: height=" + player.getFluidHeight(net.minecraft.tags.FluidTags.LAVA) + ", ticks=" + player.tickCount + ", block=" + level.getFluidState(center));
			double[] acceleration = new double[3];
			int[] counts = { 5, 9, 15 };
			double[] swim = { 1, 1, 0.5 };
			for (int i = 0; i < counts.length; i++) {
				player.getInventory().setItem(0, new ItemStack(Items.STONE, counts[i]));
				PlayerEvents.onPlayerTick(player);
				MovementScenario.checkImpulse(player, swim[i]);
				player.setDeltaMovement(Vec3.ZERO);
				travel.heavyinventories$travelInFluid(Vec3.ZERO);
				acceleration[i] = player.getDeltaMovement().y;
			}
			require(acceleration[0] < 0 && Math.abs(acceleration[1] / acceleration[0] - 1) < 0.00001 && Math.abs(acceleration[2] / acceleration[0] - 2) < 0.00001, "Deep-lava gravity did not use encumbrance multipliers");
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
			player.setDeltaMovement(Vec3.ZERO);
			travel.heavyinventories$travelInFluid(Vec3.ZERO);
			require(Math.abs(player.getDeltaMovement().y) < Math.abs(acceleration[2]), "Encumbrance replaced Slow Falling gravity");
		} finally {
			player.removeEffect(MobEffects.SLOW_FALLING);
			original.forEach((pos, block) -> level.setBlock(pos, block, 2));
			player.setPos(originalPosition);
			player.setDeltaMovement(Vec3.ZERO);
			player.clearFire();
			access.heavyinventories$updateFluid();
		}
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
