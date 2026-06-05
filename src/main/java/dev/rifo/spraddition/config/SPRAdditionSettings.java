package dev.rifo.spraddition.config;

public final class SPRAdditionSettings {
    private static boolean spawnRagdollOnDeath = true;
    private static boolean transferInventoryToRagdoll = true;
    private static boolean autoRemoveEmptyRagdolls = true;
    private static int emptyRagdollRemovalTimer = 60;
    private static int absoluteRagdollRemovalTimer = 300;

    private static double grabStiffness = 35.0;
    private static double grabDamping = 15.0;
    private static double grabMaxForce = 80.0;
    private static double grabSpeedMultiplier = 0.5;
    private static double grabMaxDistance = 3.0;

    // Fall ragdoll
    private static boolean fallRagdollEnabled = true;
    private static double fallRagdollSpeedThreshold = 14.0;
    private static boolean ragdollImpactDamageEnabled = true;
    private static double ragdollImpactDamageMultiplier = 1.3;

    private SPRAdditionSettings() {
    }

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

    public static boolean autoRemoveEmptyRagdolls() {
        return autoRemoveEmptyRagdolls;
    }

    public static void setAutoRemoveEmptyRagdolls(boolean v) {
        autoRemoveEmptyRagdolls = v;
    }

    public static int emptyRagdollRemovalTimer() {
        return emptyRagdollRemovalTimer;
    }

    public static void setEmptyRagdollRemovalTimer(int v) {
        emptyRagdollRemovalTimer = v;
    }

    public static int absoluteRagdollRemovalTimer() {
        return absoluteRagdollRemovalTimer;
    }

    public static void setAbsoluteRagdollRemovalTimer(int v) {
        absoluteRagdollRemovalTimer = v;
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

    // Fall ragdoll

    public static boolean fallRagdollEnabled() {
        return fallRagdollEnabled;
    }

    public static void setFallRagdollEnabled(boolean v) {
        fallRagdollEnabled = v;
    }

    public static double fallRagdollSpeedThreshold() {
        return fallRagdollSpeedThreshold;
    }

    public static void setFallRagdollSpeedThreshold(double v) {
        fallRagdollSpeedThreshold = v;
    }

    public static boolean ragdollImpactDamageEnabled() {
        return ragdollImpactDamageEnabled;
    }

    public static void setRagdollImpactDamageEnabled(boolean v) {
        ragdollImpactDamageEnabled = v;
    }

    public static double ragdollImpactDamageMultiplier() {
        return ragdollImpactDamageMultiplier;
    }

    public static void setRagdollImpactDamageMultiplier(double v) {
        ragdollImpactDamageMultiplier = v;
    }
}
