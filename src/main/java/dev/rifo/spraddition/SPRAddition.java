package dev.rifo.spraddition;

import dev.leo.sableplayerragdoll.api.RagdollAPI;
import dev.leo.sableplayerragdoll.block.RagdollPartBlock;
import dev.leo.sableplayerragdoll.block.entity.RagdollPartBlockEntity;
import dev.leo.sableplayerragdoll.physics.RagdollAssemblyHelper;
import dev.leo.sableplayerragdoll.physics.RagdollExpireHelper;
import dev.leo.sableplayerragdoll.physics.RagdollSessionManager;
import dev.rifo.spraddition.api.RagdollDeathEvent;
import dev.rifo.spraddition.api.SPRAdditionAPI;
import dev.rifo.spraddition.config.SPRAdditionConfig;
import dev.rifo.spraddition.physics.SPRAdditionDeathHelper;
import dev.rifo.spraddition.physics.RagdollFallTracker;
import dev.rifo.spraddition.physics.TorsoInventoryHolder;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.UUID;
import java.util.List;

import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod("spr_addition")
public final class SPRAddition {

    public static final Logger LOGGER = LogUtils.getLogger();

    public SPRAddition(IEventBus modBus, ModContainer modContainer) {
        modBus.addListener(SPRAdditionConfig::onLoad);
        modBus.addListener(SPRAdditionConfig::onReload);
        SPRAdditionConfig.register(modContainer);

        NeoForge.EVENT_BUS.addListener(SPRAddition::onPlayerDeath);
        NeoForge.EVENT_BUS.addListener(SPRAddition::onPlayerDrops);
        NeoForge.EVENT_BUS.addListener(SPRAddition::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(SPRAddition::onServerStopped);
        NeoForge.EVENT_BUS.addListener(SPRAddition::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(SPRAddition::onServerTick);
        NeoForge.EVENT_BUS.addListener(SPRAddition::onServerStarting);
        NeoForge.EVENT_BUS.addListener(SPRAddition::onLivingFall);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        SPRAdditionDeathHelper.tickEmptyRagdolls(event.getServer());
        RagdollFallTracker.tick(event.getServer());
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        dev.rifo.spraddition.command.SPRAdditionCommands.register(event.getDispatcher());
    }

    private static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();

        ServerSubLevel ragdoll = RagdollSessionManager.activeRagdollForPlayer(level, player.getUUID());
        if (ragdoll != null) {
            SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(level);
            if (physicsSystem != null) {
                RagdollExpireHelper.expireImmediate(physicsSystem, level, ragdoll, "player died");
            }
            return;
        }

        RagdollDeathEvent deathEvent = new RagdollDeathEvent(player);
        if (NeoForge.EVENT_BUS.post(deathEvent).isCanceled()) return;

        // Normalize player state immediately before spawning the death ragdoll.
        // RagdollAssemblyHelper.spawn() reads xRot, yHeadRot, yBodyRot, and Pose
        // to position the head block entity.  After a live ragdoll session these
        // values can be at physics-driven extremes (e.g. -90° face-down, SWIMMING
        // pose), causing the death ragdoll head to appear stretched / corrupted.
        // Normalising here is the last possible moment — it fires synchronously
        // right before the spawn call, so client movement packets cannot
        // overwrite the values in between.
        RagdollFallTracker.normalizePlayerState(player);

        UUID headId = SPRAdditionDeathHelper.spawnDeathRagdoll(player, level);
        if (headId != null) {
            deathEvent.setSession(SPRAdditionAPI.getPlayerlessSessionById(level, headId));
        }
    }

    @net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    private static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (SPRAdditionDeathHelper.consumePendingSuppress(player.getUUID())) {
            UUID headId = SPRAdditionDeathHelper.getLatestDeathRagdoll(player.getUUID());
            if (headId != null) {
                for (net.minecraft.world.entity.item.ItemEntity drop : event.getDrops()) {
                    SPRAdditionDeathHelper.addItem(headId, drop.getItem().copy());
                }
            }
            event.getDrops().clear();
            event.setCanceled(true);
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        if (!player.isShiftKeyDown()) return;

        BlockState state = level.getBlockState(event.getPos());
        if (!(state.getBlock() instanceof RagdollPartBlock)) return;
        if (state.getValue(RagdollPartBlock.BODY_PART) != RagdollPartBlockEntity.BodyPart.TORSO) return;

        SubLevel subLevel = Sable.HELPER.getContaining(level, event.getPos());
        UUID headId = subLevel != null ? RagdollAssemblyHelper.linkedHead(subLevel.getUniqueId()) : null;

        BlockEntity be = level.getBlockEntity(event.getPos());
        if (be instanceof TorsoInventoryHolder torso) {
            boolean hasInv = torso.spraddition$hasDeathInventory() || (headId != null && !SPRAdditionDeathHelper.getInventorySnapshot(headId).isEmpty());
            if (hasInv) {
                openTorsoChest(player, headId, torso, (RagdollPartBlockEntity) be);
                event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
                event.setCanceled(true);
            }
        }
    }

    private static void openTorsoChest(
            ServerPlayer player,
            @Nullable UUID headId,
            TorsoInventoryHolder torso,
            RagdollPartBlockEntity be
    ) {
        SimpleContainer container = torso.spraddition$getMenuContainer(headId, be);

        String ownerName = torso.spraddition$getSkinName();
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x6, id, inv, container, 6) {
                    {
                        for (int i = 0; i < container.getContainerSize(); i++) {
                            net.minecraft.world.inventory.Slot oldSlot = this.slots.get(i);
                            net.minecraft.world.inventory.Slot newSlot = new net.minecraft.world.inventory.Slot(oldSlot.container, oldSlot.getContainerSlot(), oldSlot.x, oldSlot.y) {
                                @Override
                                public boolean mayPlace(ItemStack stack) {
                                    return false; // Make slot read-only to prevent placing items
                                }
                            };
                            newSlot.index = oldSlot.index;
                            this.slots.set(i, newSlot);
                        }
                    }

                    @Override
                    public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
                        if (index >= container.getContainerSize()) {
                            return ItemStack.EMPTY; // Prevent shift-clicking from player inventory into ragdoll
                        }
                        
                        net.minecraft.world.inventory.Slot slot = this.slots.get(index);
                        if (slot != null && slot.hasItem() && index < container.getContainerSize()) {
                            ItemStack stack = slot.getItem();
                            try {
                                Class.forName("top.theillusivec4.curios.api.CuriosApi");
                                if (dev.rifo.spraddition.physics.CuriosEquipHelper.tryEquipCurio(player, stack)) {
                                    slot.set(ItemStack.EMPTY);
                                    return ItemStack.EMPTY;
                                }
                            } catch (ClassNotFoundException | NoClassDefFoundError ignored) {}
                        }
                        return super.quickMoveStack(player, index);
                    }
                },
                Component.literal(ownerName + "'s belongings")
        ));
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        SPRAdditionDeathHelper.resetState();
        RagdollFallTracker.resetAll();
    }

    private static void onServerStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        SPRAdditionDeathHelper.load(event.getServer());
        LOGGER.info("SablePlayerRagdoll Addition Server Starting. Loaded saved data.");
    }

    /**
     * [EXPERIMENTAL] Suppress vanilla fall damage while the player is in ragdoll
     * (or just exited one). Impact damage is handled by RagdollFallTracker.
     */
    private static void onLivingFall(LivingFallEvent event) {
        if (!dev.rifo.spraddition.config.SPRAdditionSettings.fallRagdollEnabled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (RagdollFallTracker.shouldSuppressFallDamage(player.getUUID())) {
            event.setCanceled(true);
        }
    }
}
