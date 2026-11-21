package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.config.GodfallConfig;
import com.lazybones.godfalltrinkets.item.custom.StomachPouchItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** 进食时把营养存进大胃袋，并从玩家饥饿条上扣回等量 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public class StomachPouchEatEvent {

    @SubscribeEvent
    public static void onPlayerEatFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack foodStack = event.getItem();
        FoodProperties food = foodStack.getFoodProperties(player);
        if (food == null) {
            return;
        }
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            var found = handler.findFirstCurio(s -> s.getItem() instanceof StomachPouchItem);
            if (found.isEmpty()) {
                return;
            }
            ItemStack pouch = found.get().stack();

            int addHunger = food.nutrition();
            float addSat = food.saturation() * food.nutrition();

            int currHunger = StomachPouchItem.getStoredHunger(pouch);
            float currSat = StomachPouchItem.getStoredSaturation(pouch);
            int maxHunger = GodfallConfig.STOMACH_MAX_HUNGER.get();
            float maxSat = GodfallConfig.STOMACH_MAX_SATURATION.get().floatValue();

            if (currHunger < maxHunger || currSat < maxSat) {
                StomachPouchItem.setStoredHunger(pouch, Math.min(maxHunger, currHunger + addHunger));
                StomachPouchItem.setStoredSaturation(pouch, Math.min(maxSat, currSat + addSat));
                // 抵消这次食物本来会加到玩家身上的饥饿 / 饱和度
                player.getFoodData().eat(-addHunger, -addSat);
            }
        });
    }
}
