package dev.rifo.spraddition.api;

import dev.leo.sableplayerragdoll.api.PlayerlessDespawnRule;
import dev.leo.sableplayerragdoll.api.PlayerlessRagdollSession;
import dev.leo.sableplayerragdoll.api.RagdollAPI;
import dev.leo.sableplayerragdoll.physics.RagdollExpireHelper;
import dev.rifo.spraddition.physics.SPRAdditionDeathHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class SPRAdditionAPI {

    private SPRAdditionAPI() {}

    @Nullable
    public static dev.leo.sableplayerragdoll.api.RagdollSession launchWithVelocity(ServerPlayer player, double vx, double vy, double vz) {
        return RagdollAPI.launch(player, new Vec3(vx, vy, vz));
    }

    @Nullable
    public static PlayerlessRagdollSession spawnDeathRagdoll(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID headId = SPRAdditionDeathHelper.spawnDeathRagdoll(player, level);
        if (headId == null) return null;
        return getPlayerlessSessionById(level, headId);
    }

    @Nullable
    public static PlayerlessRagdollSession getPlayerlessSessionById(ServerLevel level, UUID headId) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (!(container instanceof ServerSubLevelContainer serverContainer)) return null;
        SubLevel subLevel = serverContainer.getSubLevel(headId);
        if (!(subLevel instanceof ServerSubLevel serverSubLevel) || serverSubLevel.isRemoved()) return null;
        return new ActivePlayerlessRagdollSession(level, serverSubLevel, level.getGameTime());
    }

    public static List<ItemStack> getDeathInventory(ServerPlayer player) {
        return getDeathInventory(SPRAdditionDeathHelper.getLatestDeathRagdoll(player.getUUID()));
    }

    public static List<ItemStack> getDeathInventory(@Nullable UUID ragdollHeadId) {
        if (ragdollHeadId == null) return List.of();
        return SPRAdditionDeathHelper.getInventorySnapshot(ragdollHeadId);
    }

    public static boolean addItemToDeathRagdoll(ServerPlayer player, ItemStack item) {
        return addItemToDeathRagdoll(SPRAdditionDeathHelper.getLatestDeathRagdoll(player.getUUID()), item);
    }

    public static boolean addItemToDeathRagdoll(@Nullable UUID ragdollHeadId, ItemStack item) {
        if (ragdollHeadId == null) return false;
        return SPRAdditionDeathHelper.addItem(ragdollHeadId, item);
    }

    public static void setDeathInventory(ServerPlayer player, List<ItemStack> items) {
        setDeathInventory(SPRAdditionDeathHelper.getLatestDeathRagdoll(player.getUUID()), items);
    }

    public static void setDeathInventory(@Nullable UUID ragdollHeadId, List<ItemStack> items) {
        if (ragdollHeadId == null) return;
        SPRAdditionDeathHelper.setInventory(ragdollHeadId, items);
    }

    public static void clearDeathRagdollInventory(ServerPlayer player) {
        clearDeathRagdollInventory(SPRAdditionDeathHelper.getLatestDeathRagdoll(player.getUUID()));
    }

    public static void clearDeathRagdollInventory(@Nullable UUID ragdollHeadId) {
        if (ragdollHeadId == null) return;
        SPRAdditionDeathHelper.clearInventory(ragdollHeadId);
    }

    public static boolean hasDeathRagdoll(ServerPlayer player) {
        return isDeathRagdoll(SPRAdditionDeathHelper.getLatestDeathRagdoll(player.getUUID()));
    }

    public static boolean isDeathRagdoll(@Nullable UUID ragdollHeadId) {
        return ragdollHeadId != null && SPRAdditionDeathHelper.isDeathRagdoll(ragdollHeadId);
    }

    public static void normalizePlayerState(ServerPlayer player) {
        dev.rifo.spraddition.physics.RagdollFallTracker.normalizePlayerState(player);
    }

    public static void removeAllRagdolls(net.minecraft.server.MinecraftServer server) {
        java.util.Set<UUID> ragdollIds = new java.util.HashSet<>(SPRAdditionDeathHelper.getAllDeathRagdollHeads());
        for (ServerLevel level : server.getAllLevels()) {
            SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(level);
            if (physicsSystem == null) continue;
            SubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container instanceof ServerSubLevelContainer serverContainer) {
                for (UUID id : ragdollIds) {
                    SubLevel subLevel = serverContainer.getSubLevel(id);
                    if (subLevel instanceof ServerSubLevel serverSubLevel && !serverSubLevel.isRemoved()) {
                        RagdollExpireHelper.expireImmediate(physicsSystem, level, serverSubLevel, "api call");
                    }
                }
            }
        }
    }

    public static void removePlayerlessRagdollForPlayer(ServerPlayer player) {
        java.util.Set<UUID> ragdollHeads = SPRAdditionDeathHelper.getAllDeathRagdolls(player.getUUID());
        if (!ragdollHeads.isEmpty()) {
            ServerLevel level = player.serverLevel();
            SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(level);
            if (physicsSystem != null) {
                SubLevelContainer container = SubLevelContainer.getContainer(level);
                if (container instanceof ServerSubLevelContainer serverContainer) {
                    for (UUID headId : new java.util.ArrayList<>(ragdollHeads)) {
                        SubLevel subLevel = serverContainer.getSubLevel(headId);
                        if (subLevel instanceof ServerSubLevel serverSubLevel && !serverSubLevel.isRemoved()) {
                            RagdollExpireHelper.expireImmediate(physicsSystem, level, serverSubLevel, "api call");
                        }
                    }
                }
            }
        }
    }

    private record ActivePlayerlessRagdollSession(ServerLevel level, ServerSubLevel subLevel, long startGameTime) implements PlayerlessRagdollSession {

        @Override
        public UUID id() {
            return subLevel.getUniqueId();
        }

        @Override
        public Vec3 currentVelocity() {
            SubLevelPhysicsSystem sys = SubLevelPhysicsSystem.get(level);
            if (sys == null) return Vec3.ZERO;
            var handle = sys.getPhysicsHandle(subLevel);
            if (handle == null || !handle.isValid()) return Vec3.ZERO;
            var vel = handle.getLinearVelocity(new org.joml.Vector3d());
            return new Vec3(vel.x / 20.0, vel.y / 20.0, vel.z / 20.0);
        }

        @Override
        public long elapsedTicks() {
            return level.getGameTime() - startGameTime;
        }

        @Override
        public void release() {
            SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(level);
            if (physicsSystem != null && !subLevel.isRemoved()) {
                RagdollExpireHelper.expireImmediate(physicsSystem, level, subLevel, "api playerless release");
            }
        }
    }
}

