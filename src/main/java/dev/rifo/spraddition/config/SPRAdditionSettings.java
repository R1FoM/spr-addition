package dev.rifo.spraddition.config;

public final class SPRAdditionSettings {
    private static boolean spawnRagdollOnDeath = true;
    private static boolean transferInventoryToRagdoll = true;

    private static double grabStiffness = 35.0;
    private static double grabDamping = 15.0;
    private static double grabMaxForce = 80.0;
    private static double grabSpeedMultiplier = 0.5;
    private static double grabMaxDistance = 5.0;

    private SPRAdditionSettings() {}

    public static boolean spawnRagdollOnDeath() {
        return spawnRagdollOnDeath;
    }

    public static void setSpawnRagdollOnDeath(boolean v) {
        spawnRagdollOnDeath = v;
    }

    public static boolean transferInventoryToRagdoll() {
        return transferInventoryToRagdoll;
    }

    public static void setTransferInventoryToRagdoll(boolean v) {
        transferInventoryToRagdoll = v;
    }

    public static double grabStiffness() {
        return grabStiffness;
    }

    public static void setGrabStiffness(double v) {
        grabStiffness = v;
    }

    public static double grabDamping() {
        return grabDamping;
    }

    public static void setGrabDamping(double v) {
        grabDamping = v;
    }

    public static double grabMaxForce() {
        return grabMaxForce;
    }

    public static void setGrabMaxForce(double v) {
        grabMaxForce = v;
    }

    public static double grabSpeedMultiplier() {
        return grabSpeedMultiplier;
    }

    public static void setGrabSpeedMultiplier(double v) {
        grabSpeedMultiplier = v;
    }

    public static double grabMaxDistance() {
        return grabMaxDistance;
    }

    public static void setGrabMaxDistance(double v) {
        grabMaxDistance = v;
    }
}

