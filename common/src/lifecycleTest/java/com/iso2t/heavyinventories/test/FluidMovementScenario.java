package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.test.mixin.FluidTestAccess;
import com.iso2t.heavyinventories.test.mixin.FluidTravelTestAccess;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

public final class FluidMovementScenario {
	public static void run (ServerPlayer anchor) {
		var server = anchor.level().getServer();
		var level = server.overworld();
		var state = ServerWeightState.of(server);
		var oldSettings = state.settings();
		var oldWeights = state.weights();
		var values = new HashMap<>(oldWeights);
		values.put(BuiltInRegistries.ITEM.getKey(Items.STONE), 25f);
		var profile = new GameProfile(UUID.fromString("b80f4e78-1bd2-4b0e-a7de-8e662f03b996"), "FluidTest");
		var player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
		new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false)) {
			@Override public boolean hasClientLoaded () { return true; }
		};
		var holder = PlayerHolder.getOrCreate(player);
		var center = new BlockPos(8, level.getMaxY() - 12, 8);
		var blocks = new LinkedHashMap<BlockPos, BlockState>();
		for (var pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 3, 2))) blocks.put(pos.immutable(), level.getBlockState(pos));
		try {
			player.setGameMode(GameType.SURVIVAL);
			player.getAbilities().flying = false;
			player.setPos(center.getX() + .5, center.getY(), center.getZ() + .5);
			player.baseTick();
			for (boolean lava : new boolean[] {false, true}) {
				for (var pos : blocks.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
				for (var pos : blocks.keySet()) level.setBlock(pos, (lava ? Blocks.LAVA : Blocks.WATER).defaultBlockState(), 2);
				state.replace(new ServerSettings(1000), values);
				reset(player, center);
				if (lava ? !player.isInLava() : !player.isInWater()) throw new AssertionError("Fluid fixture missing " + lava);
				load(player, 0);
				double baseGravity = gravity(player);
				int[] counts = {0, 20, 36, 38, 40, 60};
				double[] speeds = {1, 1, 1, .75, .5, .5};
				double[] gravities = {1, 1, 1, 1.5, 2, 2};
				for (int i = 0; i < counts.length; i++) {
					reset(player, center);
					load(player, counts[i]);
					MovementScenario.checkImpulse(player, speeds[i]);
					close(baseGravity * gravities[i], gravity(player), "gravity lava=" + lava + " count=" + counts[i]);
				}
				load(player, 40);
				var config = state.settings().toJson();
				var effects = config.getAsJsonObject("effects");
				effects.addProperty(lava ? "lava" : "water", false);
				state.replace(ServerSettings.parse(config), values);
				MovementScenario.checkImpulse(player, 1);
				close(baseGravity, gravity(player), "fluid scope off");
				effects.addProperty(lava ? "lava" : "water", true);
				effects.getAsJsonObject("swimming").addProperty("enabled", false);
				state.replace(ServerSettings.parse(config), values);
				MovementScenario.checkImpulse(player, 1);
				close(baseGravity * 2, gravity(player), "sinking independent");
				effects.getAsJsonObject("swimming").addProperty("enabled", true);
				effects.getAsJsonObject("sinking").addProperty("enabled", false);
				state.replace(ServerSettings.parse(config), values);
				MovementScenario.checkImpulse(player, .5);
				close(baseGravity, gravity(player), "swimming independent");
				// Follow actual aiStep jump dispatch: vanilla jumpInLiquid and NeoForge jumpInFluid.
				reset(player, center);
				player.setJumping(true);
				player.aiStep();
				double allowed = player.getDeltaMovement().y;
				effects.getAsJsonObject("upwardMovement").addProperty("enabled", true);
				state.replace(ServerSettings.parse(config), values);
				reset(player, center);
				player.setJumping(true);
				player.aiStep();
				if (!(allowed > player.getDeltaMovement().y + .005)) throw new AssertionError("Fluid jump input not denied, lava=" + lava);
				reset(player, center);
				var travel = (FluidTravelTestAccess) player;
				player.setDeltaMovement(.1, .4, .2);
				travel.heavyinventories$jumpInLiquid(lava ? FluidTags.LAVA : FluidTags.WATER);
				close(.4, player.getDeltaMovement().y, "denial preserves incoming upward force");
				player.moveRelative(1, new Vec3(0, 1, 0));
				close(.4, player.getDeltaMovement().y, "positive relative input denied");
				player.moveRelative(1, new Vec3(0, -1, 0));
				close(-.6, player.getDeltaMovement().y, "downward input allowed");
				// A clear edge lets the vanilla jump-out boost run; denial must cancel only that boost.
				var waterPos = player.position();
				player.setPos(waterPos.x, level.getMaxY() + 10, waterPos.z);
				player.horizontalCollision = true;
				player.setDeltaMovement(.1, .12, .2);
				travel.heavyinventories$jumpOutOfFluid(player.getY());
				close(.12, player.getDeltaMovement().y, "ledge boost denied without deleting momentum");
				effects.getAsJsonObject("upwardMovement").addProperty("enabled", false);
				state.replace(ServerSettings.parse(config), values);
				travel.heavyinventories$jumpOutOfFluid(player.getY());
				close(.3, player.getDeltaMovement().y, "ledge boost restored immediately");
				reset(player, center);
				effects.getAsJsonObject("upwardMovement").addProperty("enabled", true);
				state.replace(ServerSettings.parse(config), values);
				load(player, 39);
				if (holder.preventsFluidAscent()) throw new AssertionError("Upward denial started below threshold");
				load(player, 40);
				if (!holder.preventsFluidAscent()) throw new AssertionError("Upward denial missing at threshold");
				player.getAbilities().flying = true;
				if (holder.preventsFluidAscent()) throw new AssertionError("Flying exemption lost");
				MovementScenario.checkImpulse(player, 1);
				player.getAbilities().flying = false;
				if (!lava) {
					// Aiming upward while swimming is a separate impulse from holding jump.
					reset(player, center);
					player.setSwimming(true);
					player.setXRot(-90);
					player.travel(Vec3.ZERO);
					double denied = player.getDeltaMovement().y;
					effects.getAsJsonObject("upwardMovement").addProperty("enabled", false);
					state.replace(ServerSettings.parse(config), values);
					reset(player, center);
					player.setSwimming(true);
					player.setXRot(-90);
					player.travel(Vec3.ZERO);
					if (!(player.getDeltaMovement().y > denied + .01)) throw new AssertionError("Upward swim aim not denied");
					reset(player, center);
					effects.getAsJsonObject("upwardMovement").addProperty("enabled", true);
					state.replace(ServerSettings.parse(config), values);
					player.onInsideBubbleColumn(false);
					if (!(player.getDeltaMovement().y > 0)) throw new AssertionError("Bubble propulsion was denied");
					double bubbleY = player.getDeltaMovement().y;
					travel.heavyinventories$jumpInLiquid(FluidTags.WATER);
					close(bubbleY, player.getDeltaMovement().y, "bubble motion retained during denial");
					player.onInsideBubbleColumn(true);
					if (!(player.getDeltaMovement().y < bubbleY)) throw new AssertionError("Downward bubble motion changed");
				}
				// Existing effects still modify vanilla's fluid calculations before HI scales them.
				state.replace(new ServerSettings(1000), values);
				reset(player, center);
				player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
				if (!(Math.abs(gravity(player)) < Math.abs(baseGravity * 2))) throw new AssertionError("Slow Falling replaced");
				player.removeAllEffects();
				if (!lava) {
					player.getInventory().setItem(36, MovementScenario.enchanted(player, Items.IRON_BOOTS, net.minecraft.world.item.enchantment.Enchantments.DEPTH_STRIDER, 3));
					((FluidTravelTestAccess) player).heavyinventories$refreshEquipment();
					if (!(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.WATER_MOVEMENT_EFFICIENCY) > 0)) throw new AssertionError("Depth Strider fixture did not apply");
					player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100, 0));
					var enhanced = state.settings().toJson();
					enhanced.getAsJsonObject("effects").getAsJsonObject("swimming").addProperty("enabled", false);
					state.replace(ServerSettings.parse(enhanced), values);
					reset(player, center);
					((FluidTravelTestAccess) player).heavyinventories$travelInFluid(new Vec3(0, 0, 1));
					double vanillaEnhanced = player.getDeltaMovement().horizontalDistance();
					enhanced.getAsJsonObject("effects").getAsJsonObject("swimming").addProperty("enabled", true);
					state.replace(ServerSettings.parse(enhanced), values);
					reset(player, center);
					((FluidTravelTestAccess) player).heavyinventories$travelInFluid(new Vec3(0, 0, 1));
					close(vanillaEnhanced * .5, player.getDeltaMovement().horizontalDistance(), "Depth Strider and Dolphins Grace composition");
					player.getInventory().setItem(36, ItemStack.EMPTY);
					((FluidTravelTestAccess) player).heavyinventories$refreshEquipment();
					player.removeAllEffects();
					// Compare the fluid's own current impulse with restrictions enabled and disabled.
					for (var pos : blocks.keySet()) level.setBlock(pos, Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 4), 2);
					level.setBlock(center, Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 1), 2);
					level.setBlock(center.east(), Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 7), 2);
					enhanced.getAsJsonObject("effects").getAsJsonObject("upwardMovement").addProperty("enabled", true);
					state.replace(ServerSettings.parse(enhanced), values);
					reset(player, center);
					var current = player.getDeltaMovement();
					if (current.lengthSqr() < 1e-8) throw new AssertionError("Current fixture has no flow");
					enhanced.getAsJsonObject("effects").addProperty("water", false);
					state.replace(ServerSettings.parse(enhanced), values);
					reset(player, center);
					close(0, player.getDeltaMovement().subtract(current).length(), "current impulse retained");
				}
				// Enter shallow fluid with a configured threshold below the unchanged land-jump cutoff.
				for (var pos : blocks.keySet()) level.setBlock(pos, pos.getY() == center.getY() ? (lava ? Blocks.LAVA : Blocks.WATER).defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
				state.replace(new ServerSettings(1000), values);
				var shallow = state.settings().toJson();
				shallow.getAsJsonObject("effects").getAsJsonObject("upwardMovement").addProperty("enabled", true);
				shallow.getAsJsonObject("effects").getAsJsonObject("upwardMovement").addProperty("thresholdPercent", 50);
				state.replace(ServerSettings.parse(shallow), values);
				load(player, 20);
				player.setPos(center.getX() + .5, center.getY() + .7, center.getZ() + .5);
				((FluidTestAccess) player).heavyinventories$updateFluid();
				player.setDeltaMovement(Vec3.ZERO);
				player.jumpFromGround();
				close(0, player.getDeltaMovement().y, "shallow fluid ground jump denied");
				load(player, 19);
				player.jumpFromGround();
				if (!(player.getDeltaMovement().y > 0)) throw new AssertionError("Dropping weight did not restore shallow jump");
				load(player, 40);
				player.setPos(center.getX() + .5, level.getMaxY() + 10, center.getZ() + .5);
				((FluidTestAccess) player).heavyinventories$updateFluid();
				close(1, holder.getFluidSwimMultiplier(), "leaving fluid clears swim penalty");
				close(1, holder.getFluidSinkGravityMultiplier(), "leaving fluid clears gravity penalty");
				if (holder.preventsFluidAscent()) throw new AssertionError("Dry player retained fluid denial");
			}
			HeavyInventories.LOGGER.info("FLUID MOVEMENT PASSED: water/lava curves, independent toggles, scope, jump/aim/ledge denial, retained momentum/bubbles, threshold recovery, Slow Falling");
		} finally {
			blocks.forEach((pos, block) -> level.setBlock(pos, block, 2));
			state.replace(oldSettings, oldWeights);
			player.getTextFilter().leave();
		}
	}

	public static void prepareClientCheck (ServerPlayer player) {
		var state = ServerWeightState.of(player.level().getServer());
		var config = state.settings().toJson();
		var effects = config.getAsJsonObject("effects");
		effects.getAsJsonObject("swimming").addProperty("minMultiplier", .3);
		effects.getAsJsonObject("sinking").addProperty("maxMultiplier", 2.5);
		effects.getAsJsonObject("upwardMovement").addProperty("enabled", true);
		state.replace(ServerSettings.parse(config), state.weights());
		TestPlayerTick.update(player);
	}

	public static void checkClient (Player player) {
		var access = (FluidTestAccess) player;
		var holder = PlayerHolder.getOrCreate(player);
		boolean water = player.isInWater();
		var motion = player.getDeltaMovement();
		try {
			access.heavyinventories$setWater(true);
			close(.3, holder.getFluidSwimMultiplier(), "synchronized swimming multiplier");
			close(2.5, holder.getFluidSinkGravityMultiplier(), "synchronized sinking multiplier");
			if (!holder.preventsFluidAscent()) throw new AssertionError("Synchronized ascent denial missing");
			player.setDeltaMovement(Vec3.ZERO);
			player.moveRelative(1, new Vec3(1, 1, 0));
			close(.3 / Math.sqrt(2), player.getDeltaMovement().horizontalDistance(), "client horizontal input");
			close(0, player.getDeltaMovement().y, "client vertical input");
			player.setDeltaMovement(0, .4, 0);
			((FluidTravelTestAccess) player).heavyinventories$jumpInLiquid(FluidTags.WATER);
			close(.4, player.getDeltaMovement().y, "client force preservation");
			HeavyInventories.LOGGER.info("CLIENT FLUID MOVEMENT PASSED: synchronized custom swimming/sinking settings, ascent denial and external motion");
		} finally {
			access.heavyinventories$setWater(water);
			player.setDeltaMovement(motion);
		}
	}

	private static void load (ServerPlayer player, int count) {
		player.getInventory().setItem(0, count == 0 ? ItemStack.EMPTY : new ItemStack(Items.STONE, count));
		PlayerHolder.getOrCreate(player).update();
	}
	private static void reset (ServerPlayer player, BlockPos pos) {
		player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
		player.setDeltaMovement(Vec3.ZERO);
		player.setOnGround(false);
		player.horizontalCollision = false;
		player.setJumping(false);
		player.setSwimming(false);
		player.setSprinting(false);
		player.setXRot(0);
		player.setYRot(0);
		((FluidTestAccess) player).heavyinventories$updateFluid();
	}
	private static double gravity (ServerPlayer player) {
		player.setDeltaMovement(Vec3.ZERO);
		((FluidTravelTestAccess) player).heavyinventories$travelInFluid(Vec3.ZERO);
		return player.getDeltaMovement().y;
	}
	private static void close (double expected, double actual, String message) {
		if (Math.abs(expected - actual) > .00001) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
	}
}
