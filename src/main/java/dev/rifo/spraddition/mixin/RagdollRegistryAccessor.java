package dev.rifo.spraddition.mixin;

import dev.leo.sableplayerragdoll.physics.RagdollRegistry;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mixin(value = RagdollRegistry.class, remap = false)
public interface RagdollRegistryAccessor {

    @Accessor("RAGDOLL_BODY_IDS")
    static Set<UUID> getRagdollBodyIds() {
        throw new AssertionError();
    }

    @Invoker("ensureValidMass")
    static boolean invokeEnsureValidMass(ServerSubLevel body, List<BlockPos> expectedBlocks) {
        throw new AssertionError();
    }

    @Invoker("dropFailed")
    static void invokeDropFailed(SubLevelPhysicsSystem physicsSystem, ServerSubLevel body) {
        throw new AssertionError();
    }
}

