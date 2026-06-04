package dev.rifo.spraddition.mixin;

import dev.leo.sableplayerragdoll.physics.RagdollExpireHelper;
import dev.rifo.spraddition.physics.SPRAdditionDeathHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RagdollExpireHelper.class, remap = false)
public class MixinRagdollExpireHelper {

    @Inject(
        method = "expire(Ldev/ryanhcode/sable/sublevel/system/SubLevelPhysicsSystem;Lnet/minecraft/server/level/ServerLevel;Ldev/ryanhcode/sable/sublevel/ServerSubLevel;Ljava/lang/String;)V",
        at = @At(value = "INVOKE", target = "Ldev/leo/sableplayerragdoll/physics/RagdollDeferredSync;queueRemoval(Ljava/util/UUID;Lnet/minecraft/server/level/ServerLevel;)V")
    )
    private static void spraddition$onExpire(SubLevelPhysicsSystem physicsSystem, ServerLevel level, ServerSubLevel subLevel, String reason, CallbackInfo ci) {
        SPRAdditionDeathHelper.dropInventoryAtRagdoll(level, subLevel);
    }

    @Inject(
        method = "expireImmediate(Ldev/ryanhcode/sable/sublevel/system/SubLevelPhysicsSystem;Lnet/minecraft/server/level/ServerLevel;Ldev/ryanhcode/sable/sublevel/ServerSubLevel;Ljava/lang/String;Z)V",
        at = @At(value = "INVOKE", target = "Ldev/leo/sableplayerragdoll/physics/RagdollRemovalHelper;removeRagdollSubLevel(Ldev/ryanhcode/sable/sublevel/system/SubLevelPhysicsSystem;Ldev/ryanhcode/sable/sublevel/ServerSubLevel;)V")
    )
    private static void spraddition$onExpireImmediate(SubLevelPhysicsSystem physicsSystem, ServerLevel level, ServerSubLevel subLevel, String reason, boolean placePlayerAtRagdoll, CallbackInfo ci) {
        SPRAdditionDeathHelper.dropInventoryAtRagdoll(level, subLevel);
    }
}

