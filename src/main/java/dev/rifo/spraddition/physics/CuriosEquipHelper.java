package dev.rifo.spraddition.physics;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Map;

public class CuriosEquipHelper {
    public static boolean tryEquipCurio(Player player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        var tags = CuriosApi.getCuriosHelper().getCurioTags(stack.getItem());
        if (tags.isEmpty()) {
            return false;
        }
        
        var handler = player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
        if (handler != null) {
            Map<String, ICurioStacksHandler> curios = handler.getCurios();
            for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                String slotType = entry.getKey();
                ICurioStacksHandler stackHandler = entry.getValue();
                
                // Only try to equip if the item is valid for this slot type
                boolean isValid = false;
                for (String tag : tags) {
                    if (tag.equals(slotType)) {
                        isValid = true;
                        break;
                    }
                }
                if (!isValid) continue;
                
                var targetStacks = stackHandler.getStacks();
                for (int i = 0; i < targetStacks.getSlots(); i++) {
                    if (targetStacks.getStackInSlot(i).isEmpty()) {
                        targetStacks.setStackInSlot(i, stack.copy());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
