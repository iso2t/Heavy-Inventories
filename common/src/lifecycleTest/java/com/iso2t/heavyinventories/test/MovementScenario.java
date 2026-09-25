package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.api.enchantment.ModEnchantments;
import com.iso2t.heavyinventories.api.events.PlayerEvents;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.test.mixin.FluidTestAccess;
import com.iso2t.heavyinventories.test.mixin.FluidTravelTestAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;

public final class MovementScenario {
    public static ItemStack enchanted(Player player, Item item, ResourceKey<Enchantment> enchantment, int level) {
        var stack = new ItemStack(item);
        stack.enchant(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment), level);
        return stack;
    }

    public static void run(ServerPlayer player) {
        // Dedicated test worlds persist potion effects between runs.
        if (player.connection != null) {
            player.removeAllEffects();
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().flying = false;
        }
        var state = ServerWeightState.of(player.level().getServer());
        var values = new HashMap<>(state.weights());
        values.put(BuiltInRegistries.ITEM.getKey(Items.STONE), 100f);
        for (var item : new Item[]{Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.IRON_HELMET})
            values.put(BuiltInRegistries.ITEM.getKey(item), 0f);
        state.replace(new ServerSettings(1000, WalkingMode.PROGRESSIVE), values);
        var inventory = player.getInventory();
        inventory.clearContent();
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.inventoryMenu.getCraftSlots().clearContent();
        var holder = PlayerHolder.getOrCreate(player);
        inventory.setItem(0, new ItemStack(Items.STONE, 5));
        PlayerEvents.onPlayerTick(player);
        checkImpulse(player, Math.sqrt(0.5));
        state.replace(new ServerSettings(1000, WalkingMode.AT_NINETY_PERCENT), values);
        PlayerEvents.onPlayerTick(player);
        checkImpulse(player, 1);
        inventory.getItem(0).setCount(9);
        PlayerEvents.onPlayerTick(player);
        require(holder.isEncumbered() && !holder.isOverEncumbered(), "90% boundary mismatch");
        player.setDeltaMovement(Vec3.ZERO);
        player.jumpFromGround();
        require(player.getDeltaMovement().y == 0, "Encumbered jump was allowed");
        inventory.getItem(0).setCount(15);
        PlayerEvents.onPlayerTick(player);
        checkImpulse(player, 0);
        inventory.setItem(36, enchanted(player, Items.IRON_BOOTS, ModEnchantments.SUREFOOTED, 4));
        PlayerEvents.onPlayerTick(player);
        checkImpulse(player, 0.2);
        inventory.getItem(0).setCount(0);
        PlayerEvents.onPlayerTick(player);
        checkImpulse(player, 1);
        inventory.setItem(38, enchanted(player, Items.IRON_CHESTPLATE, ModEnchantments.BRACING, 10));
        inventory.setItem(37, enchanted(player, Items.IRON_LEGGINGS, ModEnchantments.REINFORCED, 5));
        for (int i = 0; i < 5; i++) PlayerEvents.onPlayerTick(player);
        require(holder.getMaxWeight() == 2250, "Bonuses accumulated or failed to stack");
        inventory.setItem(39, new ItemStack(Items.IRON_HELMET));
        PlayerEvents.onPlayerTick(player);
        require(holder.getMaxWeight() == 2250, "Unrelated equipment reset bonuses");
        inventory.setItem(37, ItemStack.EMPTY);
        PlayerEvents.onPlayerTick(player);
        require(holder.getMaxWeight() == 2000, "Removing Reinforced changed Bracing");
        inventory.setItem(38, enchanted(player, Items.IRON_CHESTPLATE, ModEnchantments.BRACING, 1));
        PlayerEvents.onPlayerTick(player);
        require(holder.getMaxWeight() == 1100, "Replacing Bracing retained its old level");
        inventory.setItem(38, ItemStack.EMPTY);
        PlayerEvents.onPlayerTick(player);
        require(holder.getMaxWeight() == 1000, "Removing Bracing did not restore base");
        if (player.connection != null) {
            player.removeAllEffects();
            checkFluidsAndFalls(player);
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1));
            PlayerEvents.onPlayerTick(player);
            require(holder.getMaxWeight() == 1200, "Strength II did not add 20% base");
            player.removeEffect(MobEffects.STRENGTH);
            PlayerEvents.onPlayerTick(player);
            require(holder.getMaxWeight() == 1000, "Expired/removed Strength retained capacity");
            inventory.setItem(0, new ItemStack(Items.STONE, 15));
            for (var mode : new GameType[]{GameType.CREATIVE, GameType.SPECTATOR}) {
                player.setGameMode(mode);
                PlayerEvents.onPlayerTick(player);
                require(!holder.isOverEncumbered() && !holder.isEncumbered(), "Game mode was not exempt");
                checkImpulse(player, 1);
            }
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().flying = true;
            PlayerEvents.onPlayerTick(player);
            checkImpulse(player, 1);
            player.getAbilities().flying = false;
            player.startFallFlying();
            checkImpulse(player, 1);
            player.stopFallFlying();
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 1200, 1));
        }
        inventory.setItem(0, new ItemStack(Items.STONE, 15));
        PlayerEvents.onPlayerTick(player);
        checkImpulse(player, 0.2);
        player.setDeltaMovement(Vec3.ZERO);
        if (player.connection != null) CompatibilityScenario.run(player);
    }

    public static void checkImpulse(Player player, double expected) {
        float oldYaw = player.getYRot();
        var oldMotion = player.getDeltaMovement();
        player.setYRot(0);
        player.setDeltaMovement(Vec3.ZERO);
        player.moveRelative(1, new Vec3(0, 0, 1));
        require(Math.abs(player.getDeltaMovement().horizontalDistance() - expected) < 0.00001, "Straight movement multiplier mismatch");
        player.setDeltaMovement(Vec3.ZERO);
        player.moveRelative(1, new Vec3(1, 0, 1));
        require(Math.abs(player.getDeltaMovement().horizontalDistance() - expected) < 0.00001, "Diagonal movement multiplier mismatch");
        player.setDeltaMovement(Vec3.ZERO);
        player.moveRelative(1, new Vec3(0, 1, 0));
        require(player.getDeltaMovement().y == 1, "Horizontal penalty altered vertical input");
        player.setYRot(oldYaw);
        player.setDeltaMovement(oldMotion);
    }

    private static void checkFluidsAndFalls(ServerPlayer player) {
        var access = (FluidTestAccess) player;
        var travel = (FluidTravelTestAccess) player;
        var position = player.position();
        boolean water = player.isInWater();
        boolean sprinting = player.isSprinting();
        player.setSprinting(false);
        try {
            double[] waterGravity = new double[3];
            double[] lavaGravity = new double[3];
            int[] counts = {5, 9, 15};
            double[] swim = {1, 0.75, 0.5};
            int[] damage = {2, 3, 6};
            for (int i = 0; i < counts.length; i++) {
                player.getInventory().setItem(0, new ItemStack(Items.STONE, counts[i]));
                PlayerEvents.onPlayerTick(player);
                access.heavyinventories$setWater(true);
                checkImpulse(player, swim[i]);
                player.setDeltaMovement(Vec3.ZERO);
                travel.heavyinventories$travelInFluid(Vec3.ZERO);
                waterGravity[i] = player.getDeltaMovement().y;
                access.heavyinventories$setWater(false);
                player.setDeltaMovement(Vec3.ZERO);
                travel.heavyinventories$travelInFluid(Vec3.ZERO);
                lavaGravity[i] = player.getDeltaMovement().y;
                player.setHealth(20);
                player.invulnerableTime = 0;
                player.causeFallDamage(5, 1, player.damageSources().fall());
                require(Math.abs(player.getHealth() - (20 - damage[i])) < 0.001, "Fall damage scaling mismatch");
            }
            for (var gravity : new double[][]{waterGravity, lavaGravity}) {
                require(gravity[0] < 0, "Fluid gravity test did not sink");
                require(Math.abs(gravity[1] / gravity[0] - 1.5) < 0.00001, "Encumbered fluid gravity mismatch");
                require(Math.abs(gravity[2] / gravity[0] - 3) < 0.00001, "Overloaded fluid gravity mismatch");
            }
        } finally {
            access.heavyinventories$setWater(water);
            player.setSprinting(sprinting);
            player.setPos(position);
            player.setDeltaMovement(Vec3.ZERO);
            player.setHealth(20);
            player.invulnerableTime = 0;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
