package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.enchantment.ModEnchantments;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.test.mixin.FluidTestAccess;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.UUID;

/** Real fall/health processing on an isolated player, with an unattached connection for vanilla feedback. */
public final class FallDamageScenario {
	public static void run (ServerPlayer anchor) {
		var server = anchor.level().getServer();
		var level = server.overworld();
		var state = ServerWeightState.of(server);
		var savedSettings = state.settings();
		var savedWeights = state.weights();
		var profile = new GameProfile(UUID.fromString("b80f4e78-1bd2-4b0e-a7de-8e662f03b997"), "FallDamageTest");
		var player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
		new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false)) {
			@Override public boolean hasClientLoaded () { return true; }
		};
		var holder = PlayerHolder.getOrCreate(player);
		var weights = new HashMap<>(savedWeights);
		weights.put(BuiltInRegistries.ITEM.getKey(Items.STONE), 25f);
		for (var item : new net.minecraft.world.item.Item[] { Items.IRON_BOOTS, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS })
			weights.put(BuiltInRegistries.ITEM.getKey(item), 0f);
		try {
			player.setGameMode(GameType.SURVIVAL);
			player.getAbilities().flying = false;
			player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
			player.setPos(.5, level.getMaxY() + 10, .5);
			player.baseTick();
			for (var mode : WalkingMode.values()) {
				state.replace(new ServerSettings(1000, mode), weights);
				int[] counts = {0, 20, 36, 40, 50, 60};
				double[] multipliers = {1, 1, 1, 1 + 2.0 / 7, 2, 2};
				for (int i = 0; i < counts.length; i++) {
					load(player, counts[i]);
					close(multipliers[i], holder.getFallDamageMultiplier(), "curve " + mode + " count=" + counts[i]);
					landing(player, Blocks.STONE, 8, 5 * multipliers[i], "ordinary fall");
					for (double distance : new double[] {0, 3, 3.5, 3.99}) landing(player, Blocks.STONE, distance, 0, "safe fall");
				}
			}
			load(player, 50);
			landing(player, Blocks.HAY_BLOCK, 13, 4, "hay reduction");
			landing(player, Blocks.HONEY_BLOCK, 13, 4, "honey reduction");
			landing(player, Blocks.RED_BED, 10, 4, "bed reduction");
			landing(player, Blocks.HAY_BLOCK, 7, 0, "harmless cushioned fall stays harmless");
			landing(player, Blocks.SLIME_BLOCK, 30, 0, "slime immunity");
			player.setDeltaMovement(0, -1, 0);
			Blocks.SLIME_BLOCK.updateEntityMovementAfterFallOn(level, player);
			close(1, player.getDeltaMovement().y, "slime bounce");
			player.getInventory().setItem(36, MovementScenario.enchanted(player, Items.IRON_BOOTS, Enchantments.FEATHER_FALLING, 4));
			landing(player, Blocks.STONE, 8, 5.2, "Feather Falling IV");
			player.getInventory().setItem(36, ItemStack.EMPTY);
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 0));
			landing(player, Blocks.STONE, 8, 8, "Resistance I");
			player.removeAllEffects();
			player.getAttribute(Attributes.SAFE_FALL_DISTANCE).setBaseValue(7);
			landing(player, Blocks.STONE, 7.99, 0, "increased safe distance");
			landing(player, Blocks.STONE, 9, 4, "safe-distance attribute");
			player.getAttribute(Attributes.SAFE_FALL_DISTANCE).setBaseValue(3);
			player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER).setBaseValue(.5);
			landing(player, Blocks.STONE, 9, 6, "fall-damage attribute");
			player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER).setBaseValue(1);
			player.getInventory().setItem(36, MovementScenario.enchanted(player, Items.IRON_BOOTS, ModEnchantments.SUREFOOTED, 4));
			holder.update();
			landing(player, Blocks.STONE, 8, 10, "Surefooted does not protect falls");
			player.getInventory().setItem(36, ItemStack.EMPTY);
			load(player, 40);
			player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1));
			holder.update();
			landing(player, Blocks.STONE, 8, 5, "Strength increases capacity");
			player.removeAllEffects();
			player.getInventory().setItem(38, MovementScenario.enchanted(player, Items.IRON_CHESTPLATE, ModEnchantments.BRACING, 2));
			holder.update();
			landing(player, Blocks.STONE, 8, 5, "Bracing increases capacity");
			player.getInventory().setItem(38, ItemStack.EMPTY);
			load(player, 50);
			for (var mode : new GameType[] {GameType.CREATIVE, GameType.SPECTATOR}) {
				player.setGameMode(mode);
				landing(player, Blocks.STONE, 30, 0, mode + " immunity");
			}
			player.setGameMode(GameType.SURVIVAL);
			player.getAbilities().flying = false;
			player.setInvulnerable(true);
			landing(player, Blocks.STONE, 8, 0, "damage immunity");
			player.setInvulnerable(false);
			player.fallDistance = 30;
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
			player.setDeltaMovement(0, -.1, 0);
			player.aiStep();
			close(0, player.fallDistance, "Slow Falling resets distance");
			landing(player, Blocks.STONE, player.fallDistance, 0, "Slow Falling landing");
			player.removeAllEffects();
			waterLanding(player);
			var custom = state.settings().toJson();
			var fall = custom.getAsJsonObject("effects").getAsJsonObject("fallDamage");
			fall.addProperty("startPercent", 50);
			fall.addProperty("fullPercent", 100);
			fall.addProperty("maxMultiplier", 3);
			state.replace(ServerSettings.parse(custom), weights);
			load(player, 30);
			landing(player, Blocks.STONE, 8, 10, "custom thresholds/cap");
			fall.addProperty("enabled", false);
			state.replace(ServerSettings.parse(custom), weights);
			// No holder refresh: a config edit must remove the modifier on the next fall.
			landing(player, Blocks.STONE, 8, 5, "disabled immediately");
			landing(player, Blocks.HAY_BLOCK, 13, 2, "disabled preserves landing reduction");
			HeavyInventories.LOGGER.info("FALL DAMAGE PASSED: default/custom curves, safe falls, Feather Falling, Resistance, capacity bonuses, attributes, game modes, Slow Falling, water, landing blocks, immediate disable");
		} finally {
			state.replace(savedSettings, savedWeights);
			player.getTextFilter().leave();
		}
	}

	private static void load (ServerPlayer player, int count) {
		player.getInventory().setItem(0, count == 0 ? ItemStack.EMPTY : new ItemStack(Items.STONE, count));
		PlayerHolder.getOrCreate(player).update();
	}

	private static void landing (ServerPlayer player, Block block, double distance, double expected, String name) {
		player.setHealth(100);
		player.invulnerableTime = 0;
		block.fallOn(player.level(), block.defaultBlockState(), player.blockPosition().below(), player, distance);
		close(expected, 100 - player.getHealth(), name);
	}

	private static void waterLanding (ServerPlayer player) {
		var level = player.level();
		var pos = new BlockPos(8, level.getMaxY() - 5, 8);
		var oldBlock = level.getBlockState(pos);
		var oldPosition = player.position();
		try {
			level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
			player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
			player.fallDistance = 30;
			((FluidTestAccess) player).heavyinventories$updateFluid();
			if (!player.isInWater()) throw new AssertionError("Water landing fixture did not enter water");
			close(0, player.fallDistance, "water resets fall distance");
			landing(player, Blocks.STONE, player.fallDistance, 0, "water landing");
		} finally {
			level.setBlock(pos, oldBlock, 2);
			player.setPos(oldPosition);
			player.setDeltaMovement(Vec3.ZERO);
			((FluidTestAccess) player).heavyinventories$updateFluid();
		}
	}

	private static void close (double expected, double actual, String name) {
		if (Math.abs(expected - actual) > .00002) throw new AssertionError(name + ": expected " + expected + ", got " + actual);
	}
}
