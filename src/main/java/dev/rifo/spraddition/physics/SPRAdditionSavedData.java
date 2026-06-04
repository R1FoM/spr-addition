package dev.rifo.spraddition.physics;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SPRAdditionSavedData extends SavedData {
    private static final String FILE_ID = "spr_addition_data";
    private static final SavedData.Factory<SPRAdditionSavedData> FACTORY = new SavedData.Factory<>(SPRAdditionSavedData::new, SPRAdditionSavedData::load);

    // Tracked data
    public final Set<UUID> deathRagdollHeads = ConcurrentHashMap.newKeySet();
    public final ConcurrentHashMap<UUID, Set<UUID>> playerDeathRagdolls = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<UUID, UUID> latestDeathRagdolls = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<UUID, NonNullList<ItemStack>> deathInventories = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<UUID, Integer> emptyRagdollTicks = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<UUID, Integer> absoluteRagdollTicks = new ConcurrentHashMap<>();

    public static SPRAdditionSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public static SPRAdditionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SPRAdditionSavedData data = new SPRAdditionSavedData();

        ListTag list = tag.getList("DeathRagdolls", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag ragdollTag = list.getCompound(i);
            UUID headId = ragdollTag.getUUID("Head");
            data.deathRagdollHeads.add(headId);

            if (ragdollTag.hasUUID("Player")) {
                UUID playerId = ragdollTag.getUUID("Player");
                data.playerDeathRagdolls.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(headId);
                
                if (ragdollTag.getBoolean("IsLatest")) {
                    data.latestDeathRagdolls.put(playerId, headId);
                }
            }

            if (ragdollTag.contains("Inventory", Tag.TAG_LIST)) {
                NonNullList<ItemStack> inv = NonNullList.withSize(54, ItemStack.EMPTY);
                ListTag invList = ragdollTag.getList("Inventory", Tag.TAG_COMPOUND);
                for (int j = 0; j < invList.size(); j++) {
                    CompoundTag slotTag = invList.getCompound(j);
                    int slot = slotTag.getByte("Slot") & 0xFF;
                    if (slot < 54) {
                        ItemStack parsed = ItemStack.parse(registries, slotTag.getCompound("Item")).orElse(ItemStack.EMPTY);
                        inv.set(slot, parsed);
                    }
                }
                data.deathInventories.put(headId, inv);
            }

            if (ragdollTag.contains("EmptyTicks")) {
                data.emptyRagdollTicks.put(headId, ragdollTag.getInt("EmptyTicks"));
            }

            if (ragdollTag.contains("AbsoluteTicks")) {
                data.absoluteRagdollTicks.put(headId, ragdollTag.getInt("AbsoluteTicks"));
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (UUID headId : deathRagdollHeads) {
            CompoundTag ragdollTag = new CompoundTag();
            ragdollTag.putUUID("Head", headId);

            // Find player
            UUID ownerId = null;
            for (Map.Entry<UUID, Set<UUID>> entry : playerDeathRagdolls.entrySet()) {
                if (entry.getValue().contains(headId)) {
                    ownerId = entry.getKey();
                    break;
                }
            }
            if (ownerId != null) {
                ragdollTag.putUUID("Player", ownerId);
                if (headId.equals(latestDeathRagdolls.get(ownerId))) {
                    ragdollTag.putBoolean("IsLatest", true);
                }
            }

            NonNullList<ItemStack> inv = deathInventories.get(headId);
            if (inv != null) {
                ListTag invList = new ListTag();
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack stack = inv.get(i);
                    if (!stack.isEmpty()) {
                        CompoundTag slotTag = new CompoundTag();
                        slotTag.putByte("Slot", (byte) i);
                        slotTag.put("Item", stack.save(registries));
                        invList.add(slotTag);
                    }
                }
                if (!invList.isEmpty()) {
                    ragdollTag.put("Inventory", invList);
                }
            }

            if (emptyRagdollTicks.containsKey(headId)) {
                ragdollTag.putInt("EmptyTicks", emptyRagdollTicks.get(headId));
            }

            if (absoluteRagdollTicks.containsKey(headId)) {
                ragdollTag.putInt("AbsoluteTicks", absoluteRagdollTicks.get(headId));
            }

            list.add(ragdollTag);
        }
        tag.put("DeathRagdolls", list);
        return tag;
    }
}
