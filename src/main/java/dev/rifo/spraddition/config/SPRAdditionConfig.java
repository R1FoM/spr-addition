package dev.rifo.spraddition.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class SPRAdditionConfig {
        private static final Builder BUILDER = new Builder();

        static {
                BUILDER.comment("Sable Player Ragdoll Addition configuration").push("death");
        }

        public static final BooleanValue SPAWN_RAGDOLL_ON_DEATH = BUILDER
                        .comment("When true, a ragdoll is spawned at the player's death location with their skin.")
                        .define("spawnRagdollOnDeath", true);

        public static final BooleanValue TRANSFER_INVENTORY_TO_RAGDOLL = BUILDER
                        .comment("When true, the player's full inventory is stored in the TORSO of the death ragdoll instead of dropping normally. Items drop when the ragdoll expires.")
                        .define("transferInventoryToRagdoll", true);

        public static final BooleanValue AUTO_REMOVE_EMPTY_RAGDOLLS = BUILDER
                        .comment("When true, death ragdolls will be automatically removed after a delay if their inventory is empty.")
                        .define("autoRemoveEmptyRagdolls", true);

        public static final ModConfigSpec.IntValue EMPTY_RAGDOLL_REMOVAL_TIMER = BUILDER
                        .comment("The delay (in seconds) before an empty death ragdoll is automatically removed (if autoRemoveEmptyRagdolls is true). Default is 60.")
                        .defineInRange("emptyRagdollRemovalTimer", 60, 1, 3600);

        public static final ModConfigSpec.IntValue ABSOLUTE_RAGDOLL_REMOVAL_TIMER = BUILDER
                        .comment("The absolute delay (in seconds) before ANY death ragdoll is automatically removed, even if it contains items. Default is 300 (5 minutes). Set to 0 to disable.")
                        .defineInRange("absoluteRagdollRemovalTimer", 300, 0, 86400);

        static {
                BUILDER.pop();
                BUILDER.comment("Ragdoll grabbing physics configuration").push("grab_physics");
        }

        public static final DoubleValue GRAB_STIFFNESS = BUILDER
                        .comment("Stiffness of the grabbing constraint (lower = heavier). Default is 35.0 (original was 70.0).")
                        .defineInRange("grabStiffness", 35.0, 0.0, 1000.0);

        public static final DoubleValue GRAB_DAMPING = BUILDER
                        .comment("Damping of the grabbing constraint. Default is 15.0 (original was 12.0).")
                        .defineInRange("grabDamping", 15.0, 0.0, 1000.0);

        public static final DoubleValue GRAB_MAX_FORCE = BUILDER
                        .comment("Maximum force of the grabbing constraint (lower = heavier). Default is 80.0 (original was 180.0).")
                        .defineInRange("grabMaxForce", 80.0, 0.0, 1000.0);

        public static final DoubleValue GRAB_SPEED_MULTIPLIER = BUILDER
                        .comment("Movement speed multiplier when grabbing a ragdoll. Default is 0.5 (50% speed).")
                        .defineInRange("grabSpeedMultiplier", 0.5, 0.0, 1.0);

        public static final DoubleValue GRAB_MAX_DISTANCE = BUILDER
                        .comment("Maximum distance the player can be from the ragdoll before the grab breaks. Default is 3.0.")
                        .defineInRange("grabMaxDistance", 3.0, 1.0, 100.0);

        static {
                BUILDER.pop();
                BUILDER.comment("Automatic ragdoll from fall speed and ragdoll impact damage").push("fall_ragdoll");
        }

        public static final BooleanValue FALL_RAGDOLL_ENABLED = BUILDER
                        .comment("When true, the player is automatically put into ragdoll mode when falling faster than the configured speed threshold.")
                        .define("fallRagdollEnabled", true);

        public static final DoubleValue FALL_RAGDOLL_SPEED_THRESHOLD = BUILDER
                        .comment("The downward speed (blocks per second) at which the player is automatically ragdolled. Default is 14.0 (roughly 3+ block freefall).")
                        .defineInRange("fallRagdollSpeedThreshold", 14.0, 1.0, 100.0);

        public static final BooleanValue RAGDOLL_IMPACT_DAMAGE_ENABLED = BUILDER
                        .comment("When true, a player in ragdoll mode takes fall damage upon landing (same formula as normal fall damage). Requires fallRagdollEnabled.")
                        .define("ragdollImpactDamageEnabled", true);

        public static final DoubleValue RAGDOLL_IMPACT_DAMAGE_MULTIPLIER = BUILDER
                        .comment("Multiplier applied to the ragdoll impact damage. 1.0 = same as normal fall damage.")
                        .defineInRange("ragdollImpactDamageMultiplier", 1.3, 0.0, 10.0);

        static {
                BUILDER.pop();
                BUILDER.comment("Ragdoll from nearby explosion").push("explosion_ragdoll");
        }

        public static final BooleanValue EXPLOSION_RAGDOLL_ENABLED = BUILDER
                        .comment("When true, players hit by an explosion are automatically ragdolled and launched in the blast direction.")
                        .define("explosionRagdollEnabled", true);

        public static final DoubleValue EXPLOSION_RAGDOLL_IMPULSE = BUILDER
                        .comment("Impulse scale applied to the player when ragdolled by an explosion. Default is 28.0.")
                        .defineInRange("explosionRagdollImpulse", 28.0, 0.0, 200.0);

        public static final DoubleValue EXPLOSION_EXISTING_RAGDOLL_MULTIPLIER = BUILDER
                        .comment("Multiplier for impulse applied to an already existing ragdoll hit by an explosion. Default is 1.0.")
                        .defineInRange("explosionExistingRagdollMultiplier", 1.0, 0.0, 10.0);

        static {
                BUILDER.pop();
        }

        public static final ModConfigSpec SPEC = BUILDER.build();

        private SPRAdditionConfig() {
        }

        public static void register(ModContainer container) {
                container.registerConfig(Type.SERVER, SPEC);
        }

        public static void onLoad(Loading event) {
                if (event.getConfig().getSpec() == SPEC)
                        apply();
        }

        public static void onReload(Reloading event) {
                if (event.getConfig().getSpec() == SPEC)
                        apply();
        }

        private static void apply() {
                SPRAdditionSettings.setSpawnRagdollOnDeath(SPAWN_RAGDOLL_ON_DEATH.get());
                SPRAdditionSettings.setTransferInventoryToRagdoll(TRANSFER_INVENTORY_TO_RAGDOLL.get());
                SPRAdditionSettings.setAutoRemoveEmptyRagdolls(AUTO_REMOVE_EMPTY_RAGDOLLS.get());
                SPRAdditionSettings.setEmptyRagdollRemovalTimer(EMPTY_RAGDOLL_REMOVAL_TIMER.get());
                SPRAdditionSettings.setAbsoluteRagdollRemovalTimer(ABSOLUTE_RAGDOLL_REMOVAL_TIMER.get());
                SPRAdditionSettings.setGrabStiffness(GRAB_STIFFNESS.get());
                SPRAdditionSettings.setGrabDamping(GRAB_DAMPING.get());
                SPRAdditionSettings.setGrabMaxForce(GRAB_MAX_FORCE.get());
                SPRAdditionSettings.setGrabSpeedMultiplier(GRAB_SPEED_MULTIPLIER.get());
                SPRAdditionSettings.setGrabMaxDistance(GRAB_MAX_DISTANCE.get());
                SPRAdditionSettings.setFallRagdollEnabled(FALL_RAGDOLL_ENABLED.get());
                SPRAdditionSettings.setFallRagdollSpeedThreshold(FALL_RAGDOLL_SPEED_THRESHOLD.get());
                SPRAdditionSettings.setRagdollImpactDamageEnabled(RAGDOLL_IMPACT_DAMAGE_ENABLED.get());
                SPRAdditionSettings.setRagdollImpactDamageMultiplier(RAGDOLL_IMPACT_DAMAGE_MULTIPLIER.get());
                SPRAdditionSettings.setExplosionRagdollEnabled(EXPLOSION_RAGDOLL_ENABLED.get());
                SPRAdditionSettings.setExplosionRagdollImpulse(EXPLOSION_RAGDOLL_IMPULSE.get());
                SPRAdditionSettings.setExplosionExistingRagdollMultiplier(EXPLOSION_EXISTING_RAGDOLL_MULTIPLIER.get());
        }
}
