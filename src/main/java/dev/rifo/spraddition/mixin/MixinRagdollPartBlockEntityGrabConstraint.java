package dev.rifo.spraddition.mixin;

import dev.rifo.spraddition.config.SPRAdditionSettings;
import dev.rifo.spraddition.physics.SPRAdditionGrabModifier;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(targets = "dev.leo.sableplayerragdoll.block.entity.RagdollPartBlockEntity$GrabConstraint", remap = false)
public class MixinRagdollPartBlockEntityGrabConstraint {

    @Shadow
    @Final
    private UUID playerId;

    @Shadow
    @Nullable
    private PhysicsConstraintHandle constraintHandle;

    @Inject(method = "physicsTick", at = @At("TAIL"))
    private void spraddition$overrideGrabPhysics(ServerSubLevel subLevel, CallbackInfo ci) {
        if (this.constraintHandle != null) {
            double stiffness = SPRAdditionSettings.grabStiffness();
            double damping = SPRAdditionSettings.grabDamping();
            double maxForce = SPRAdditionSettings.grabMaxForce();

            for (ConstraintJointAxis axis : ConstraintJointAxis.LINEAR) {
                this.constraintHandle.setMotor(axis, 0.0, stiffness, damping, true, maxForce);
            }
            
            Player player = subLevel.getLevel().getPlayerByUUID(this.playerId);
            if (player != null) {
                SPRAdditionGrabModifier.applyModifier(player);
            }
        }
    }

    @Inject(method = "removeJoint", at = @At("HEAD"))
    private void spraddition$onRemoveJoint(CallbackInfo ci) {
        if (this.constraintHandle != null) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(this.playerId);
                if (player != null) {
                    SPRAdditionGrabModifier.removeModifier(player);
                }
            }
        }
    }
}

