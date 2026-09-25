package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.events.PlayerEvents;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.config.HudMode;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.platform.Services;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.test.mixin.GuiFeedbackTestAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.waypoints.Waypoint;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Real contextual-bar transitions and window/background fixtures in disposable worlds.
 */
public final class RingCompatibilityScenario {
	private static final String[] NAMES    = { "locator", "mounted-jump", "experience-return", "day-small-crowded", "night-large-crowded" };
	private static final String[] BARS     = { "LocatorBarRenderer", "JumpableVehicleBarRenderer", "ExperienceBarRenderer", "ExperienceBarRenderer", "ExperienceBarRenderer" };
	private static final UUID     WAYPOINT = UUID.fromString("5157cc0f-1f54-479a-970c-5d831ee234ec");
	private static       int      index, phase, waiting, ringStart, xpStart, width, height, level;
	private static float  health;
	private static double maxHealth;
	private static          long                    time;
	private static          Horse                   horse;
	private static          CompletableFuture<Void> operation;
	private static volatile boolean                 captured;

	public static boolean tick (Minecraft client, ServerPlayer player) {
		var server = client.getSingleplayerServer();
		if (index == NAMES.length) {
			if (phase != 4) {
				RingHudScenario.active = false;
				client.getWindow().setWindowed(width, height);
				operation = server.submit(() -> {
					player.getInventory().clearContent();
					player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
					player.setHealth(health);
					player.setExperienceLevels(level);
					server.clockManager().setTotalTicks(player.level().dimensionType().defaultClock().orElseThrow(), time);
					try {
						ServerWeightState.of(server).reload(server);
					} catch (java.io.IOException e) {
						throw new java.io.UncheckedIOException(e);
					}
					PlayerEvents.onPlayerTick(player);
				});
				phase = 4;
			}
			if (!operation.isDone()) return false;
			operation.join();
			HeavyInventories.LOGGER.info("RING COMPATIBILITY PASSED: locator, mounted jump, experience return, 800x600 daylight, 1600x900 night, armor and extra hearts, XP ordering, one ring per frame");
			return true;
		}
		if (phase == 0) {
			HeavyInventories.LOGGER.info("Ring compatibility checkpoint {}", NAMES[index]);
			if (index == 0) {
				width = client.getWindow().getScreenWidth();
				height = client.getWindow().getScreenHeight();
			}
			if (index >= 3) client.getWindow().setWindowed(index == 3 ? 800 : 1600, index == 3 ? 600 : 900);
			ConfigOptions.HUD_MODE = HudMode.RING;
			ConfigOptions.RING_VERTICAL_OFFSET = 7;
			ConfigOptions.ENABLE_GUI_OVERLAY = true;
			client.setScreen(null);
			client.gui.getChat().clearMessages(true);
			operation = server.submit(() -> {
				if (index == 0) {
					level = player.experienceLevel;
					health = player.getHealth();
					maxHealth = player.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
					time = server.clockManager().getTotalTicks(player.level().dimensionType().defaultClock().orElseThrow());
					player.removeAllEffects();
					player.getInventory().clearContent();
					player.getInventory().setItem(0, new ItemStack(Items.STONE));
					player.setExperienceLevels(30);
					player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(0, 0, 30));
					player.connection.send(ClientboundTrackedWaypointPacket.addWaypointAzimuth(WAYPOINT, new Waypoint.Icon(), 0));
				} else if (index == 1) {
					player.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(WAYPOINT));
					horse = EntityType.HORSE.create(player.level(), EntitySpawnReason.COMMAND);
					if (horse == null) throw new AssertionError("Horse fixture could not spawn");
					horse.setPos(player.position());
					horse.setNoAi(true);
					horse.setNoGravity(true);
					horse.setTamed(true);
					horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
					player.level().addFreshEntity(horse);
					if (!player.startRiding(horse, true, false)) throw new AssertionError("Horse fixture could not mount");
				} else if (index == 2) {
					player.stopRiding();
					horse.discard();
				} else {
					player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60);
					player.setHealth(60);
					player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
					player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
					player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
					player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
					server.clockManager().setTotalTicks(player.level().dimensionType().defaultClock().orElseThrow(), index == 3 ? 6000 : 18000);
				}
				ServerWeightState.of(server).replace(ServerSettings.DEFAULT, Map.of(Identifier.withDefaultNamespace("stone"), index == 4 ? 1100f : 900f, Identifier.withDefaultNamespace("diamond_helmet"), 0f, Identifier.withDefaultNamespace("diamond_chestplate"), 0f, Identifier.withDefaultNamespace("diamond_leggings"), 0f, Identifier.withDefaultNamespace("diamond_boots"), 0f));
				PlayerEvents.onPlayerTick(player);
			});
			RingHudScenario.active = true;
			waiting = 0;
			phase = 1;
		} else if (phase == 1) {
			if (!operation.isDone()) return false;
			operation.join();
			var bar = ((GuiFeedbackTestAccess) client.gui).heavyinventories$contextualBar().getRight();
			if (++waiting > 240)
				throw new AssertionError("Contextual bar/fixture did not settle: " + NAMES[index] + ", bar=" + bar.getClass().getSimpleName() + ", weight=" + PlayerHolder.getOrCreate(client.player).getWeight() + ", XP=" + client.player.experienceLevel + ", health=" + client.player.getMaxHealth() + ", armor=" + client.player.getArmorValue());
			if (!bar.getClass().getSimpleName().equals(BARS[index])) return false;
			if (client.player.experienceLevel != 30) return false;
			if (PlayerHolder.getOrCreate(client.player).getWeight() != (index == 4 ? 1100 : 900)) return false;
			if (index == 1 && client.player.jumpableVehicle() == null) return false;
			if (index >= 3 && (client.player.getMaxHealth() != 60 || client.player.getArmorValue() != 20)) return false;
			ringStart = RingHudScenario.ringFrames;
			xpStart = RingHudScenario.xpFrames;
			waiting = 0;
			phase = 2;
		} else if (phase == 2) {
			if (++waiting < 25) return false;
			if (RingHudScenario.ringFrames <= ringStart || RingHudScenario.xpFrames <= xpStart) throw new AssertionError("Ring or vanilla XP missing over " + NAMES[index]);
			if (index >= 3 && (client.getWindow().getScreenWidth() != (index == 3 ? 800 : 1600) || client.getWindow().getScreenHeight() != (index == 3 ? 600 : 900))) throw new AssertionError("Window fixture did not resize");
			captured = false;
			Screenshot.grab(Services.PLATFORM.getGameDirectory().toFile(), "ring-" + NAMES[index] + ".png", client.getMainRenderTarget(), 1, message -> captured = true);
			phase = 3;
		} else if (phase == 3 && captured) {
			index++;
			phase = 0;
		}
		return false;
	}
}
