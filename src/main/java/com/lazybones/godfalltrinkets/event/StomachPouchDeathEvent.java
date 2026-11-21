package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.config.GodfallConfig;
import com.lazybones.godfalltrinkets.item.custom.StomachPouchItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** 死亡时按比例蒸发大胃袋里的储存 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public class StomachPouchDeathEvent {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        double drainRatio = GodfallConfig.STOMACH_DEATH_DRAIN_RATIO.get();
        if (drainRatio <= 0D) {
            return;
        }
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            var found = handler.findFirstCurio(s -> s.getItem() instanceof StomachPouchItem);
            if (found.isEmpty()) {
                return;
            }
            ItemStack pouch = found.get().stack();
            int currHunger = StomachPouchItem.getStoredHunger(pouch);
            float currSat = StomachPouchItem.getStoredSaturation(pouch);
            StomachPouchItem.setStoredHunger(pouch,
                    (int) Math.max(0, Math.floor(currHunger * (1D - drainRatio))));
            StomachPouchItem.setStoredSaturation(pouch,
                    (float) Math.max(0F, currSat * (1D - drainRatio)));
        });
    }
}
