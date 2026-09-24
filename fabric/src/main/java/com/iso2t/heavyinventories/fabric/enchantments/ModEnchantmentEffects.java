package com.iso2t.heavyinventories.fabric.enchantments;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.enchantment.BracingEnchantmentEffect;
import com.iso2t.heavyinventories.api.enchantment.ReinforcedEnchantmentEffect;
import com.iso2t.heavyinventories.api.enchantment.SurefootedEnchantmentEffect;

public class ModEnchantmentEffects {

    public static final MapCodec<BracingEnchantmentEffect> BRACING = register("bracing", BracingEnchantmentEffect.CODEC);
    public static final MapCodec<ReinforcedEnchantmentEffect> REINFORCED = register("reinforced", ReinforcedEnchantmentEffect.CODEC);
    public static final MapCodec<SurefootedEnchantmentEffect> SUREFOOTED = register("surefooted", SurefootedEnchantmentEffect.CODEC);

    private static <E extends EnchantmentEntityEffect> MapCodec<E> register(String id, MapCodec<E> codec) {
        return Registry.register(BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE, Identifier.fromNamespaceAndPath(HeavyInventories.MOD_ID, id), codec);
    }

    public static void register() {
        HeavyInventories.LOGGER.info("Initializing enchantment effects");
    }
}
