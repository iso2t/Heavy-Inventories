package com.iso2t.heavyinventories.api.movement;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import com.iso2t.heavyinventories.api.player.PlayerHolder;

public class ModifyPlayerMove {

    private static final float SUREFOOTED_OVERENCUMBERED_SPEED = 0.1f; // 10% when overencumbered
    private static final float MIN_FLOOR_WITH_SUREFOOTED = 0.25f; // 25% when encumbered
    private static final float ENCUMBRANCE_CURVE_K = 0.5f;

    public static void hook (Player player, Input input) {
        if (player.isCreative()) return;

        var holder = PlayerHolder.getOrCreate(player);
        boolean over = holder.isOverEncumbered();

        float factor = Mth.clamp(holder.getEncumberedPercentage() / 100f, 0f, 1f);

        // Surefooted floor per level (1.0f means none)
        float multiplier = getMultiplier(holder, over, factor);


		// TODO: This does not work as it is no longer stored as values.
		// TODO: Figure it out ok ty future me!!! 060526
        //input.forwardImpulse *= multiplier;
        //input.leftImpulse *= multiplier;
    }

    private static float getMultiplier(PlayerHolder holder, boolean over, float factor) {
        float surefootedFloor = holder.getSureFootedMult();
        boolean hasSurefooted = surefootedFloor < 1.0f;

        float multiplier;
        if (over) {
            multiplier = hasSurefooted ? SUREFOOTED_OVERENCUMBERED_SPEED : 0f;
        } else {
            multiplier = (float) Math.pow(1f - factor, ENCUMBRANCE_CURVE_K);

            if (hasSurefooted) {
                multiplier = Math.max(multiplier, Math.max(surefootedFloor, MIN_FLOOR_WITH_SUREFOOTED));
            }
        }

        multiplier = Mth.clamp(multiplier, 0f, 1f);
        return multiplier;
    }
}
