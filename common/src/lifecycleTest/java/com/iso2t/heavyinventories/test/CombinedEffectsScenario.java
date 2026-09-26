package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.enchantment.ModEnchantments;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.api.player.PlayerKnockback;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import com.iso2t.heavyinventories.network.PlayerWeightPayload;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.test.mixin.FluidTestAccess;
import com.iso2t.heavyinventories.test.mixin.FluidTravelTestAccess;
import com.iso2t.heavyinventories.test.mixin.FoodDataTestAccess;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Combined engine checks and repeatable water-recovery trials in a restored local fixture. */
public final class CombinedEffectsScenario {
	public static void run (ServerPlayer anchor) {
		var server = anchor.level().getServer();
		var level = server.overworld();
		var state = ServerWeightState.of(server);
		var savedSettings = state.settings();
		var savedWeights = state.weights();
		var weights = new HashMap<>(savedWeights);
		weights.put(BuiltInRegistries.ITEM.getKey(Items.STONE), 25f);
		weights.put(BuiltInRegistries.ITEM.getKey(Items.IRON_BOOTS), 0f);
		var profile = new GameProfile(UUID.fromString("b80f4e78-1bd2-4b0e-a7de-8e662f03b994"), "CombinedTest");
		var player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
		int[] snapshots = {0};
		new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false)) {
			@Override public boolean hasClientLoaded () { return true; }
			@Override public void send (Packet<?> packet) {
				if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof PlayerWeightPayload) snapshots[0]++;
			}
		};
		var holder = PlayerHolder.getOrCreate(player);
		var water = (FluidTestAccess) player;
		var travel = (FluidTravelTestAccess) player;
		var food = (FoodDataTestAccess) player.getFoodData();
		var center = new BlockPos(8, level.getMaxY() - 16, 8);
		var blocks = new LinkedHashMap<BlockPos, BlockState>();
		try {
			player.setGameMode(GameType.SURVIVAL);
			player.getAbilities().flying = false;
			player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
			player.setPos(8.5, level.getMaxY() + 10, 8.5);
			player.baseTick();
			player.setYRot(0);
			player.setLastClientInput(new Input(true, false, false, false, false, false, false));
			for (var mode : WalkingMode.values()) {
				state.replace(new ServerSettings(1000, mode), weights);
				for (int count : new int[] {0, 35, 36, 39, 40, 50, 60}) {
					load(player, count);
					double ratio = count * .025;
					double burden = mode == WalkingMode.PROGRESSIVE ? 1 - Math.sqrt(Math.max(0, 1 - ratio)) : Math.clamp((ratio - .9) / .1, 0, 1);
					water.heavyinventories$setWater(false);
					MovementScenario.checkImpulse(player, 1 - burden);
					player.setOnGround(true);
					food.heavyinventories$setExhaustion(0);
					player.checkMovementStatistics(0, 0, 1);
					close(.01 * burden, food.heavyinventories$getExhaustion(), "combined walking cost");
					player.setHealth(100);
					player.invulnerableTime = 0;
					Blocks.STONE.fallOn(level, Blocks.STONE.defaultBlockState(), player.blockPosition().below(), player, 8);
					close(5 * (1 + Math.clamp((ratio - .9) / .35, 0, 1)), 100 - player.getHealth(), "combined fall damage");
					water.heavyinventories$setWater(true);
					double severity = Math.clamp((ratio - .9) / .1, 0, 1);
					MovementScenario.checkImpulse(player, 1 - .5 * severity);
					close(1 + severity, holder.getFluidSinkGravityMultiplier(), "combined sinking");
					if (holder.preventsFluidAscent()) throw new AssertionError("Default unexpectedly denies ascent");
					player.setSwimming(true);
					player.setOnGround(false);
					player.tickCount += 21;
					food.heavyinventories$setExhaustion(0);
					player.checkMovementStatistics(0, 0, 1);
					close(.01 * (1 + .5 * burden), food.heavyinventories$getExhaustion(), "combined swimming cost");
					player.setSwimming(false);
					KnockbackScenario.checkBonus(player, .4 * Math.min(ratio, 1));
				}
			}
			state.replace(new ServerSettings(1000), weights);
			load(player, 40);
			player.getInventory().setItem(36, MovementScenario.enchanted(player, Items.IRON_BOOTS, ModEnchantments.SUREFOOTED, 4));
			holder.update();
			close(.2, holder.getWalkingMultiplier(), "Surefooted overloaded floor");
			close(.5, holder.getFluidSwimMultiplier(), "Surefooted keeps fluid penalty");
			player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1));
			holder.update();
			close(1200, holder.getMaxWeight(), "Strength capacity");
			close(1, holder.getFluidSwimMultiplier(), "Strength relieves fluid strain");
			close(1, holder.getFallDamageMultiplier(), "Strength relieves fall strain");
			KnockbackScenario.checkBonus(player, .4);
			player.removeAllEffects();
			player.getInventory().setItem(36, ItemStack.EMPTY);
			holder.update();
			// Repeated real fluid travel with jump held: full load must retain the default escape route.
			for (var pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 8, 2))) {
				blocks.put(pos.immutable(), level.getBlockState(pos));
				level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
			}
			double defaultRise = swimTrial(player, center);
			if (!(defaultRise > 0)) throw new AssertionError("Default full-load water escape lost: " + defaultRise);
			var config = state.settings().toJson();
			config.getAsJsonObject("effects").getAsJsonObject("upwardMovement").addProperty("enabled", true);
			state.replace(ServerSettings.parse(config), weights);
			holder.update();
			if (!(swimTrial(player, center) < 0)) throw new AssertionError("Optional ascent denial did not sink");
			load(player, 35);
			if (!(swimTrial(player, center) > 0)) throw new AssertionError("Dropping weight did not restore ascent");
			load(player, 40);
			player.setDeltaMovement(Vec3.ZERO);
			player.onInsideBubbleColumn(false);
			double bubbleY = player.getDeltaMovement().y;
			travel.heavyinventories$jumpInLiquid(FluidTags.WATER);
			if (!(bubbleY > 0)) throw new AssertionError("Bubble recovery missing");
			close(bubbleY, player.getDeltaMovement().y, "combined denial retains bubble impulse");
			for (var entry : config.getAsJsonObject("effects").entrySet())
				if (entry.getValue().isJsonObject()) entry.getValue().getAsJsonObject().addProperty("enabled", false);
			state.replace(ServerSettings.parse(config), weights);
			holder.update();
			if (!(swimTrial(player, center) > defaultRise)) throw new AssertionError("Disabling failed to restore vanilla ascent");
			close(1, holder.getFallDamageMultiplier(), "all disabled fall");
			close(1, holder.getFluidSwimMultiplier(), "all disabled swimming");
			close(1, holder.getFluidSinkGravityMultiplier(), "all disabled sinking");
			KnockbackScenario.checkBonus(player, 0);
			player.tickCount += 21;
			player.setSwimming(true);
			food.heavyinventories$setExhaustion(0);
			player.checkMovementStatistics(0, 0, 1);
			close(.01, food.heavyinventories$getExhaustion(), "all disabled vanilla swimming cost");
			// Stable snapshots do not repeat; every actual change and a fresh holder still send.
			holder.synchronize(player);
			int sent = snapshots[0];
			if (sent != 1) throw new AssertionError("Initial snapshot missing: " + sent);
			for (int i = 0; i < 60; i++) { player.tickCount++; holder.update(); holder.synchronize(player); }
			if (snapshots[0] != sent) throw new AssertionError("Unchanged snapshot resent");
			load(player, 39);
			holder.synchronize(player);
			if (snapshots[0] != ++sent) throw new AssertionError("Inventory change not sent");
			state.replace(new ServerSettings(1000), weights);
			holder.update(); holder.synchronize(player);
			if (snapshots[0] != ++sent) throw new AssertionError("Settings change not sent");
			var modifier = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getModifier(PlayerKnockback.MODIFIER_ID);
			for (int i = 0; i < 60; i++) { player.tickCount++; holder.update(); holder.synchronize(player); }
			if (snapshots[0] != sent || modifier != player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getModifier(PlayerKnockback.MODIFIER_ID)) throw new AssertionError("Stable enabled state churn");
			HeavyInventories.LOGGER.info("COMBINED EFFECTS PASSED: both modes, boundaries/overload, food/fall/fluid/resistance, Strength/Surefooted, repeated water escape/denial/drop/disable, bubbles, all disabled, change-only snapshots");
		} finally {
			blocks.forEach((pos, block) -> level.setBlock(pos, block, 2));
			state.replace(savedSettings, savedWeights);
			player.getTextFilter().leave();
		}
	}

	private static double swimTrial (ServerPlayer player, BlockPos center) {
		player.setPos(center.getX() + .5, center.getY(), center.getZ() + .5);
		player.setDeltaMovement(Vec3.ZERO);
		player.setSwimming(false);
		player.setOnGround(false);
		for (int i = 0; i < 20; i++) {
			((FluidTestAccess) player).heavyinventories$updateFluid();
			((FluidTravelTestAccess) player).heavyinventories$jumpInLiquid(FluidTags.WATER);
			((FluidTravelTestAccess) player).heavyinventories$travelInFluid(Vec3.ZERO);
		}
		return player.getY() - center.getY();
	}
	private static void load (ServerPlayer player, int count) {
		player.getInventory().setItem(0, count == 0 ? ItemStack.EMPTY : new ItemStack(Items.STONE, count));
		PlayerHolder.getOrCreate(player).update();
	}
	private static void close (double expected, double actual, String name) {
		if (!Double.isFinite(actual) || Math.abs(expected - actual) > .00003) throw new AssertionError(name + ": expected " + expected + ", got " + actual);
	}
}
