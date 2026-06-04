package dev.rifo.spraddition.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;

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
            .comment("Maximum distance the player can be from the ragdoll before the grab breaks. Default is 5.0.")
            .defineInRange("grabMaxDistance", 5.0, 1.0, 100.0);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private SPRAdditionConfig() {}

    public static void register(ModContainer container) {
        container.registerConfig(Type.SERVER, SPEC);
    }

    public static void onLoad(Loading event) {
        if (event.getConfig().getSpec() == SPEC) apply();
    }

    public static void onReload(Reloading event) {
        if (event.getConfig().getSpec() == SPEC) apply();
    }

    private static void apply() {
        SPRAdditionSettings.setSpawnRagdollOnDeath(SPAWN_RAGDOLL_ON_DEATH.get());
        SPRAdditionSettings.setTransferInventoryToRagdoll(TRANSFER_INVENTORY_TO_RAGDOLL.get());
        SPRAdditionSettings.setGrabStiffness(GRAB_STIFFNESS.get());
        SPRAdditionSettings.setGrabDamping(GRAB_DAMPING.get());
        SPRAdditionSettings.setGrabMaxForce(GRAB_MAX_FORCE.get());
        SPRAdditionSettings.setGrabSpeedMultiplier(GRAB_SPEED_MULTIPLIER.get());
        SPRAdditionSettings.setGrabMaxDistance(GRAB_MAX_DISTANCE.get());
    }
}

