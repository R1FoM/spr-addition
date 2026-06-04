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
    }

    @Inject(method = "configure(Ldev/leo/sableplayerragdoll/block/entity/RagdollPartBlockEntity$BodyPart;Lnet/minecraft/world/entity/player/Player;)V", at = @At("TAIL"))
    private void spraddition$onConfigure(BodyPart bodyPart, Player player, CallbackInfo ci) {
        if (bodyPart == BodyPart.TORSO) {
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
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void spraddition$loadAdditional(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        RagdollPartBlockEntity be = (RagdollPartBlockEntity) (Object) this;
        if (be.bodyPart() == BodyPart.TORSO && tag.contains("DeathInventory", Tag.TAG_LIST)) {
            this.spraddition$deathInventory = NonNullList.withSize(54, ItemStack.EMPTY);
            ListTag invList = tag.getList("DeathInventory", Tag.TAG_COMPOUND);
            for (int i = 0; i < invList.size(); i++) {
                CompoundTag slotTag = invList.getCompound(i);
                int slot = slotTag.getByte("Slot") & 0xFF;
                if (slot < 54) {
                    ItemStack parsed = ItemStack.parse(registries, slotTag.getCompound("Item")).orElse(ItemStack.EMPTY);
                    this.spraddition$deathInventory.set(slot, parsed);
                }
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

