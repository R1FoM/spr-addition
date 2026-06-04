package dev.rifo.spraddition.physics;

import dev.leo.sableplayerragdoll.SablePlayerRagdoll;
import dev.leo.sableplayerragdoll.api.PlayerlessDespawnRule;
import dev.leo.sableplayerragdoll.config.RagdollSettings;
import dev.leo.sableplayerragdoll.physics.*;
import dev.rifo.spraddition.config.SPRAdditionSettings;
import dev.rifo.spraddition.mixin.RagdollRegistryAccessor;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SPRAdditionDeathHelper {

    private static final ConcurrentHashMap<UUID, NonNullList<ItemStack>> DEATH_INVENTORIES =
            new ConcurrentHashMap<>();

    private static final Set<UUID> DEATH_RAGDOLL_HEADS = ConcurrentHashMap.newKeySet();

    private static final ConcurrentHashMap<UUID, UUID> PENDING_INVENTORY_SUPPRESS =
            new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<UUID, UUID> LATEST_DEATH_RAGDOLLS = 
            new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<UUID, Set<UUID>> PLAYER_DEATH_RAGDOLLS = 
            new ConcurrentHashMap<>();

    private SPRAdditionDeathHelper() {}

    public static UUID spawnDeathRagdoll(ServerPlayer player, ServerLevel level) {
        if (!RagdollSettings.enabled() || !SPRAdditionSettings.spawnRagdollOnDeath()) {
            return null;
        }

        Vec3 position = player.position();
        double headingDegrees = player.getYRot();

        NonNullList<ItemStack> inventory = captureFullInventory(player);

        ServerSubLevel headSubLevel = spawnDeathRagdollSubLevel(
                level, player, position, headingDegrees, PlayerlessDespawnRule.never()
        );

        if (headSubLevel == null) {
            SablePlayerRagdoll.LOGGER.warn(
                    "[spr_addition] death ragdoll spawn failed for {}", player.getGameProfile().getName());
            return null;
        }

        UUID headId = headSubLevel.getUniqueId();
        DEATH_RAGDOLL_HEADS.add(headId);
        LATEST_DEATH_RAGDOLLS.put(player.getUUID(), headId);
        PLAYER_DEATH_RAGDOLLS.computeIfAbsent(player.getUUID(), k -> ConcurrentHashMap.newKeySet()).add(headId);

        if (SPRAdditionSettings.transferInventoryToRagdoll()) {
            DEATH_INVENTORIES.put(headId, inventory);
            PENDING_INVENTORY_SUPPRESS.put(player.getUUID(), headId);
            SablePlayerRagdoll.LOGGER.info(
                    "[spr_addition] death ragdoll {} spawned for {} with {} inventory items",
                    RagdollRegistry.shortId(headId), player.getGameProfile().getName(),
                    inventory.stream().filter(s -> !s.isEmpty()).count());
        } else {
            SablePlayerRagdoll.LOGGER.info(
                    "[spr_addition] death ragdoll {} spawned for {} (inventory transfer disabled)",
                    RagdollRegistry.shortId(headId), player.getGameProfile().getName());
        }

        return headId;
    }

    private static ServerSubLevel spawnDeathRagdollSubLevel(
            ServerLevel level,
            ServerPlayer player,
            Vec3 position,
            double headingDegrees,
            PlayerlessDespawnRule despawnRule
    ) {
        SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(level);
        if (physicsSystem == null) return null;

        Vec3 heading = Vec3.directionFromRotation(0.0F, (float) headingDegrees);
        Vec3 forward = normalizeOr(new Vec3(heading.x, 0.0, heading.z), new Vec3(0.0, 0.0, 1.0));
        Vec3 right = horizontalRight(forward);
        Vec3 baseCenter = Vec3.atCenterOf(BlockPos.containing(position));

        RagdollAssemblyHelper.Doll doll = RagdollAssemblyHelper.spawn(level, player, baseCenter, right, forward);
        if (doll == null) return null;

        ServerSubLevel ragdollBody = doll.headSubLevel();
        BlockPos plotSeat = ragdollBody.getPlot().getCenterBlock();
        if (!RagdollRegistryAccessor.invokeEnsureValidMass(ragdollBody, List.of(plotSeat))) {
            SablePlayerRagdoll.LOGGER.warn("[spr_addition] death ragdoll {} has no valid mass — dropping",
                    RagdollRegistry.shortId(ragdollBody.getUniqueId()));
            RagdollRegistryAccessor.invokeDropFailed(physicsSystem, ragdollBody);
            return null;
        }

        RagdollRegistryAccessor.getRagdollBodyIds().add(ragdollBody.getUniqueId());
        RagdollSavedData.get(level).saveRagdoll(ragdollBody.getUniqueId(), doll.partSubLevelIds());
        RagdollDeferredSync.queuePlayerlessLaunch(ragdollBody, new Vector3d(), new Vector3d(), false, despawnRule);

        SablePlayerRagdoll.LOGGER.info(
                "[spr_addition] death ragdoll {} for {} at {} ({} parts, {} constraints)",
                RagdollRegistry.shortId(ragdollBody.getUniqueId()), player.getGameProfile().getName(),
                BlockPos.containing(position).toShortString(), doll.allSubLevels().size(), doll.constraints());

        return ragdollBody;
    }

    private static Vec3 normalizeOr(Vec3 vec, Vec3 fallback) {
        double length = vec.lengthSqr();
        return length < 1.0E-4 ? fallback : vec.normalize();
    }

    private static Vec3 horizontalRight(Vec3 forward) {
        return new Vec3(-forward.z, 0.0, forward.x);
    }

    public static boolean consumePendingSuppress(UUID playerId) {
        return PENDING_INVENTORY_SUPPRESS.remove(playerId) != null;
    }

    public static void dropInventoryAtRagdoll(ServerLevel level, ServerSubLevel headSubLevel) {
        UUID headId = headSubLevel.getUniqueId();
        if (!DEATH_RAGDOLL_HEADS.remove(headId)) {
            return;
        }

        for (Set<UUID> set : PLAYER_DEATH_RAGDOLLS.values()) {
            set.remove(headId);
        }

        NonNullList<ItemStack> inventory = DEATH_INVENTORIES.remove(headId);
        if (inventory == null || inventory.isEmpty()) {
            return;
        }

        Vec3 dropPos = resolveWorldDropPosition(level, headSubLevel);

        int dropped = 0;
        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) continue;
            ItemEntity entity = new ItemEntity(level, dropPos.x, dropPos.y, dropPos.z, stack.copy());
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
            dropped++;
        }

        if (dropped > 0) {
            SablePlayerRagdoll.LOGGER.info(
                    "[spr_addition] dropped {} item(s) from death ragdoll {} at world {}",
                    dropped, RagdollRegistry.shortId(headId),
                    String.format("(%.1f, %.1f, %.1f)", dropPos.x, dropPos.y, dropPos.z));
        }
    }

    public static UUID getLatestDeathRagdoll(UUID playerId) {
        return LATEST_DEATH_RAGDOLLS.get(playerId);
    }

    public static Set<UUID> getAllDeathRagdolls(UUID playerId) {
        Set<UUID> set = PLAYER_DEATH_RAGDOLLS.get(playerId);
        return set != null ? set : Set.of();
    }

    private static Vec3 resolveWorldDropPosition(ServerLevel level, ServerSubLevel headSubLevel) {
        UUID torsoId = RagdollAssemblyHelper.linkedTorso(headSubLevel.getUniqueId());
        ServerSubLevel source = headSubLevel;

        if (torsoId != null) {
            SubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container instanceof ServerSubLevelContainer serverContainer) {
                SubLevel torso = serverContainer.getSubLevel(torsoId);
                if (torso instanceof ServerSubLevel serverTorso && !serverTorso.isRemoved()) {
                    source = serverTorso;
                }
            }
        }

        return source.logicalPose()
                .transformPosition(Vec3.atCenterOf(source.getPlot().getCenterBlock()))
                .add(0.0, 0.5, 0.0);
    }

    public static boolean isDeathRagdoll(UUID headId) {
        return DEATH_RAGDOLL_HEADS.contains(headId);
    }

    public static void registerDeathRagdoll(UUID headId, NonNullList<ItemStack> inventory) {
        DEATH_RAGDOLL_HEADS.add(headId);
        if (!inventory.isEmpty()) {
            DEATH_INVENTORIES.put(headId, inventory);
            PENDING_INVENTORY_SUPPRESS.put(headId, headId);
        }
    }

    public static List<ItemStack> getInventorySnapshot(UUID headId) {
        NonNullList<ItemStack> inv = DEATH_INVENTORIES.get(headId);
        if (inv == null) return List.of();
        return inv.stream().filter(s -> !s.isEmpty()).map(ItemStack::copy).toList();
    }

    public static boolean addItem(UUID headId, ItemStack item) {
        NonNullList<ItemStack> inv = DEATH_INVENTORIES.get(headId);
        if (inv == null || item.isEmpty()) return false;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i).isEmpty()) {
                inv.set(i, item.copy());
                return true;
            }
        }
        return false;
    }

    public static void setInventory(UUID headId, List<ItemStack> items) {
        if (!DEATH_RAGDOLL_HEADS.contains(headId)) return;
        NonNullList<ItemStack> inv = NonNullList.withSize(54, ItemStack.EMPTY);
        int slot = 0;
        for (ItemStack item : items) {
            if (slot >= inv.size()) break;
            if (!item.isEmpty()) {
                inv.set(slot++, item.copy());
            }
        }
        DEATH_INVENTORIES.put(headId, inv);
    }

    public static void clearInventory(UUID headId) {
        DEATH_INVENTORIES.remove(headId);
    }

    public static NonNullList<ItemStack> captureFullInventory(ServerPlayer player) {
        NonNullList<ItemStack> result = NonNullList.withSize(54, ItemStack.EMPTY);
        Inventory inv = player.getInventory();
        int slot = 0;
        for (ItemStack stack : inv.items) {
            if (slot < 54 && !stack.isEmpty()) {
                result.set(slot, stack.copy());
            }
            slot++;
        }
        for (ItemStack stack : inv.armor) {
            if (slot < 54 && !stack.isEmpty()) {
                result.set(slot, stack.copy());
            }
            slot++;
        }
        for (ItemStack stack : inv.offhand) {
            if (slot < 54 && !stack.isEmpty()) {
                result.set(slot, stack.copy());
            }
            slot++;
        }
        return result;
    }

    public static void resetState() {
        DEATH_INVENTORIES.clear();
        DEATH_RAGDOLL_HEADS.clear();
        PENDING_INVENTORY_SUPPRESS.clear();
        LATEST_DEATH_RAGDOLLS.clear();
        PLAYER_DEATH_RAGDOLLS.clear();
    }
}

