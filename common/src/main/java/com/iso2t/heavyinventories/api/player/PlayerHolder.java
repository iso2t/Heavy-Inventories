package com.iso2t.heavyinventories.api.player;

import lombok.Getter;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.network.PlayerWeightPayload;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import com.iso2t.heavyinventories.api.util.Functions;

/**
 * Transient state owned by one player entity, on one logical side.
 * Never copied during respawn or persisted: the new entity rebuilds it from inventory/effects.
 */
public class PlayerHolder {

    // Tunables
    private static final float ENCUMBERED_BASE_MULT = 0.25f; // encumbered speed without boots
    private static final float SUREFOOTED_OVER_PERLVL = 0.05f; // +5%/lvl when over-encumbered (L4=>0.20)
    private static final float SUREFOOTED_ENC_PERLVL = 0.10f; // +10%/lvl when encumbered (L4=>0.40)
    private static final float ENCUMBERED_SWIM_MULT = 0.75f; // 75% horizontal swim speed
    private static final float OVER_ENC_SWIM_MULT = 0.50f; // 50% horizontal swim speed
    private static final float ENCUMBERED_SINK_MULT = 1.5f;  // +50% gravity in fluids
    private static final float OVER_ENC_SINK_MULT = 3.0f;  // +200% gravity in fluids

    private long definitionsRevision = -1;
    private boolean receivedState;
    private boolean syncedEncumbered;
    private boolean syncedOverEncumbered;
    private boolean canEditServerConfig;
    private long serverRevision;
    private PlayerWeightPayload lastSent;
    private long lastSentTick;
    private long lastDefinitionsSent = -1;

    private final PlayerWeightCache weightCache = new PlayerWeightCache();

	@Getter
	private final Player player;

	@Getter
    private float weight;
    private float maxWeight;

    // ENCHANTMENT EFFECTS
	@Getter
    private float bracingOffset;
	private int   bracingAppliedLevel;
	@Getter
	private float reinforcedOffset;
	private int   reinforcedAppliedLevel;
	private int   sureFootedAppliedLevel = 0;
	@Getter
	private float sureFootedMult         = 1.0f; // final movement multiplier floor in [0..1


    public PlayerHolder(Player player) {
        this.player = player;
        this.weight = 0;
        this.maxWeight = ServerSettings.DEFAULT.startingWeight();
        this.bracingOffset = 0f;
        this.reinforcedOffset = 0f;
    }

    /**
     * Main player update method.
     */
    public void update() {
        if (player.level().isClientSide()) return;
        var state = ServerWeightState.of(player.level().getServer());
        if (definitionsRevision != state.revision()) {
            float nextBase = state.settings().startingWeight();
            bracingOffset = (float) ((double) bracingOffset / maxWeight * nextBase);
            reinforcedOffset = (float) ((double) reinforcedOffset / maxWeight * nextBase);
            maxWeight = nextBase;
            definitionsRevision = state.revision();
            weightCache.invalidate();
        }
        weight = PlayerWeightCache.getOrCompute(player);
    }

    PlayerWeightCache weightCache() {
        return weightCache;
    }

    /**
     * Gets the maximum weight the player can carry.
     *
     * @return The maximum weight.
     */
    public float getMaxWeight() {
        return maxWeight + getBracingOffset() + getReinforcedOffset();
    }

    public float getBaseMaxWeight() {
        return maxWeight;
    }

	/**
     * Checks if the player is encumbered.
     * The weight percentage is compared to 90% through 99.9%.
     *
     * @return True if the player is encumbered, false otherwise.
     */
    public boolean isEncumbered() {
        if (player.level().isClientSide()) return receivedState && syncedEncumbered;
        if (getPlayer().isCreative()) return false;

        // Allow the percentage range to be 100%-110% with strength potion.
        if (hasStrength()) return getEncumberedPercentage() >= 100 && getEncumberedPercentage() < 110;

        return getEncumberedPercentage() >= 90 && getEncumberedPercentage() < 100;
    }

    /**
     * Checks if the player is over encumbered.
     * The weight percentage is compared to 100%+.
     *
     * @return True if the player is over encumbered, false otherwise.
     */
    public boolean isOverEncumbered() {
        if (player.level().isClientSide()) return receivedState && syncedOverEncumbered;
        if (getPlayer().isCreative()) return false;

        // Allow the percentage range to be 115%-125% with strength potion.
        if (hasStrength()) return getEncumberedPercentage() >= 115 && getEncumberedPercentage() < 125;
        return getEncumberedPercentage() >= 100;
    }

    /**
     * Allows the strength effect to be checked to determine encumbrance.
     *
     * @return True if the player has the strength effect, false otherwise.
     */
    private boolean hasStrength() {
        return getPlayer().hasEffect(MobEffects.INSTANT_DAMAGE);
    }

    public float getEncumberedPercentage() {
        return (float) Math.min(Float.MAX_VALUE, ((double) getWeight() / getMaxWeight()) * 100);
    }

    /**
     * Horizontal swim multiplier when in fluids.
     */
    public float getFluidSwimMultiplier() {
        if (isOverEncumbered()) return OVER_ENC_SWIM_MULT;
        if (isEncumbered()) return ENCUMBERED_SWIM_MULT;
        return 1.0f;
    }

    /**
     * Gravity multiplier used when falling/sinking in fluids.
     */
    public float getFluidSinkGravityMultiplier() {
        if (isOverEncumbered()) return OVER_ENC_SINK_MULT;
        if (isEncumbered()) return ENCUMBERED_SINK_MULT;
        return 1.0f;
    }

    public void applyBracing(int level, float perLevelPct, float capPct) {
        if (level < 0) level = 0;
        if (level == bracingAppliedLevel) return;


        float base = getBaseMaxWeight();
        float pct = Math.min(perLevelPct * level, capPct);
        float offset = (base * pct);

        bracingOffset = offset;
        bracingAppliedLevel = level;
    }

    public void clearBracing() {
        bracingOffset = 0f;
        bracingAppliedLevel = 0;
    }

    public void applyReinforced(int level, float perLevelPct, float capPct) {
        if (level < 0) level = 0;
        if (level == reinforcedAppliedLevel) return;


        float base = getBaseMaxWeight();
        float pct = Math.min(perLevelPct * level, capPct);
        float offset = (base * pct);

        reinforcedOffset = offset;
        reinforcedAppliedLevel = level;
    }

    public void clearReinforced() {
        reinforcedOffset = 0f;
        reinforcedAppliedLevel = 0;
    }

    public void applySureFooted(int level) {
        if (level < 0) level = 0;
        if (level == sureFootedAppliedLevel) return;
        sureFootedAppliedLevel = level;

        float base = Functions.either(isOverEncumbered(), 0.0f, Functions.either(isEncumbered(), ENCUMBERED_BASE_MULT, 1.0f));
        float floor = Functions.either(isOverEncumbered(), SUREFOOTED_ENC_PERLVL * level, Functions.either(isEncumbered(), SUREFOOTED_ENC_PERLVL * level, 1.0f));
        this.sureFootedMult = Math.min(1.0f, Math.max(base, floor));
    }

    public void clearSureFooted() {
        this.sureFootedMult = 1.0f;
        this.sureFootedAppliedLevel = 0;
    }

    /**
     * Gets a player holder for the given player.
     *
     * @param player The player to get the holder for.
     * @return The player holder.
     */
    public static PlayerHolder getOrCreate(Player player) {
        return ((PlayerStateAccess) player).heavyinventories$getHolder();
    }

    public boolean hasServerState() { return !player.level().isClientSide() || receivedState; }
    public boolean canEditServerConfig() { return canEditServerConfig; }
    public long serverRevision() { return serverRevision; }

    public void accept(PlayerWeightPayload snapshot) {
        if (!player.level().isClientSide() || player.getId() != snapshot.entityId()
                || !player.level().dimension().identifier().equals(snapshot.dimension())) return;
        weight = snapshot.weight();
        maxWeight = snapshot.baseCapacity();
        bracingOffset = snapshot.bracing();
        reinforcedOffset = snapshot.reinforced();
        sureFootedMult = snapshot.surefooted();
        syncedEncumbered = snapshot.encumbered();
        syncedOverEncumbered = snapshot.overEncumbered();
        canEditServerConfig = snapshot.canEdit();
        serverRevision = snapshot.revision();
        receivedState = true;
    }

    public void synchronize(net.minecraft.server.level.ServerPlayer target) {
        if (target.connection == null) return;
        var state = ServerWeightState.of(target.level().getServer());
        var platform = com.iso2t.heavyinventories.platform.Services.PLATFORM;
        if (lastDefinitionsSent != state.revision()) {
            state.packets().forEach(packet -> platform.sendToPlayer(target, packet));
            lastDefinitionsSent = state.revision();
        }
        var snapshot = new PlayerWeightPayload(target.getId(), target.level().dimension().identifier(),
                weight, maxWeight, bracingOffset, reinforcedOffset, sureFootedMult, isEncumbered(), isOverEncumbered(),
                com.iso2t.heavyinventories.server.ServerConfiguration.canEdit(target), state.revision());
        if (!snapshot.equals(lastSent) || target.tickCount - lastSentTick >= 20) {
            platform.sendToPlayer(target, snapshot);
            lastSent = snapshot;
            lastSentTick = target.tickCount;
        }
    }

}
