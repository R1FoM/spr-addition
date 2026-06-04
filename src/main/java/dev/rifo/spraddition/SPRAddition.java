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
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.UUID;

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
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        SPRAdditionDeathHelper.tickEmptyRagdolls(event.getServer());
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

        UUID headId = SPRAdditionDeathHelper.spawnDeathRagdoll(player, level);
        if (headId != null) {
            deathEvent.setSession(SPRAdditionAPI.getPlayerlessSessionById(level, headId));
        }
    }

    private static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (SPRAdditionDeathHelper.consumePendingSuppress(player.getUUID())) {
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
        if (be instanceof TorsoInventoryHolder torso && torso.spraddition$hasDeathInventory()) {
            openTorsoChest(player, headId, torso, (RagdollPartBlockEntity) be);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
            event.setCanceled(true);
        }
    }

    private static void openTorsoChest(
            ServerPlayer player,
            @Nullable UUID headId,
            TorsoInventoryHolder torso,
            RagdollPartBlockEntity be
    ) {
        NonNullList<ItemStack> deathInv = torso.spraddition$getDeathInventory();
        int slots = deathInv.size();

        SimpleContainer container = new SimpleContainer(slots);
        for (int i = 0; i < slots; i++) {
            container.setItem(i, deathInv.get(i).copy());
        }

        container.addListener(c -> {
            NonNullList<ItemStack> updated = NonNullList.withSize(slots, ItemStack.EMPTY);
            for (int i = 0; i < slots; i++) {
                updated.set(i, c.getItem(i).copy());
            }
            torso.spraddition$setDeathInventory(updated);
            be.setChanged();
            if (headId != null) {
                SPRAdditionDeathHelper.setInventory(headId, updated);
            }
        });

        String ownerName = torso.spraddition$getSkinName();
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x6, id, inv, container, 6),
                Component.literal(ownerName + "'s belongings")
        ));
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        SPRAdditionDeathHelper.resetState();
    }

    private static void onServerStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        SPRAdditionDeathHelper.load(event.getServer());
        LOGGER.info("SablePlayerRagdoll Addition Server Starting. Loaded saved data.");
    }
}
