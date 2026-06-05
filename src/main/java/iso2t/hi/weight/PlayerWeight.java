package iso2t.hi.weight;

import iso2t.hi.core.registries.DataAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PlayerWeight {

	private PlayerWeight () {
	}

	public static float getCurrentWeight (Player player) {
		return player.getData(DataAttachments.CURRENT_WEIGHT);
	}

	public static void setCurrentWeight (Player player, float weight) {
		player.setData(DataAttachments.CURRENT_WEIGHT, Math.max(0f, weight));
		updateEncumbrance(player);
	}

	public static float getMaxWeight (Player player) {
		return player.getData(DataAttachments.MAX_WEIGHT);
	}

	public static void setMaxWeight (Player player, float maxWeight) {
		player.setData(DataAttachments.MAX_WEIGHT, Math.max(1f, maxWeight));
		updateEncumbrance(player);
	}

	public static int getEncumbranceLevel (Player player) {
		return player.getData(DataAttachments.ENCUMBRANCE_LEVEL);
	}

	public static void recalculate (Player player) {
		float total = 0f;

		Inventory inventory = player.getInventory();

		for (ItemStack stack : inventory.getNonEquipmentItems()) {
			total += getStackWeight(stack);
		}

		setCurrentWeight(player, total);
	}

	public static void updateEncumbrance (Player player) {
		float current = getCurrentWeight(player);
		float max = getMaxWeight(player);

		int level = getLevel(current, max);

		player.setData(DataAttachments.ENCUMBRANCE_LEVEL, level);
	}

	private static int getLevel (float current, float max) {
		if (current <= (max * 0.95f) - 1) return  0;
		else if (current <= max * 0.95f) return  1;
		else return 2;
	}

	// TODO: wtf this is ass
	public static void applyEncumbrance (ServerPlayer player) {
		int level = getEncumbranceLevel(player);

		if (level <= 0 || player.isCreative() || player.isSpectator()) {
			return;
		}

		if (level == 1) {
			//player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false, true));
			return;
		}

		//player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, true, false, true));

		//player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 1, true, false, true));
	}

	private static float getStackWeight (ItemStack stack) {
		if (stack.isEmpty()) {
			return 0f;
		}

		float singleItemWeight = ItemWeight.getItemWeight(stack);
		return singleItemWeight * stack.getCount();
	}

}