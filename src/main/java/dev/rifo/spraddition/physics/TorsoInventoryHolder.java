package dev.rifo.spraddition.physics;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public interface TorsoInventoryHolder {
    NonNullList<ItemStack> spraddition$getDeathInventory();
    void spraddition$setDeathInventory(NonNullList<ItemStack> inventory);
    boolean spraddition$hasDeathInventory();
    void spraddition$clearDeathInventory();
    String spraddition$getSkinName();
    net.minecraft.world.SimpleContainer spraddition$getMenuContainer(@org.jetbrains.annotations.Nullable java.util.UUID headId, dev.leo.sableplayerragdoll.block.entity.RagdollPartBlockEntity be);
}

