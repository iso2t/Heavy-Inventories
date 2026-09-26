package com.iso2t.heavyinventories.api.player;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Owns only HI's temporary contribution; vanilla combines and bounds the complete attribute. */
public final class PlayerKnockback {
	public static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("heavyinventories", "carried_weight_knockback");

	private PlayerKnockback () {}

	public static void update (Player player, float amount) {
		if (player.level().isClientSide()) return;
		var attribute = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (attribute == null) return;
		var current = attribute.getModifier(MODIFIER_ID);
		if (amount <= 0 || !player.isAlive()) {
			if (current != null) attribute.removeModifier(MODIFIER_ID);
			return;
		}
		if (current != null && current.amount() == amount && current.operation() == AttributeModifier.Operation.ADD_VALUE) return;
		if (current != null) attribute.removeModifier(MODIFIER_ID);
		attribute.addTransientModifier(new AttributeModifier(MODIFIER_ID, amount, AttributeModifier.Operation.ADD_VALUE));
	}
}
