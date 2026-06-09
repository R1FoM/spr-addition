package dev.rifo.spraddition.physics;

import dev.rifo.spraddition.config.SPRAdditionSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class SPRAdditionGrabModifier {
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("spr_addition",
            "grab_speed_penalty");
    private static final ResourceLocation SABLE_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath("sable_player_ragdoll", "grab_slowdown");

    public static void applyModifier(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(SABLE_MODIFIER_ID);

            double multiplier = SPRAdditionSettings.grabSpeedMultiplier();
            double value = multiplier - 1.0;

            AttributeModifier existing = attribute.getModifier(MODIFIER_ID);
            if (existing != null) {
                if (existing.amount() == value) {
                    return; // Already applied
                }
                attribute.removeModifier(MODIFIER_ID);
            }

            if (multiplier != 1.0) {
                attribute.addTransientModifier(
                        new AttributeModifier(MODIFIER_ID, value, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    public static void removeModifier(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(MODIFIER_ID);
            attribute.removeModifier(SABLE_MODIFIER_ID); // Just in case
        }
    }
}
