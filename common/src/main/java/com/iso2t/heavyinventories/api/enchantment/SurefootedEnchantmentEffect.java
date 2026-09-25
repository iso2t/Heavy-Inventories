package com.iso2t.heavyinventories.api.enchantment;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public record SurefootedEnchantmentEffect() implements EnchantmentEntityEffect {

	public static final MapCodec<SurefootedEnchantmentEffect> CODEC = MapCodec.unit(SurefootedEnchantmentEffect::new);

	@Override
	public void apply (@NotNull ServerLevel serverLevel, int enchantmentLevel, @NotNull EnchantedItemInUse enchantedItemInUse, @NotNull Entity entity, @NotNull Vec3 vec3) {
		// Legacy codec retained for datapacks. PlayerHolder rebuilds bonuses from current equipment.
	}

	@Override
	public @NotNull MapCodec<? extends EnchantmentEntityEffect> codec () {
		return CODEC;
	}
}
