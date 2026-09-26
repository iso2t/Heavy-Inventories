package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.enchantment.ModEnchantments;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.test.mixin.FluidTestAccess;
import com.iso2t.heavyinventories.test.mixin.FoodDataTestAccess;
import com.iso2t.heavyinventories.test.mixin.GameModeTestAccess;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.UUID;

/** Exercises the real transformed server methods, without changing the connected test player's state. */
public final class ExhaustionScenario {
	private static final Input FORWARD = new Input(true, false, false, false, false, false, false);

	public static void run (ServerPlayer anchor) {
		var server = anchor.level().getServer();
		var state = ServerWeightState.of(server);
		var savedSettings = state.settings();
		var savedWeights = state.weights();
		var player = new ServerPlayer(server, server.overworld(),
				new GameProfile(UUID.fromString("b80f4e78-1bd2-4b0e-a7de-8e662f03b998"), "ExhaustionTest"), ClientInformation.createDefault());
		var holder = PlayerHolder.getOrCreate(player);
		var water = (FluidTestAccess) player;
		var food = (FoodDataTestAccess) player.getFoodData();
		var weights = new HashMap<>(savedWeights);
		weights.put(BuiltInRegistries.ITEM.getKey(Items.STONE), 100f);
		weights.put(BuiltInRegistries.ITEM.getKey(Items.IRON_BOOTS), 0f);
		try {
			state.replace(new ServerSettings(1000, WalkingMode.PROGRESSIVE), weights);
			player.setPos(0.5, server.overworld().getMaxY() + 10, 0.5);
			player.setOnGround(true);
			player.setYRot(0);
			player.setLastClientInput(FORWARD);
			player.getInventory().setItem(0, new ItemStack(Items.STONE, 5));
			holder.update();
			double burden = 1 - Math.sqrt(.5);
			double multiplier = 1 + .5 * burden;
			movement(player, 0, 0, 1, .01 * burden, "half-load walking");
			player.setShiftKeyDown(true);
			movement(player, 0, 0, 1, .01 * burden, "crouched walking");
			player.setShiftKeyDown(false);
			player.setSprinting(true);
			movement(player, 0, 0, 1, .1 * multiplier, "sprinting exactly once");
			jump(player, .2 * multiplier, "sprint jump");
			player.setSprinting(false);
			jump(player, .05 * multiplier, "ordinary jump");
			food.heavyinventories$setExhaustion(0);
			player.causeFoodExhaustion(.3f);
			close(.3, food.heavyinventories$getExhaustion(), "unrelated exhaustion unchanged");
			movement(player, 0, 0, 0, 0, "stationary");
			player.setLastClientInput(Input.EMPTY);
			movement(player, 0, 0, 1, 0, "idle drift");
			player.setLastClientInput(FORWARD);
			movement(player, 1, 0, 0, 0, "sideways drift");
			movement(player, 0, 0, -1, 0, "backward drift");
			food.heavyinventories$setExhaustion(0);
			player.setPos(100, player.getY(), 100);
			holder.update();
			close(0, food.heavyinventories$getExhaustion(), "teleport and idle update");

			player.push(0, 0, .5);
			movement(player, 0, 0, 1, 0, "external push with matching input");
			player.tickCount += 21;
			movement(player, 0, 0, 1, .01 * burden, "normal movement after push grace");
			player.knockback(.5, 0, 1);
			movement(player, 0, 0, 1, 0, "knockback with input");
			player.tickCount += 21;
			player.move(MoverType.PISTON, new Vec3(.1, 0, 0));
			player.setOnGround(true);
			movement(player, 0, 0, 1, 0, "piston transport");
			player.tickCount += 21;
			player.onInsideBubbleColumn(false);
			movement(player, 0, 0, 1, 0, "bubble column propulsion");
			player.tickCount += 21;
			player.getAbilities().flying = true;
			movement(player, 0, 0, 1, 0, "flying exemption");
			player.getAbilities().flying = false;
			player.startFallFlying();
			movement(player, 0, 0, 1, 0, "gliding exemption");
			player.stopFallFlying();
			for (var mode : new GameType[] { GameType.CREATIVE, GameType.SPECTATOR }) {
				((GameModeTestAccess) player.gameMode).heavyinventories$setMode(mode, GameType.SURVIVAL);
				movement(player, 0, 0, 1, 0, mode + " walking exemption");
				player.setSprinting(true);
				movement(player, 0, 0, 1, 0, mode + " sprint immunity");
				jump(player, 0, mode + " jump immunity");
				player.setSprinting(false);
			}
			((GameModeTestAccess) player.gameMode).heavyinventories$setMode(GameType.SURVIVAL, GameType.SPECTATOR);
			// NeoForge retains the prior flight flag until the normal ability update; this fixture does not tick.
			player.getAbilities().flying = false;

			water.heavyinventories$setWater(true);
			player.setSwimming(true);
			player.setOnGround(false);
			movement(player, 0, 0, 1, .01 * multiplier, "swimming exactly once");
			player.setXRot(-90);
			movement(player, 0, 1, 0, .01 * multiplier, "vertical swimming follows view");
			player.setXRot(0);
			player.setLastClientInput(new Input(false, false, false, false, true, false, false));
			movement(player, 0, 1, 0, .01 * multiplier, "swim ascent input");
			player.setLastClientInput(Input.EMPTY);
			movement(player, 0, 0, 1, .01, "passive water preserves vanilla cost only");
			player.setLastClientInput(FORWARD);
			checkCurrent(player);
			player.tickCount += 21;
			player.setSwimming(false);
			movement(player, 0, 0, 1, .01 * multiplier, "surface wading");
			water.heavyinventories$setWater(false);
			player.setOnGround(true);

			state.replace(new ServerSettings(1000, WalkingMode.AT_NINETY_PERCENT), weights);
			holder.update();
			movement(player, 0, 0, 1, 0, "threshold mode below onset");
			player.getInventory().setItem(0, new ItemStack(Items.STONE, 9));
			holder.update();
			movement(player, 0, 0, 1, 0, "threshold mode at onset");
			int jumps = player.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP));
			jump(player, 0, "blocked jump has no cost");
			jump(player, 0, "repeated blocked jump has no cost");
			close(0, player.getDeltaMovement().y, "blocked jump motion");
			close(jumps, player.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP)), "blocked jump statistic");
			player.getInventory().setItem(0, new ItemStack(Items.STONE, 10));
			player.getInventory().setItem(36, MovementScenario.enchanted(player, Items.IRON_BOOTS, ModEnchantments.SUREFOOTED, 4));
			holder.update();
			close(.2, holder.getWalkingMultiplier(), "Surefooted active");
			movement(player, 0, 0, 1, .01, "Surefooted does not discount exhaustion");
			player.setSprinting(true);
			movement(player, 0, 0, 1, .15, "maximum strain sprint");
			player.getInventory().clearContent();
			holder.update();
			movement(player, 0, 0, 1, .1, "empty inventory sprint");

			state.replace(new ServerSettings(1000, WalkingMode.PROGRESSIVE), weights);
			player.getInventory().setItem(0, new ItemStack(Items.STONE, 5));
			holder.update();
			var disabled = state.settings().toJson();
			disabled.getAsJsonObject("effects").getAsJsonObject("exhaustion").addProperty("enabled", false);
			state.replace(ServerSettings.parse(disabled), weights);
			// Deliberately do not update the holder: disabling must take effect on the next action.
			movement(player, 0, 0, 1, .1, "disabled sprint immediately vanilla");
			jump(player, .2, "disabled sprint jump");
			player.setSprinting(false);
			movement(player, 0, 0, 1, 0, "disabled walking");
			jump(player, .05, "disabled ordinary jump");
			food.heavyinventories$setExhaustion(4.1f);
			float saturation = player.getFoodData().getSaturationLevel();
			player.getFoodData().tick(player);
			close(saturation - 1, player.getFoodData().getSaturationLevel(), "vanilla saturation consumption");
			close(.1, food.heavyinventories$getExhaustion(), "vanilla exhaustion processing");
			var oldDifficulty = server.getWorldData().getDifficulty();
			try {
				for (var difficulty : Difficulty.values()) {
					server.getWorldData().setDifficulty(difficulty);
					player.getFoodData().setFoodLevel(20);
					player.getFoodData().setSaturation(0);
					food.heavyinventories$setExhaustion(4.1f);
					player.getFoodData().tick(player);
					close(difficulty == Difficulty.PEACEFUL ? 20 : 19, player.getFoodData().getFoodLevel(), "difficulty " + difficulty);
				}
			} finally {
				server.getWorldData().setDifficulty(oldDifficulty);
			}
			HeavyInventories.LOGGER.info("EXHAUSTION PASSED: walking, crouching, sprint/jump/swim costs, both modes, idle/force exemptions, Surefooted, blocked jumps, disable, vanilla food processing");
		} finally {
			state.replace(savedSettings, savedWeights);
		}
	}

	private static void checkCurrent (ServerPlayer player) {
		var level = player.level();
		var pos = new BlockPos(0, level.getMaxY() - 5, 0);
		var old = level.getBlockState(pos);
		var oldNeighbor = level.getBlockState(pos.east());
		var oldPosition = player.position();
		try {
			level.setBlock(pos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1), 2);
			level.setBlock(pos.east(), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 4), 2);
			if (level.getFluidState(pos).getFlow(level, pos).lengthSqr() < .00001) throw new AssertionError("Current fixture has no flow");
			player.setPos(.5, pos.getY(), .5);
			movement(player, 0, 0, 1, .01, "current with active input preserves vanilla cost only");
		} finally {
			level.setBlock(pos, old, 2);
			level.setBlock(pos.east(), oldNeighbor, 2);
			player.setPos(oldPosition);
		}
	}

	private static void movement (ServerPlayer player, double x, double y, double z, double expected, String name) {
		var food = (FoodDataTestAccess) player.getFoodData();
		food.heavyinventories$setExhaustion(0);
		player.checkMovementStatistics(x, y, z);
		close(expected, food.heavyinventories$getExhaustion(), name);
	}

	private static void jump (ServerPlayer player, double expected, String name) {
		var food = (FoodDataTestAccess) player.getFoodData();
		food.heavyinventories$setExhaustion(0);
		player.setDeltaMovement(Vec3.ZERO);
		player.jumpFromGround();
		close(expected, food.heavyinventories$getExhaustion(), name);
	}

	private static void close (double expected, double actual, String name) {
		if (Math.abs(expected - actual) > .000001) throw new AssertionError(name + ": expected " + expected + ", got " + actual);
	}
}
