package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.enchantment.ModEnchantments;
import com.iso2t.heavyinventories.player.PlayerEvents;
import com.iso2t.heavyinventories.player.PlayerHolder;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.test.mixin.ElytraFlightTestAccess;
import com.iso2t.heavyinventories.test.mixin.FireworkFlightTestAccess;
import com.iso2t.heavyinventories.test.mixin.FluidTravelTestAccess;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.UUID;

public final class ElytraFlightScenario {

	public static void run (ServerPlayer anchor) {
		var server = anchor.level().getServer();
		var state = ServerWeightState.of(server);
		var savedSettings = state.settings();
		var savedWeights = state.weights();
		var player = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.fromString("64edfb10-b371-4a15-9f35-0942ca2ebc81"), "ElytraTest"), ClientInformation.createDefault());
		new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
		var weights = new HashMap<>(savedWeights);
		weights.put(BuiltInRegistries.ITEM.getKey(Items.STONE), 100f);
		weights.put(BuiltInRegistries.ITEM.getKey(Items.ELYTRA), 0f);
		try {
			state.replace(ServerSettings.DEFAULT, weights);
			player.setPos(.5, player.level().getMaxY() + 20, .5);
			player.setOnGround(false);
			player.getInventory().setItem(38, new ItemStack(Items.ELYTRA));
			((FluidTravelTestAccess) player).heavyinventories$refreshEquipment();
			for (float pitch : new float[] { -10, 0, 10 }) {
				load(player, 0);
				double empty = glideDistance(player, pitch);
				load(player, 5);
				double half = glideDistance(player, pitch);
				load(player, 10);
				double full = glideDistance(player, pitch);
				load(player, 20);
				close(full, glideDistance(player, pitch), "penalty cap");
				require(empty > half && half > full && full > empty * .65 && full < empty * .9, "Default glide balance at pitch " + pitch);
				HeavyInventories.LOGGER.info("ELYTRA RANGE: pitch={}, 100-block descent, empty={} blocks, 500 lb={} blocks, 1000 lb={} blocks", pitch, empty, half, full);
			}
			load(player, 10);
			checkSoaring(player);
			player.stopFallFlying();
			require(player.tryToStartFallFlying(), "Loaded elytra could not deploy");
			var holder = PlayerHolder.getOrCreate(player);
			var before = holder.elytraEffects();
			player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 3));
			state.replace(new ServerSettings(2000), weights);
			holder.update();
			require(holder.getMaxWeight() > 2000 && before.equals(holder.elytraEffects()), "Capacity bonuses changed flight penalties");
			player.removeAllEffects();
			checkBoost(player, .75);
			var config = state.settings().toJson();
			var flight = config.getAsJsonObject("effects").getAsJsonObject("elytra");
			flight.addProperty("referenceWeight", 500);
			flight.addProperty("maxLiftReduction", .2);
			flight.addProperty("maxRocketReduction", .4);
			state.replace(ServerSettings.parse(config), weights);
			close(.8, holder.elytraEffects().liftMultiplier(), "live custom lift");
			checkBoost(player, .6);
			flight.addProperty("enabled", false);
			state.replace(ServerSettings.parse(config), weights);
			checkBoost(player, 1);
			double disabled = glideDistance(player, 0);
			state.replace(ServerSettings.DEFAULT, weights);
			load(player, 0);
			close(disabled, glideDistance(player, 0), "disable restores vanilla glide");
			checkBoost(player, 1);
			load(player, 10);
			for (float pitch : new float[] { -90, -45, 45, 90 }) {
				player.setXRot(pitch);
				Vec3 movement = new Vec3(0, -.2, 1);
				player.setDeltaMovement(movement);
				var changed = ((ElytraFlightTestAccess) player).heavyinventories$glide(movement);
				require(Double.isFinite(changed.lengthSqr()) && player.isFallFlying(), "Pitch stopped flight or produced invalid velocity");
			}
			player.setXRot(0);
			player.setDeltaMovement(new Vec3(0, -.2, 1));
			var normal = ((ElytraFlightTestAccess) player).heavyinventories$glide(player.getDeltaMovement());
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
			var slow = ((ElytraFlightTestAccess) player).heavyinventories$glide(player.getDeltaMovement());
			require(slow.y > normal.y, "Flight penalties bypassed Slow Falling");
			player.removeAllEffects();
			for (var mode : new GameType[] { GameType.CREATIVE, GameType.SPECTATOR }) {
				player.setGameMode(mode);
				player.startFallFlying();
				checkBoost(player, 1);
				close(disabled, glideDistance(player, 0), mode + " glide exemption");
			}
			HeavyInventories.LOGGER.info("ELYTRA FLIGHT PASSED: measured glide range, smooth weight scaling/cap, deployment, rocket thrust/duration, capacity independence, live config/disable, dive/climb, Slow Falling, creative/spectator");
		} finally {
			state.replace(savedSettings, savedWeights);
			player.getTextFilter().leave();
		}
	}

	private static void checkSoaring (ServerPlayer player) {
		var enchantment = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.SOARING);
		require(enchantment.value().getMaxLevel() == 4 && enchantment.value().canEnchant(new ItemStack(Items.ELYTRA)), "Soaring definition does not support elytra I-IV");
		require(!enchantment.value().canEnchant(new ItemStack(Items.IRON_CHESTPLATE)), "Soaring supports armor");
		require(enchantment.is(EnchantmentTags.IN_ENCHANTING_TABLE), "Soaring is missing from the book enchantment pool");
		var holder = PlayerHolder.getOrCreate(player);
		double unenchanted = glideDistance(player, 0), previous = unenchanted;
		for (int level : new int[] { 1, 2, 3, 4, 255 }) {
			player.getInventory().setItem(38, MovementScenario.enchanted(player, Items.ELYTRA, ModEnchantments.SOARING, level));
			holder.update();
			int capped = Math.min(level, 4);
			close(.85 + .03 * capped, holder.elytraEffects().liftMultiplier(), "Soaring lift " + level);
			checkBoost(player, .75 + .05 * capped);
			double distance = glideDistance(player, 0);
			if (level <= 4) require(distance > previous, "Soaring did not improve glide range");
			else close(previous, distance, "Command enchantment level bypassed cap");
			previous = distance;
		}
		load(player, 0);
		require(glideDistance(player, 0) > previous, "Soaring completely removed the weight penalty");
		load(player, 10);
		player.getInventory().setItem(38, ItemStack.EMPTY);
		player.getInventory().setItem(1, MovementScenario.enchanted(player, Items.ELYTRA, ModEnchantments.SOARING, 4));
		holder.update();
		close(.85, holder.elytraEffects().liftMultiplier(), "Carried elytra granted Soaring");
		player.getInventory().setItem(1, ItemStack.EMPTY);
		player.getInventory().setItem(38, MovementScenario.enchanted(player, Items.IRON_CHESTPLATE, ModEnchantments.SOARING, 4));
		holder.update();
		close(.85, holder.elytraEffects().liftMultiplier(), "Command-enchanted armor granted Soaring");
		player.getInventory().setItem(38, new ItemStack(Items.ELYTRA));
		holder.update();
		close(unenchanted, glideDistance(player, 0), "Unequipping Soaring retained its bonus");
		HeavyInventories.LOGGER.info("SOARING PASSED: elytra-only definition, book availability, levels I-IV, lift/range/rockets, 80% cap, equipment removal");
	}

	public static void prepareSoaringClient (ServerPlayer player) {
		var state = ServerWeightState.of(player.level().getServer());
		var weights = new HashMap<>(state.weights());
		weights.put(BuiltInRegistries.ITEM.getKey(Items.ELYTRA), 0f);
		state.replace(state.settings(), weights);
		player.getInventory().setItem(38, MovementScenario.enchanted(player, Items.ELYTRA, ModEnchantments.SOARING, 4));
		PlayerEvents.onPlayerTick(player);
	}

	public static void checkClient (Player player) {
		var movement = player.getDeltaMovement();
		float pitch = player.getXRot(), yaw = player.getYRot();
		boolean gliding = player.isFallFlying();
		try {
			player.startFallFlying();
			close(.97, PlayerHolder.getOrCreate(player).elytraEffects().liftMultiplier(), "client synchronized Soaring lift");
			checkBoost(player, .95);
			player.setDeltaMovement(new Vec3(0, 0, 1));
			var result = ((ElytraFlightTestAccess) player).heavyinventories$glide(player.getDeltaMovement());
			close(-.019291692, result.y, "client Soaring glide lift");
			HeavyInventories.LOGGER.info("CLIENT SOARING PASSED: server equipment level synchronized into glide and rocket physics");
			HeavyInventories.LOGGER.info("CLIENT ELYTRA FLIGHT PASSED: server weight/settings, glide lift and rocket physics");
		} finally {
			if (!gliding) player.stopFallFlying();
			player.setDeltaMovement(movement);
			player.setXRot(pitch);
			player.setYRot(yaw);
		}
	}

	private static void load (ServerPlayer player, int count) {
		player.getInventory().setItem(0, count == 0 ? ItemStack.EMPTY : new ItemStack(Items.STONE, count));
		PlayerHolder.getOrCreate(player).update();
	}

	private static double glideDistance (Player player, float pitch) {
		player.startFallFlying();
		player.setXRot(pitch);
		player.setYRot(0);
		Vec3 movement = new Vec3(0, 0, 1);
		double height = 100, distance = 0;
		int ticks = 0;
		while (height > 0) {
			require(++ticks < 5000, "Glide did not descend");
			player.setDeltaMovement(movement);
			movement = ((ElytraFlightTestAccess) player).heavyinventories$glide(movement);
			height += movement.y;
			distance += movement.horizontalDistance();
		}
		return distance;
	}

	private static void checkBoost (Player player, double multiplier) {
		player.setXRot(0);
		player.setYRot(0);
		for (double initialSpeed : new double[] { 0, 1, 3 }) {
			player.setDeltaMovement(new Vec3(0, 0, initialSpeed));
			var rocket = new FireworkRocketEntity(player.level(), new ItemStack(Items.FIREWORK_ROCKET), player);
			rocket.setSilent(true);
			var access = (FireworkFlightTestAccess) rocket;
			int lifetime = access.heavyinventories$lifetime();
			try {
				rocket.tick();
				close(initialSpeed * .5 + .85 * multiplier, player.getDeltaMovement().z, "rocket speed=" + initialSpeed);
				require(access.heavyinventories$lifetime() == lifetime && access.heavyinventories$life() == 1, "Rocket duration changed");
			} finally {
				rocket.discard();
			}
		}
	}

	private static void close (double expected, double actual, String message) {
		if (Math.abs(expected - actual) > .00002) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
