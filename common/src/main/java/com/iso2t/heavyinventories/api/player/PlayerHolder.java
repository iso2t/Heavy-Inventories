package com.iso2t.heavyinventories.api.player;

import com.iso2t.heavyinventories.api.enchantment.ModEnchantments;
import com.iso2t.heavyinventories.api.weight.StackWeight;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import com.iso2t.heavyinventories.network.PlayerWeightPayload;
import com.iso2t.heavyinventories.platform.Services;
import com.iso2t.heavyinventories.server.ServerConfiguration;
import com.iso2t.heavyinventories.server.ServerWeightState;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * One entity's transient derived state. The server rebuilds it; clients consume snapshots.
 */
public final class PlayerHolder {

	@Getter
	private final Player            player;
	private final PlayerWeightCache weightCache       = new PlayerWeightCache();
	@Getter
	private       float             weight;
	private       float             baseCapacity      = ServerSettings.DEFAULT.startingWeight();
	@Getter
	private       float             bracingOffset;
	@Getter
	private       float             reinforcedOffset;
	@Getter
	private       float             strengthOffset;
	private       float             walkingMultiplier = 1;
	private       boolean           encumbered, overloaded, receivedState, canEditServerConfig;
	private WalkingMode walkingMode         = ServerSettings.DEFAULT.walkingMode();
	private long        definitionsRevision = -1, serverRevision;
	private PlayerWeightPayload lastSent;
	private long                lastSentTick, lastDefinitionsSent = -1, lastJumpNotice = Long.MIN_VALUE;

	public PlayerHolder (Player player) {
		this.player = player;
	}

	public void update () {
		if (player.level().isClientSide()) return;
		var state = ServerWeightState.of(player.level().getServer());
		if (definitionsRevision != state.revision()) {
			definitionsRevision = state.revision();
			weightCache.invalidate();
		}
		baseCapacity = state.settings().startingWeight();
		walkingMode = state.settings().walkingMode();
		float nextWeight = PlayerWeightCache.getOrCompute(player);
		if (nextWeight == StackWeight.TOO_COMPLEX && nextWeight != weight && player instanceof ServerPlayer target && target.connection != null) player.sendSystemMessage(Component.translatable("tooltip.heavyinventories.calculation_limit"));
		weight = nextWeight;
		var strength = player.getEffect(MobEffects.STRENGTH);
		var calculated = Encumbrance.calculate(weight, baseCapacity, level(ModEnchantments.BRACING), level(ModEnchantments.REINFORCED), strength == null ? 0 : (int) Math.min(256L, 1L + strength.getAmplifier()), level(ModEnchantments.SUREFOOTED), walkingMode, player.isCreative() || player.isSpectator());
		bracingOffset = calculated.bracing();
		reinforcedOffset = calculated.reinforced();
		strengthOffset = calculated.strength();
		encumbered = calculated.encumbered();
		overloaded = calculated.overloaded();
		walkingMultiplier = calculated.walkingMultiplier();
	}

	private int level (ResourceKey<Enchantment> key) {
		return player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key).map(enchantment -> EnchantmentHelper.getEnchantmentLevel(enchantment, player)).orElse(0);
	}

	PlayerWeightCache weightCache () {
		return weightCache;
	}

	public float getBaseMaxWeight () {
		return baseCapacity;
	}

	public float getMaxWeight () {
		return baseCapacity + bracingOffset + reinforcedOffset + strengthOffset;
	}

	public WalkingMode walkingMode () {
		return walkingMode;
	}

	public boolean hasServerState () {
		return !player.level().isClientSide() || receivedState;
	}

	public boolean canEditServerConfig () {
		return canEditServerConfig;
	}

	public long serverRevision () {
		return serverRevision;
	}

	private boolean exempt () {
		return player.isCreative() || player.isSpectator() || !hasServerState();
	}

	public boolean isEncumbered () {
		return !exempt() && encumbered;
	}

	public boolean isOverEncumbered () {
		return !exempt() && overloaded;
	}

	public float getEncumberedPercentage () {
		return (float) Math.min(Float.MAX_VALUE, (double) weight / getMaxWeight() * 100);
	}

	/**
	 * These modes use their own movement physics. Encumbrance still displays while mounted/gliding.
	 */
	public boolean movementExempt () {
		return exempt() || player.getAbilities().flying || player.isFallFlying() || player.isPassenger();
	}

	public float getWalkingMultiplier () {
		return movementExempt() ? 1 : walkingMultiplier;
	}

	public float getFluidSwimMultiplier () {
		return movementExempt() ? 1 : isOverEncumbered() ? 0.5f : isEncumbered() ? 0.75f : 1;
	}

	public float getFluidSinkGravityMultiplier () {
		return movementExempt() ? 1 : isOverEncumbered() ? 3 : isEncumbered() ? 1.5f : 1;
	}

	public float getFallDamageMultiplier () {
		return exempt() ? 1 : isOverEncumbered() ? 3 : isEncumbered() ? 1.5f : 1;
	}

	public boolean preventsGroundJump () {
		return !movementExempt() && (isEncumbered() || isOverEncumbered());
	}

	/**
	 * Per-entity client feedback throttle; no packets or chat messages for repeated attempts.
	 */
	public boolean allowJumpNotice () {
		long now = player.tickCount;
		if (lastJumpNotice != Long.MIN_VALUE && now >= lastJumpNotice && now - lastJumpNotice < 40) return false;
		lastJumpNotice = now;
		return true;
	}

	public static PlayerHolder getOrCreate (Player player) {
		return ((PlayerStateAccess) player).heavyinventories$getHolder();
	}

	public void accept (PlayerWeightPayload snapshot) {
		if (!player.level().isClientSide() || player.getId() != snapshot.entityId() || !player.level().dimension().identifier().equals(snapshot.dimension())) return;
		weight = snapshot.weight();
		baseCapacity = snapshot.baseCapacity();
		bracingOffset = snapshot.bracing();
		reinforcedOffset = snapshot.reinforced();
		strengthOffset = snapshot.strength();
		walkingMultiplier = snapshot.walkingMultiplier();
		walkingMode = snapshot.walkingMode();
		encumbered = snapshot.encumbered();
		overloaded = snapshot.overEncumbered();
		canEditServerConfig = snapshot.canEdit();
		serverRevision = snapshot.revision();
		receivedState = true;
	}

	public void synchronize (ServerPlayer target) {
		var state = ServerWeightState.of(target.level().getServer());
		if (lastDefinitionsSent != state.revision()) {
			state.packets().forEach(packet -> Services.PLATFORM.sendToPlayer(target, packet));
			lastDefinitionsSent = state.revision();
		}
		var snapshot = new PlayerWeightPayload(target.getId(), target.level().dimension().identifier(), weight, baseCapacity, bracingOffset, reinforcedOffset, strengthOffset, walkingMultiplier, walkingMode, isEncumbered(), isOverEncumbered(), ServerConfiguration.canEdit(target), state.revision());
		if (!snapshot.equals(lastSent) || target.tickCount - lastSentTick >= 20) {
			Services.PLATFORM.sendToPlayer(target, snapshot);
			lastSent = snapshot;
			lastSentTick = target.tickCount;
		}
	}
}
