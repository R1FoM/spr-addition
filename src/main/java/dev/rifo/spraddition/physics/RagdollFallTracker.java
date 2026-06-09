package dev.rifo.spraddition.physics;

import dev.leo.sableplayerragdoll.api.RagdollAPI;
import dev.leo.sableplayerragdoll.physics.RagdollSessionManager;
import dev.rifo.spraddition.config.SPRAdditionSettings;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [EXPERIMENTAL] Tracks physics velocity of each player's live ragdoll body
 * every
 * server tick and applies fall-impact damage either:
 * a) DURING the ragdoll session when a sharp ground-impact deceleration is
 * detected, OR
 * b) When the ragdoll session ends (fallback for soft / undetected landings).
 *
 * <h3>Why not track player.getDeltaMovement()?</h3>
 * During a ragdoll session the player <em>entity</em> is stationary — Sable
 * drives
 * physics in a sub-level. We read the velocity from the physics handle instead.
 *
 * <h3>Impact detection</h3>
 * Each tick we record the ragdoll head's downward speed. If the speed drops to
 * less
 * than {@value #IMPACT_DECEL_RATIO} × the previous tick's speed (and the
 * previous speed
 * was above {@value #IMPACT_MIN_SPEED_BPS} BPS), we treat it as a ground impact
 * and
 * immediately apply {@link #applyImpactDamage}. After an impact the peak is
 * reset to 0
 * so the ragdoll-exit fallback does not double-apply damage. A short cooldown
 * prevents
 * bounce-triggered follow-up damage.
 */
public final class RagdollFallTracker {

    // ── tuning constants
    // ──────────────────────────────────────────────────────────

    /**
     * Minimum pre-impact downward speed (blocks/second) required to trigger damage.
     */
    private static final double IMPACT_MIN_SPEED_BPS = 12.0;

    /**
     * If the current tick's downward speed is below this fraction of the previous
     * tick's speed, treat the deceleration as a ground impact.
     */
    private static final double IMPACT_DECEL_RATIO = 0.4;

    /** Ticks of cooldown after an impact — prevents bounce double-damage. */
    private static final int IMPACT_COOLDOWN_TICKS = 40;

    // ── per-player state
    // ──────────────────────────────────────────────────────────

    /** Peak downward speed (BPS) seen during the current ragdoll session. */
    private static final ConcurrentHashMap<UUID, Double> peakDownSpeedBps = new ConcurrentHashMap<>();

    /**
     * Downward speed (BPS) recorded on the previous tick — used for impact
     * detection.
     */
    private static final ConcurrentHashMap<UUID, Double> prevDownSpeedBps = new ConcurrentHashMap<>();

    /** Remaining cooldown ticks after an in-ragdoll impact. */
    private static final ConcurrentHashMap<UUID, Integer> impactCooldown = new ConcurrentHashMap<>();

    /** Players currently inside a live ragdoll session. */
    private static final Set<UUID> playersInRagdoll = ConcurrentHashMap.newKeySet();

    /**
     * Ticks remaining during which vanilla {@code LivingFallEvent} is suppressed.
     * Set for a few ticks after ragdoll ends to absorb any stale events Sable fires
     * when it repositions the player entity.
     */
    private static final ConcurrentHashMap<UUID, Integer> suppressFallDamageTicks = new ConcurrentHashMap<>();

    /**
     * Players currently receiving synthetic fall damage (so LivingFallEvent is
     * allowed).
     */
    private static final Set<UUID> syntheticDamagePlayers = ConcurrentHashMap.newKeySet();

    private RagdollFallTracker() {
    }

    // ── main tick entry-point
    // ─────────────────────────────────────────────────────

    /** Called from {@code SPRAddition.onServerTick}. */
    public static void tick(MinecraftServer server) {
        // Decay suppression timers.
        if (!suppressFallDamageTicks.isEmpty()) {
            suppressFallDamageTicks.replaceAll((k, v) -> v - 1);
            suppressFallDamageTicks.entrySet().removeIf(e -> e.getValue() <= 0);
        }

        if (!SPRAdditionSettings.fallRagdollEnabled())
            return;

        for (ServerLevel level : server.getAllLevels()) {
            List<ServerPlayer> players = new ArrayList<>(level.players());
            for (ServerPlayer player : players) {
                tickPlayer(player, level);
            }
        }
    }

    // ── per-player logic
    // ──────────────────────────────────────────────────────────

    private static void tickPlayer(ServerPlayer player, ServerLevel level) {
        UUID uuid = player.getUUID();
        ServerSubLevel ragdollSubLevel = RagdollSessionManager.activeRagdollForPlayer(level, uuid);

        // ── CASE 1: player is in a live ragdoll ─────────────────────────────────
        if (ragdollSubLevel != null) {
            SubLevelPhysicsSystem physics = SubLevelPhysicsSystem.get(level);
            if (physics != null) {
                var handle = physics.getPhysicsHandle(ragdollSubLevel);
                if (handle != null && handle.isValid()) {
                    Vector3d vel = handle.getLinearVelocity(new Vector3d());
                    // getLinearVelocity() returns blocks/second (raw Sable physics units).
                    double downSpeed = Math.max(0.0, -vel.y); // positive = falling

                    // Track peak.
                    peakDownSpeedBps.merge(uuid, downSpeed, Math::max);

                    // ── Impact detection ────────────────────────────────────────
                    double prev = prevDownSpeedBps.getOrDefault(uuid, 0.0);
                    Integer cooldown = impactCooldown.get(uuid);

                    if (cooldown != null) {
                        // Tick down the post-impact cooldown.
                        if (cooldown <= 1)
                            impactCooldown.remove(uuid);
                        else
                            impactCooldown.put(uuid, cooldown - 1);

                    } else if (prev >= IMPACT_MIN_SPEED_BPS
                            && downSpeed < prev * IMPACT_DECEL_RATIO) {
                        // Sharp deceleration = ground impact while still in ragdoll.
                        double peak = peakDownSpeedBps.getOrDefault(uuid, prev);
                        if (peak > 0
                                && SPRAdditionSettings.ragdollImpactDamageEnabled()
                                && player.isAlive()) {
                            applyImpactDamage(player, level, peak);
                            // Reset peak so ragdoll-exit does NOT double-apply.
                            peakDownSpeedBps.put(uuid, 0.0);
                            impactCooldown.put(uuid, IMPACT_COOLDOWN_TICKS);
                        }
                    }

                    prevDownSpeedBps.put(uuid, downSpeed);
                }
            }
            playersInRagdoll.add(uuid);
            return;
        }

        // ── CASE 2: ragdoll just ended this tick ─────────────────────────────────
        if (playersInRagdoll.remove(uuid)) {

            // Normalize all rotation / pose fields that Sable may have left at
            // physics-driven values. If the damage below kills the player,
            // RagdollAssemblyHelper.spawn() reads these fields; extreme values
            // cause head stretching / texture corruption in the death ragdoll.
            normalizePlayerState(player);

            // Fallback damage: only fires if in-ragdoll impact detection did NOT
            // already apply damage (peak was reset to 0 after in-ragdoll impact).
            double peak = peakDownSpeedBps.remove(uuid);
            if (peak > 0
                    && SPRAdditionSettings.ragdollImpactDamageEnabled()
                    && player.isAlive()) {
                applyImpactDamage(player, level, peak);
            }

            prevDownSpeedBps.remove(uuid);
            impactCooldown.remove(uuid);
            suppressFallDamageTicks.put(uuid, 5);
            return; // Skip auto-trigger on the same tick ragdoll ends.
        }

        // ── CASE 3: auto-ragdoll trigger ─────────────────────────────────────────
        if (!player.isAlive() || player.isSpectator() || player.isCreative())
            return;
        if (player.onGround() || player.isInWater() || player.isInLava())
            return;

        Vec3 movement = player.getDeltaMovement();
        // movement.y is blocks/tick; ×20 gives blocks/second for threshold comparison.
        double fallSpeedBps = -movement.y * 20.0;
        double threshold = SPRAdditionSettings.fallRagdollSpeedThreshold();

        if (fallSpeedBps >= threshold) {
            RagdollAPI.launch(player, movement);
        }
    }

    // ── helpers
    // ───────────────────────────────────────────────────────────────────

    /**
     * Converts peak ragdoll speed (blocks/second from Sable physics handle) to an
     * equivalent fall height and delegates to {@link ServerPlayer#causeFallDamage}
     * so that armor, Feather Falling, and safe-fall-distance / damage-multiplier
     * attributes are all respected exactly as in vanilla.
     *
     * <pre>
     *   v_tick = peakBps / 20           (blocks/tick)
     *   g      = 0.08                   (blocks/tick², vanilla gravity)
     *   h      = v_tick² / (2 × g)      (equivalent fall height, blocks)
     * </pre>
     */
    private static void applyImpactDamage(ServerPlayer player, ServerLevel level, double peakBps) {
        double vTick = peakBps / 20.0;
        double equivalentHeight = (vTick * vTick) / 0.16; // 2 × 0.08 = 0.16
        float multiplier = (float) SPRAdditionSettings.ragdollImpactDamageMultiplier();

        UUID uuid = player.getUUID();
        syntheticDamagePlayers.add(uuid);
        try {
            player.causeFallDamage((float) equivalentHeight, multiplier, level.damageSources().fall());
        } finally {
            syntheticDamagePlayers.remove(uuid);
        }
    }

    /**
     * Resets rotation and pose to neutral standing values.
     *
     * <p>
     * After a live ragdoll session Sable can leave the player entity with:
     * <ul>
     * <li>{@code xRot} at extreme angles (e.g. −90° face-down)</li>
     * <li>{@code yHeadRot} / {@code yBodyRot} diverged from {@code yRot}</li>
     * <li>A non-STANDING {@link Pose} (e.g. SWIMMING / FALL_FLYING)</li>
     * </ul>
     * All of these are read by {@code RagdollAssemblyHelper.spawn()} when building
     * the death ragdoll and cause head stretching / texture corruption.
     */
    public static void normalizePlayerState(ServerPlayer player) {
        float yRot = player.getYRot();
        player.setXRot(0.0f);
        player.xRotO = 0.0f;
        player.yHeadRot = yRot;
        player.yHeadRotO = yRot;
        player.yBodyRot = yRot;
        player.yBodyRotO = yRot;
        player.yRotO = yRot;
        if (player.getPose() != Pose.STANDING) {
            player.setPose(Pose.STANDING);
        }
    }

    public static boolean shouldSuppressFallDamage(UUID uuid) {
        if (syntheticDamagePlayers.contains(uuid))
            return false;
        return playersInRagdoll.contains(uuid) || suppressFallDamageTicks.containsKey(uuid);
    }

    /** Called when the server stops. */
    public static void resetAll() {
        peakDownSpeedBps.clear();
        prevDownSpeedBps.clear();
        impactCooldown.clear();
        playersInRagdoll.clear();
        suppressFallDamageTicks.clear();
    }
}
