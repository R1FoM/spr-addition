package dev.rifo.spraddition.mixin;

import dev.leo.sableplayerragdoll.block.entity.RagdollPartBlockEntity;
import dev.leo.sableplayerragdoll.block.entity.RagdollPartBlockEntity.BodyPart;
import dev.rifo.spraddition.physics.TorsoInventoryHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RagdollPartBlockEntity.class, remap = false)
public abstract class MixinRagdollPartBlockEntity implements TorsoInventoryHolder {

    @Shadow
    private String skinName;

    @Unique
    private NonNullList<ItemStack> spraddition$deathInventory = NonNullList.withSize(54, ItemStack.EMPTY);

    @Unique
    private net.minecraft.world.SimpleContainer spraddition$menuContainer;

    @Unique
    private java.util.UUID spraddition$ownerPlayerId;

    @Override
    public String spraddition$getSkinName() {
        return this.skinName;
    }

    @Override
    public NonNullList<ItemStack> spraddition$getDeathInventory() {
        return this.spraddition$deathInventory;
    }

    @Override
    public void spraddition$setDeathInventory(NonNullList<ItemStack> inventory) {
        this.spraddition$deathInventory = inventory;
        this.spraddition$menuContainer = null;
    }

    @Override
    public boolean spraddition$hasDeathInventory() {
        for (ItemStack stack : this.spraddition$deathInventory) {
            if (!stack.isEmpty()) return true;
        }
        return false;
    }

    @Override
    public void spraddition$clearDeathInventory() {
        this.spraddition$deathInventory = NonNullList.withSize(54, ItemStack.EMPTY);
        this.spraddition$menuContainer = null;
    }

    @Override
    public net.minecraft.world.SimpleContainer spraddition$getMenuContainer(@org.jetbrains.annotations.Nullable java.util.UUID headId, dev.leo.sableplayerragdoll.block.entity.RagdollPartBlockEntity be) {
        if (this.spraddition$menuContainer == null) {
            this.spraddition$menuContainer = new net.minecraft.world.SimpleContainer(54) {
                @Override
                public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                    return !be.isRemoved();
                }
                @Override
                public boolean canPlaceItem(int index, ItemStack stack) {
                    boolean isDeath = headId != null && dev.rifo.spraddition.physics.SPRAdditionDeathHelper.isDeathRagdoll(headId);
                    return !isDeath && dev.rifo.spraddition.config.SPRAdditionSettings.liveRagdollAllowPlacing() && index < 41;
                }
            };
            
            java.util.List<ItemStack> snapshot = headId != null ? dev.rifo.spraddition.physics.SPRAdditionDeathHelper.getInventorySnapshot(headId) : java.util.List.of();
            for (int i = 0; i < 54; i++) {
                if (i < snapshot.size() && !snapshot.get(i).isEmpty()) {
                    this.spraddition$menuContainer.setItem(i, snapshot.get(i).copy());
                } else if (i < this.spraddition$deathInventory.size() && !this.spraddition$deathInventory.get(i).isEmpty()) {
                    this.spraddition$menuContainer.setItem(i, this.spraddition$deathInventory.get(i).copy());
                }
            }

            this.spraddition$menuContainer.addListener(c -> {
                NonNullList<ItemStack> updated = NonNullList.withSize(54, ItemStack.EMPTY);
                for (int i = 0; i < 54; i++) {
                    updated.set(i, c.getItem(i).copy());
                }
                this.spraddition$deathInventory = updated;
                be.setChanged();
                if (headId != null) {
                    dev.rifo.spraddition.physics.SPRAdditionDeathHelper.setInventory(headId, updated);
                }
                
                if (this.spraddition$ownerPlayerId != null && be.getLevel() != null && !be.getLevel().isClientSide) {
                    boolean isDeath = headId != null && dev.rifo.spraddition.physics.SPRAdditionDeathHelper.isDeathRagdoll(headId);
                    if (!isDeath) {
                            Player ownerPlayer = be.getLevel().getServer().getPlayerList().getPlayer(this.spraddition$ownerPlayerId);
                            if (ownerPlayer != null && ownerPlayer instanceof net.minecraft.server.level.ServerPlayer sp) {
                                Inventory inv = sp.getInventory();
                                int slot = 0;
                                boolean changed = false;
                                for (int i = 0; i < inv.items.size(); i++) {
                                    if (slot < 54) {
                                        if (!ItemStack.matches(inv.items.get(i), updated.get(slot))) {
                                            inv.items.set(i, updated.get(slot));
                                            changed = true;
                                        }
                                        slot++;
                                    }
                                }
                                for (int i = 0; i < inv.armor.size(); i++) {
                                    if (slot < 54) {
                                        if (!ItemStack.matches(inv.armor.get(i), updated.get(slot))) {
                                            inv.armor.set(i, updated.get(slot));
                                            changed = true;
                                        }
                                        slot++;
                                    }
                                }
                                for (int i = 0; i < inv.offhand.size(); i++) {
                                    if (slot < 54) {
                                        if (!ItemStack.matches(inv.offhand.get(i), updated.get(slot))) {
                                            inv.offhand.set(i, updated.get(slot));
                                            changed = true;
                                        }
                                        slot++;
                                    }
                                }
                                if (changed) {
                                    inv.setChanged();
                                    sp.inventoryMenu.broadcastChanges();
                                    dev.rifo.spraddition.SPRAddition.LOGGER.info("[spr_addition] Successfully synced and broadcasted inventory for live ragdoll: {}", sp.getName().getString());
                                }
                            }
                    }
                }
            });
        }
        return this.spraddition$menuContainer;
    }

    @Inject(method = "configure(Ldev/leo/sableplayerragdoll/block/entity/RagdollPartBlockEntity$BodyPart;Lnet/minecraft/world/entity/player/Player;)V", at = @At("TAIL"))
    private void spraddition$onConfigure(BodyPart bodyPart, Player player, CallbackInfo ci) {
        if (bodyPart == BodyPart.TORSO) {
            this.spraddition$ownerPlayerId = player.getUUID();
            Inventory inv = player.getInventory();
            this.spraddition$deathInventory = NonNullList.withSize(54, ItemStack.EMPTY);
            int slot = 0;
            for (ItemStack stack : inv.items) {
                if (slot < 54 && !stack.isEmpty()) this.spraddition$deathInventory.set(slot, stack.copy());
                slot++;
            }
            for (ItemStack stack : inv.armor) {
                if (slot < 54 && !stack.isEmpty()) this.spraddition$deathInventory.set(slot, stack.copy());
                slot++;
            }
            for (ItemStack stack : inv.offhand) {
                if (slot < 54 && !stack.isEmpty()) this.spraddition$deathInventory.set(slot, stack.copy());
                slot++;
            }
            ((RagdollPartBlockEntity) (Object) this).setChanged();
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void spraddition$saveAdditional(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        RagdollPartBlockEntity be = (RagdollPartBlockEntity) (Object) this;
        if (be.bodyPart() == BodyPart.TORSO) {
            ListTag invList = new ListTag();
            for (int i = 0; i < this.spraddition$deathInventory.size(); i++) {
                ItemStack stack = this.spraddition$deathInventory.get(i);
                if (!stack.isEmpty()) {
                    CompoundTag slotTag = new CompoundTag();
                    slotTag.putByte("Slot", (byte) i);
                    slotTag.put("Item", stack.save(registries));
                    invList.add(slotTag);
                }
            }
            if (!invList.isEmpty()) {
                tag.put("DeathInventory", invList);
            }
            if (this.spraddition$ownerPlayerId != null) {
                tag.putUUID("OwnerPlayerId", this.spraddition$ownerPlayerId);
            }
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void spraddition$loadAdditional(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        RagdollPartBlockEntity be = (RagdollPartBlockEntity) (Object) this;
        if (be.bodyPart() == BodyPart.TORSO && tag.contains("DeathInventory", Tag.TAG_LIST)) {
            this.spraddition$deathInventory = NonNullList.withSize(54, ItemStack.EMPTY);
            this.spraddition$menuContainer = null;
            ListTag invList = tag.getList("DeathInventory", Tag.TAG_COMPOUND);
            for (int i = 0; i < invList.size(); i++) {
                CompoundTag slotTag = invList.getCompound(i);
                int slot = slotTag.getByte("Slot") & 0xFF;
                if (slot < 54) {
                    ItemStack parsed = ItemStack.parse(registries, slotTag.getCompound("Item")).orElse(ItemStack.EMPTY);
                    this.spraddition$deathInventory.set(slot, parsed);
                }
            }
            if (tag.hasUUID("OwnerPlayerId")) {
                this.spraddition$ownerPlayerId = tag.getUUID("OwnerPlayerId");
            }
        }
    }

    @Shadow
    public abstract org.joml.Vector3d grabCenter();

    @Shadow
    public abstract void stopTorsoGrab(java.util.UUID playerId);

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private java.util.Map<java.util.UUID, ?> grabbers;

    @Inject(method = "sable$physicsTick", at = @At("HEAD"))
    private void spraddition$onPhysicsTickOuter(dev.ryanhcode.sable.sublevel.ServerSubLevel subLevel, dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle handle, double timeStep, CallbackInfo ci) {
        double maxDist = dev.rifo.spraddition.config.SPRAdditionSettings.grabMaxDistance();
        if (maxDist > 0 && !this.grabbers.isEmpty()) {
            double maxDistSq = maxDist * maxDist;
            org.joml.Vector3d grabCenter = this.grabCenter();
            java.util.List<java.util.UUID> toRemove = new java.util.ArrayList<>();
            net.minecraft.world.level.Level level = ((RagdollPartBlockEntity)(Object)this).getLevel();
            if (level != null) {
                for (java.util.UUID playerId : this.grabbers.keySet()) {
                    Player player = level.getPlayerByUUID(playerId);
                    if (player != null) {
                        double distSq = dev.ryanhcode.sable.Sable.HELPER.distanceSquaredWithSubLevels(
                                level, 
                                dev.ryanhcode.sable.companion.math.JOMLConversion.toJOML(player.getEyePosition()), 
                                grabCenter
                        );
                        if (distSq > maxDistSq) {
                            toRemove.add(playerId);
                        }
                    }
                }
                for (java.util.UUID id : toRemove) {
                    this.stopTorsoGrab(id);
                }
            }
        }
    }
}

