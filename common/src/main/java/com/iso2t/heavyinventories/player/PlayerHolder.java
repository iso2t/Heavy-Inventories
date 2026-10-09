package com.iso2t.heavyinventories.player;

import com.iso2t.heavyinventories.api.EncumbranceState;
import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.WeightResult;
import com.iso2t.heavyinventories.config.EffectsSettings;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import com.iso2t.heavyinventories.enchantment.ModEnchantments;
import com.iso2t.heavyinventories.integration.CommonPlugins;
import com.iso2t.heavyinventories.integration.GameplayProviders;
import com.iso2t.heavyinventories.integration.Notifications;
import com.iso2t.heavyinventories.network.PlayerWeightPayload;
import com.iso2t.heavyinventories.platform.Services;
import com.iso2t.heavyinventories.server.ServerConfiguration;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.weight.StackWeight;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Optional;

/**
 * One entity's transient derived state. The server rebuilds it; clients consume snapshots.
 */
@RequiredArgsConstructor
public final class PlayerHolder {

	private static final int EXTERNAL_MOMENTUM_GRACE_TICKS = 20;
	private static final int JUMP_NOTICE_COOLDOWN_TICKS    = 40;

	@Getter
	private final Player               player;

	@Getter(AccessLevel.PACKAGE)
	@Accessors(fluent = true)
	private final PlayerWeightCache    weightCache                       = new PlayerWeightCache();

	@Getter
	private       float                weight;
	private       float                baseCapacity                      = ServerSettings.DEFAULT.startingWeight();

	@Getter
	private       float                bracingOffset;

	@Getter
	private       float                reinforcedOffset;

	@Getter
	private       float                strengthOffset;
	private       float                additionalCapacity;
	private       float                walkingMultiplier                 = 1;
	private       int                  soaringLevel;
	private       boolean              encumbered;
	private       boolean              overloaded;
	private       boolean              receivedState;
	private       PlayerWeightSnapshot apiSnapshot;
	private       PlayerWeightSnapshot notifiedSnapshot;

	@Getter
	@Accessors(fluent = true)
	private       boolean              canEditServerConfig;

	@Getter
	@Accessors(fluent = true)
	private       WalkingMode          walkingMode                       = ServerSettings.DEFAULT.walkingMode();
	private       EffectsSettings      effectSettings                    = ServerSettings.DEFAULT.effects();
	private       long                 definitionsRevision               = -1;

	@Getter
	@Accessors(fluent = true)
	private       long                 serverRevision;
	private       PlayerWeightPayload  lastSent;
	private       long                 lastDefinitionsSent               = -1;
	private       long                 lastJumpNotice                    = Long.MIN_VALUE;
	private       long                 movementExhaustionSuppressedUntil = Long.MIN_VALUE;

	public void update () {
		if (player.level().isClientSide()) return;
		GameplayProviders.checkCalculation();
		Notifications.checkCommit();
		var state = ServerWeightState.of(player.level().getServer());
		if (definitionsRevision != state.revision()) {
			definitionsRevision = state.revision();
			weightCache.invalidate();
		}
		baseCapacity = state.settings().startingWeight();
		walkingMode = state.settings().walkingMode();
		effectSettings = state.settings().effects();
		float nextWeight = PlayerWeightCache.getOrCompute(player);
		if (nextWeight == StackWeight.TOO_COMPLEX && nextWeight != weight && player instanceof ServerPlayer target && target.connection != null) {
			player.sendSystemMessage(Component.translatable("chat.heavyinventories.incomplete_weight"));
		}
		weight = nextWeight;
		var strength = player.getEffect(MobEffects.STRENGTH);
		int strengthLevel = strength == null ? 0 : (int) Math.min(256L, 1L + strength.getAmplifier());
		boolean gameModeExempt = player.isCreative() || player.isSpectator();
		int bracing = level(ModEnchantments.BRACING);
		int reinforced = level(ModEnchantments.REINFORCED);
		int surefooted = level(ModEnchantments.SUREFOOTED);
		var calculated = Encumbrance.calculate(weight, baseCapacity, bracing, reinforced, strengthLevel, surefooted, walkingMode, gameModeExempt);
		additionalCapacity = player instanceof ServerPlayer target ? CommonPlugins.INSTANCE.providers().capacity(target, calculated.capacity()) : 0;
		if (additionalCapacity != 0) calculated = Encumbrance.calculate(weight, baseCapacity, bracing, reinforced, strengthLevel, surefooted, walkingMode, gameModeExempt, additionalCapacity);
		bracingOffset = calculated.bracing();
		reinforcedOffset = calculated.reinforced();
		strengthOffset = calculated.strength();
		encumbered = calculated.encumbered();
		overloaded = calculated.overloaded();
		walkingMultiplier = calculated.walkingMultiplier();
		var chest = player.getItemBySlot(EquipmentSlot.CHEST);
		soaringLevel = chest.is(Items.ELYTRA) ? player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ModEnchantments.SOARING).map(enchantment -> Math.clamp(EnchantmentHelper.getItemEnchantmentLevel(enchantment, chest), 0, 4)).orElse(0) : 0;
		float resistance = effectSettings.knockback().enabled() ? EncumbranceEffects.calculate(weight, getMaxWeight(), walkingMode, effectSettings, EncumbranceEffects.Fluid.NONE, gameModeExempt, false).knockbackResistance() : 0;
		PlayerKnockback.update(player, resistance);
		captureApiSnapshot(state.revision());
		if (player instanceof ServerPlayer target && !state.stopped() && !Notifications.sameValues(notifiedSnapshot, apiSnapshot)) {
			var previous = Optional.ofNullable(notifiedSnapshot);
			notifiedSnapshot = apiSnapshot;
			var current = apiSnapshot;
			state.notifications().dispatch("server player", CommonPlugins.INSTANCE.registrations().playerChanged(), listener -> listener.changed(target, previous, current));
		}
	}

	private int level (ResourceKey<Enchantment> key) {
		return player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key).map(enchantment -> EnchantmentHelper.getEnchantmentLevel(enchantment, player)).orElse(0);
	}

	public float getBaseMaxWeight () {
		return baseCapacity;
	}

	public float getMaxWeight () {
		return (float) ((double) (baseCapacity + bracingOffset + reinforcedOffset + strengthOffset) + additionalCapacity);
	}

	public ServerSettings serverSettings () {
		return new ServerSettings(baseCapacity, walkingMode, effectSettings);
	}

	public void suppressMovementExhaustion () {
		movementExhaustionSuppressedUntil = (long) player.tickCount + EXTERNAL_MOMENTUM_GRACE_TICKS;
	}

	public boolean movementExhaustionSuppressed () {
		return player.tickCount < movementExhaustionSuppressedUntil;
	}

	public boolean hasServerState () {
		return !player.level().isClientSide() || receivedState;
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

	public ElytraFlight.State elytraEffects () {
		if (exempt() || player.getAbilities().flying || player.isPassenger() || !player.isFallFlying()) return ElytraFlight.State.NONE;
		var settings = player.level().isClientSide() ? effectSettings : ServerWeightState.of(player.level().getServer()).settings().effects();
		return ElytraFlight.calculate(weight, settings.elytra(), soaringLevel, false);
	}

	public float getFluidSwimMultiplier () {
		return fluidEffects().swimmingMultiplier();
	}

	public float getFluidSinkGravityMultiplier () {
		return fluidEffects().sinkingMultiplier();
	}

	public boolean preventsFluidAscent () {
		return fluidEffects().deniesUpwardMovement();
	}

	private EncumbranceEffects.State fluidEffects () {
		var fluid = player.isInWater() ? EncumbranceEffects.Fluid.WATER : player.isInLava() ? EncumbranceEffects.Fluid.LAVA : EncumbranceEffects.Fluid.NONE;
		if (fluid == EncumbranceEffects.Fluid.NONE || movementExempt()) return EncumbranceEffects.State.NONE;
		var settings = player.level().isClientSide() ? serverSettings() : ServerWeightState.of(player.level().getServer()).settings();
		var effects = settings.effects();
		if (!effects.swimming().enabled() && !effects.sinking().enabled() && !effects.upwardMovement().enabled()) return EncumbranceEffects.State.NONE;
		return EncumbranceEffects.calculate(weight, getMaxWeight(), settings.walkingMode(), settings.effects(), fluid, exempt(), movementExempt());
	}

	public float getFallDamageMultiplier () {
		var settings = player.level().isClientSide() ? serverSettings() : ServerWeightState.of(player.level().getServer()).settings();
		if (!settings.effects().fallDamage().enabled()) return 1;
		return EncumbranceEffects.calculate(weight, getMaxWeight(), settings.walkingMode(), settings.effects(), EncumbranceEffects.Fluid.NONE, exempt(), false).fallMultiplier();
	}

	public boolean preventsGroundJump () {
		return preventsFluidAscent() || !movementExempt() && (isEncumbered() || isOverEncumbered());
	}

	/**
	 * Per-entity client feedback throttle; no packets or chat messages for repeated attempts.
	 */
	public boolean allowJumpNotice () {
		long now = player.tickCount;
		if (lastJumpNotice != Long.MIN_VALUE && now >= lastJumpNotice && now - lastJumpNotice < JUMP_NOTICE_COOLDOWN_TICKS) return false;
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
		additionalCapacity = snapshot.additionalCapacity();
		walkingMultiplier = snapshot.walkingMultiplier();
		walkingMode = snapshot.walkingMode();
		effectSettings = snapshot.effects();
		soaringLevel = snapshot.soaringLevel();
		encumbered = snapshot.encumbered();
		overloaded = snapshot.overEncumbered();
		canEditServerConfig = snapshot.canEdit();
		serverRevision = snapshot.revision();
		receivedState = true;
		captureApiSnapshot(snapshot.revision());
	}

	public PlayerWeightSnapshot apiSnapshot () {
		if (apiSnapshot == null || !player.level().isClientSide()) return apiSnapshot;
		var status = isOverEncumbered() ? EncumbranceState.OVERLOADED : isEncumbered() ? EncumbranceState.ENCUMBERED : EncumbranceState.NORMAL;
		if (apiSnapshot.state() == status && apiSnapshot.walkingMultiplier() == getWalkingMultiplier() && apiSnapshot.effectsApply() == !exempt()) return apiSnapshot;
		return new PlayerWeightSnapshot(apiSnapshot.carriedWeight(), apiSnapshot.baseCapacity(), apiSnapshot.capacity(), status, getWalkingMultiplier(), !exempt(), apiSnapshot.revision(), apiSnapshot.tick());
	}

	private void captureApiSnapshot (long revision) {
		var carried = weight == StackWeight.TOO_COMPLEX ? WeightResult.incomplete() : WeightResult.complete(weight);
		var status = isOverEncumbered() ? EncumbranceState.OVERLOADED : isEncumbered() ? EncumbranceState.ENCUMBERED : EncumbranceState.NORMAL;
		apiSnapshot = new PlayerWeightSnapshot(carried, baseCapacity, getMaxWeight(), status, getWalkingMultiplier(), !exempt(), revision, Math.max(0, player.level().getGameTime()));
	}

	public void synchronize (ServerPlayer target) {
		var state = ServerWeightState.of(target.level().getServer());
		if (lastDefinitionsSent != state.revision()) {
			state.packets().forEach(packet -> Services.PLATFORM.sendToPlayer(target, packet));
			lastDefinitionsSent = state.revision();
		}
		var snapshot = new PlayerWeightPayload(target.getId(), target.level().dimension().identifier(), weight, baseCapacity, bracingOffset, reinforcedOffset, strengthOffset, walkingMultiplier, walkingMode, isEncumbered(), isOverEncumbered(), ServerConfiguration.canEdit(target), state.revision(), soaringLevel, effectSettings, additionalCapacity);
		if (!snapshot.equals(lastSent)) {
			Services.PLATFORM.sendToPlayer(target, snapshot);
			lastSent = snapshot;
		}
	}

}
