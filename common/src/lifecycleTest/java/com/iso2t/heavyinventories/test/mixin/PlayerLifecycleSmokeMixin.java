package com.iso2t.heavyinventories.test.mixin;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.events.PlayerEvents;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.api.player.PlayerWeightCache;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.config.ServerSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Only included by gradle/lifecycle-smoke.gradle; never shipped in either mod jar. */
@Mixin(MinecraftServer.class)
public abstract class PlayerLifecycleSmokeMixin {
    @Unique private boolean heavyinventories$tested;

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void heavyinventories$restoreMultiplayerTest(CallbackInfo ci) {
        if (Boolean.getBoolean("heavyinventories.test.multiplayer"))
            com.iso2t.heavyinventories.test.NetworkAuthorityScenario.cleanup((MinecraftServer) (Object) this);
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void heavyinventories$testLifecycle(BooleanSupplier haveTime, CallbackInfo ci) {
        if (Boolean.getBoolean("heavyinventories.test.datapacks")) {
            com.iso2t.heavyinventories.test.DatapackLoadingScenario.tick((MinecraftServer) (Object) this);
            return;
        }
        if (Boolean.getBoolean("heavyinventories.test.multiplayer")) {
            com.iso2t.heavyinventories.test.NetworkAuthorityScenario.serverTick((MinecraftServer) (Object) this);
            return;
        }
        if (heavyinventories$tested) return;
        heavyinventories$tested = true;
        var server = (MinecraftServer) (Object) this;
        if (!server.isDedicatedServer()) return;
        var profile = new GameProfile(UUID.fromString("b80f4e78-1bd2-4b0e-a7de-8e662f03b999"), "LifecycleTest");
        var original = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault());
        var holder = PlayerHolder.getOrCreate(original);
        require(holder == PlayerHolder.getOrCreate(original), "Repeated lookup must retain entity state");
        require(holder.getPlayer() == original, "Holder must reference its exact owning entity");

        var state = ServerWeightState.of(server);
        var values = new java.util.HashMap<>(state.weights());
        values.put(BuiltInRegistries.ITEM.getKey(Items.STONE), 2f);
        values.put(BuiltInRegistries.ITEM.getKey(Items.IRON_CHESTPLATE), 0f);
        values.put(BuiltInRegistries.ITEM.getKey(Items.IRON_LEGGINGS), 0f);
        state.replace(new ServerSettings(1000.5f), values);
        original.getInventory().setItem(0, new ItemStack(Items.STONE, 8));
        PlayerEvents.onPlayerTick(original);
        require(holder.getWeight() == 16f, "Inventory addition must refresh weight");
        original.getInventory().getItem(0).shrink(3);
        PlayerEvents.onPlayerTick(original);
        require(holder.getWeight() == 10f, "In-place inventory mutation must refresh weight");
        original.getInventory().setItem(38, com.iso2t.heavyinventories.test.MovementScenario.enchanted(
                original, Items.IRON_CHESTPLATE, com.iso2t.heavyinventories.api.enchantment.ModEnchantments.BRACING, 1));
        PlayerEvents.onPlayerTick(original);

        // Same UUID, new entity: death/respawn or reconnect must never recover the old holder.
        var replacement = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault());
        var replacementHolder = PlayerHolder.getOrCreate(replacement);
        require(replacementHolder != holder, "Same UUID must not share entity state");
        require(replacementHolder.getPlayer() == replacement, "Replacement must own its holder");
        PlayerEvents.onPlayerTick(replacement);
        require(replacementHolder.getWeight() == 0f, "Empty respawn inventory must start at zero");
        require(replacementHolder.getBracingOffset() == 0f, "Respawn must not copy derived bonuses");

        // keepInventory-style transfer copies stacks, not cached totals or bonuses.
        replacement.getInventory().replaceWith(original.getInventory());
        PlayerEvents.onPlayerTick(replacement);
        require(replacementHolder.getWeight() == 10f, "Transferred inventory must be recalculated");
        replacement.getInventory().clearContent();
        PlayerEvents.onPlayerTick(replacement);
        require(replacementHolder.getWeight() == 0f, "Clearing inventory must refresh weight");
        require(holder.getWeight() == 10f, "New entity must not mutate old entity state");

        // An unchanged inventory must still refresh when the existing entity changes levels.
        values.put(BuiltInRegistries.ITEM.getKey(Items.STONE), 3f);
        state.replace(new ServerSettings(2000.5f), values);
        original.setServerLevel(server.getLevel(Level.NETHER));
        PlayerEvents.onPlayerTick(original);
        require(PlayerHolder.getOrCreate(original) == holder, "Same entity retains ownership across dimensions");
        require(holder.getWeight() == 15f, "Dimension transition must invalidate the cached total");

        require(holder.getBaseMaxWeight() == 2000.5f, "Capacity change must preserve decimals on existing players");
        require(Math.abs(holder.getBracingOffset() - 200.05f) < 0.01f, "Capacity change must rebase bonuses");
        original.getInventory().setItem(37, com.iso2t.heavyinventories.test.MovementScenario.enchanted(
                original, Items.IRON_LEGGINGS, com.iso2t.heavyinventories.api.enchantment.ModEnchantments.REINFORCED, 1));
        PlayerEvents.onPlayerTick(original);
        require(Math.abs(holder.getBracingOffset() - 200.05f) < 0.01f, "Reinforced must not modify Bracing");
        original.getInventory().setItem(37, ItemStack.EMPTY);
        PlayerEvents.onPlayerTick(original);
        require(Math.abs(holder.getMaxWeight() - 2200.55f) < 0.01f, "Removing Reinforced must preserve base and Bracing");
        values.put(BuiltInRegistries.ITEM.getKey(Items.STONE), 4f);
        state.replace(state.settings(), values);
        PlayerWeightCache.markDirty(original);
        require(holder.getWeight() == 15f, "Invalidation must defer calculation until tick/read");
        PlayerEvents.onPlayerTick(original);
        require(holder.getWeight() == 20f, "Dirty inventory must refresh on next tick");
        require(replacementHolder.getWeight() == 0f, "Invalidation must not affect another entity");


        com.iso2t.heavyinventories.test.WeightCalculationScenario.run(original);
        HeavyInventories.LOGGER.info("LIFECYCLE SMOKE PASSED: entity ownership, same-UUID replacement, inventory mutation, copied inventory, level change, deferred invalidation, equipment/cursor/crafting accounting, nested contents, loaded recipe inference");
        com.iso2t.heavyinventories.test.MovementScenario.run(original);
        HeavyInventories.LOGGER.info("SERVER MOVEMENT PASSED: both curves, equipped bonuses, replacement/removal, diagonal input, ground jumping");
        server.halt(false);
    }

    @Unique
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
