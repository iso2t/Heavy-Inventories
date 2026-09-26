package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.api.player.PlayerKnockback;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.test.mixin.ArrowKnockbackTestAccess;
import com.iso2t.heavyinventories.test.mixin.ExplosionKnockbackTestAccess;
import com.iso2t.heavyinventories.test.mixin.FluidTravelTestAccess;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.UUID;

public final class KnockbackScenario {
	public static void run (ServerPlayer anchor) {
		var server = anchor.level().getServer();
		var level = server.overworld();
		var state = ServerWeightState.of(server);
		var savedSettings = state.settings();
		var savedWeights = state.weights();
		var weights = new HashMap<>(savedWeights);
		weights.put(BuiltInRegistries.ITEM.getKey(Items.STONE), 25f);
		weights.put(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_CHESTPLATE), 0f);
		var profile = new GameProfile(UUID.fromString("b80f4e78-1bd2-4b0e-a7de-8e662f03b995"), "KnockbackTest");
		var player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
		new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false)) {
			@Override public boolean hasClientLoaded () { return true; }
		};
		var holder = PlayerHolder.getOrCreate(player);
		var attribute = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		try {
			state.replace(new ServerSettings(1000), weights);
			player.setGameMode(GameType.SURVIVAL);
			player.getAbilities().flying = false;
			player.setPos(8.5, level.getMaxY() + 10, 8.5);
			var bow = MovementScenario.enchanted(player, Items.BOW, Enchantments.PUNCH, 2);
			var arrow = new Arrow(level, 8, player.getY(), 8, new ItemStack(Items.ARROW), bow);
			arrow.setDeltaMovement(1, 0, 0);
			double unloadedArrow = 0;
			int[] counts = {0, 10, 20, 40, 60};
			for (int count : counts) {
				load(player, count);
				double bonus = .4 * Math.min(count * 25 / 1000.0, 1);
				checkBonus(player, bonus);
				close(bonus, attribute.getValue(), "resistance curve");
				player.setOnGround(false);
				player.setDeltaMovement(Vec3.ZERO);
				player.knockback(1, 1, 0);
				close(1 - bonus, player.getDeltaMovement().horizontalDistance(), "ordinary knockback");
				player.setDeltaMovement(Vec3.ZERO);
				((ArrowKnockbackTestAccess) arrow).heavyinventories$knockback(player, player.damageSources().arrow(arrow, null));
				double impulse = player.getDeltaMovement().horizontalDistance();
				if (count == 0) {
					unloadedArrow = impulse;
					if (!(impulse > 0)) throw new AssertionError("Punch arrow fixture has no impulse");
				}
				close(unloadedArrow * (1 - bonus), impulse, "Punch arrow knockback");
			}
			var unchanged = attribute.getModifier(PlayerKnockback.MODIFIER_ID);
			for (int i = 0; i < 10; i++) holder.update();
			if (attribute.getModifier(PlayerKnockback.MODIFIER_ID) != unchanged) throw new AssertionError("Unchanged modifier was replaced");
			if (attribute.pack().modifiers().stream().anyMatch(m -> m.id().equals(PlayerKnockback.MODIFIER_ID))) throw new AssertionError("HI modifier would persist to disk");
			if (!player.getActiveEffects().isEmpty()) throw new AssertionError("Resistance added a status effect");
			load(player, 20);
			player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 2));
			holder.update();
			checkBonus(player, .2);
			state.replace(new ServerSettings(100), weights);
			holder.update();
			checkBonus(player, .2);
			player.removeAllEffects();
			state.replace(new ServerSettings(1000), weights);
			load(player, 40);
			player.getInventory().setItem(38, new ItemStack(Items.NETHERITE_CHESTPLATE));
			((FluidTravelTestAccess) player).heavyinventories$refreshEquipment();
			holder.update();
			close(.5, attribute.getValue(), "netherite stacks with HI");
			var otherId = Identifier.fromNamespaceAndPath("lifecycle_test", "other_resistance");
			attribute.setBaseValue(.15);
			attribute.addPermanentModifier(new AttributeModifier(otherId, .6, AttributeModifier.Operation.ADD_VALUE));
			holder.update();
			close(1, attribute.getValue(), "vanilla caps combined resistance");
			var custom = state.settings().toJson();
			var knockback = custom.getAsJsonObject("effects").getAsJsonObject("knockback");
			knockback.addProperty("enabled", false);
			state.replace(ServerSettings.parse(custom), weights);
			holder.update();
			checkBonus(player, 0);
			close(.85, attribute.getValue(), "disable preserves armor/base/other modifier");
			close(.15, attribute.getBaseValue(), "base unchanged");
			if (attribute.getModifier(otherId) == null) throw new AssertionError("Other modifier removed");
			attribute.removeModifier(otherId);
			attribute.setBaseValue(0);
			player.getInventory().setItem(38, ItemStack.EMPTY);
			((FluidTravelTestAccess) player).heavyinventories$refreshEquipment();
			knockback.addProperty("enabled", true);
			knockback.addProperty("referenceWeight", 500);
			knockback.addProperty("maxResistance", .6);
			state.replace(ServerSettings.parse(custom), weights);
			load(player, 10);
			checkBonus(player, .3);
			for (var mode : new GameType[] {GameType.CREATIVE, GameType.SPECTATOR}) {
				player.setGameMode(mode);
				holder.update();
				checkBonus(player, 0);
			}
			player.setGameMode(GameType.SURVIVAL);
			player.getAbilities().flying = false;
			holder.update();
			checkBonus(player, .3);
			player.setHealth(0);
			holder.update();
			checkBonus(player, 0);
			player.setHealth(20);
			holder.update();
			checkBonus(player, .3);
			var replacement = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
			PlayerHolder.getOrCreate(replacement).update();
			checkBonus(replacement, 0);
			replacement.getInventory().replaceWith(player.getInventory());
			PlayerHolder.getOrCreate(replacement).update();
			checkBonus(replacement, .3);
			replacement.getInventory().clearContent();
			PlayerHolder.getOrCreate(replacement).update();
			checkBonus(replacement, 0);
			// Explosion entity effects only: no block destruction, sound, fire, or health damage.
			state.replace(new ServerSettings(1000), weights);
			level.addNewPlayer(player);
			var calculator = new ExplosionDamageCalculator() {
				@Override public boolean shouldDamageEntity(Explosion explosion, Entity entity) { return false; }
			};
			double explosionImpulse = 0;
			for (int count : new int[] {0, 40}) {
				load(player, count);
				player.setDeltaMovement(Vec3.ZERO);
				var explosion = new ServerExplosion(level, null, null, calculator, player.position().add(1, 0, 0), 2, false, Explosion.BlockInteraction.KEEP);
				((ExplosionKnockbackTestAccess) explosion).heavyinventories$affectEntities();
				if (count == 0) {
					explosionImpulse = player.getDeltaMovement().length();
					if (!(explosionImpulse > 0)) throw new AssertionError("Explosion fixture did not hit player");
				} else close(explosionImpulse, player.getDeltaMovement().length(), "explosion knockback unchanged");
				close(0, player.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE), "explosion attribute unchanged");
			}
			HeavyInventories.LOGGER.info("KNOCKBACK PASSED: physical weight curve, melee/Punch/explosion paths, equipment stacking, unchanged modifier identity, transient persistence, custom settings, disable, modes, death/replacement");
		} finally {
			level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
			player.getTextFilter().leave();
			state.replace(savedSettings, savedWeights);
		}
	}

	public static void checkBonus (ServerPlayer player, double expected) {
		var modifier = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getModifier(PlayerKnockback.MODIFIER_ID);
		if (expected == 0 && modifier != null) throw new AssertionError("Zero bonus left an HI modifier");
		if (expected != 0 && modifier == null) throw new AssertionError("Missing HI modifier");
		close(expected, modifier == null ? 0 : modifier.amount(), "HI contribution");
	}
	private static void load (ServerPlayer player, int count) {
		player.getInventory().setItem(0, count == 0 ? ItemStack.EMPTY : new ItemStack(Items.STONE, count));
		PlayerHolder.getOrCreate(player).update();
	}
	private static void close (double expected, double actual, String message) {
		if (Math.abs(expected - actual) > .00001) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
	}
}
