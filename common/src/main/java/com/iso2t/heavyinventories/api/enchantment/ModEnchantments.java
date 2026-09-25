package com.iso2t.heavyinventories.api.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import com.iso2t.heavyinventories.HeavyInventories;

public class ModEnchantments {

    public static final ResourceKey<Enchantment> BRACING = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(HeavyInventories.MOD_ID, "bracing"));
    public static final ResourceKey<Enchantment> REINFORCED = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(HeavyInventories.MOD_ID, "reinforced"));
    public static final ResourceKey<Enchantment> SUREFOOTED = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(HeavyInventories.MOD_ID, "surefooted"));

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        var items = context.lookup(Registries.ITEM);

        register(context, BRACING, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ItemTags.CHEST_ARMOR_ENCHANTABLE),
                5,
                10,
                Enchantment.dynamicCost(12, 7),
                Enchantment.dynamicCost(25, 7),
                2,
                EquipmentSlotGroup.CHEST)));

        register(context, REINFORCED, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ItemTags.LEG_ARMOR_ENCHANTABLE),
                5,
                5,
                Enchantment.dynamicCost(8, 9),
                Enchantment.dynamicCost(12, 10),
                2,
                EquipmentSlotGroup.LEGS)));

        register(context, SUREFOOTED, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE),
                5,
                4,
                Enchantment.dynamicCost(22, 11),
                Enchantment.dynamicCost(27, 14),
                2,
                EquipmentSlotGroup.FEET)));
    }

    private static void register(BootstrapContext<Enchantment> registry, ResourceKey<Enchantment> key, Enchantment.Builder builder) {
        registry.register(key, builder.build(key.identifier()));
    }

}
