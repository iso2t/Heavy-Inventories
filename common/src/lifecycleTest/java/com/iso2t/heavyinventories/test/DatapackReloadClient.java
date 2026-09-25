package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.client.WeightDisplay;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.tooltips.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Confirms each server checkpoint over real packets before allowing the next reload. */
public final class DatapackReloadClient {
    private static boolean started, pauseOnLostFocus;
    private static int ticks, checked;

    public static void tick(Minecraft client) {
        if (!started) {
            pauseOnLostFocus = client.options.pauseOnLostFocus;
            client.options.pauseOnLostFocus = false;
            started = true;
        }
        if (client.player == null) return;
        require(++ticks < 1800, "Client reload synchronization timed out at checkpoint " + checked);
        var checkpoint = DatapackLoadingScenario.checkpoint();
        if (checkpoint != null && checkpoint.id() > checked) {
            var holder = PlayerHolder.getOrCreate(client.player);
            var arrow = ClientWeightData.weight(Identifier.parse("minecraft:arrow"));
            var stone = ClientWeightData.weight(Identifier.parse("minecraft:stone"));
            if (!holder.hasServerState() || holder.serverRevision() != checkpoint.revision()
                    || arrow == null || stone == null || arrow != checkpoint.arrow() || stone != checkpoint.stone()
                    || Math.abs(holder.getWeight() - checkpoint.total()) > 0.0001f) return;
            for (int count : new int[]{1, 64}) {
                String expected = WeightDisplay.weight(checkpoint.arrow() * count, ConfigOptions.WEIGHT_MEASURE);
                var tooltip = Tooltip.addTooltips(new java.util.ArrayList<>(), new ItemStack(Items.ARROW, count));
                require(tooltip.stream().anyMatch(line -> line.getString().contains(expected)), "Reloaded tooltip mismatch");
            }
            checked = checkpoint.id();
            DatapackLoadingScenario.acknowledge(checked);
        }
        if (DatapackLoadingScenario.complete()) {
            require(checked == 8, "Not all successful/failed reload checkpoints reached the client");
            HeavyInventories.LOGGER.info("CLIENT DATAPACK RELOAD PASSED: 8 checkpoints, definitions, unchanged inventory totals, single/stack tooltips, failure retention, restoration");
            client.options.pauseOnLostFocus = pauseOnLostFocus;
            client.stop();
        }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
