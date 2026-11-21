package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

/** 完成「均衡饮食」成就送一件大胃袋（每个玩家只给一次） */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public class StomachPouchAdvancementReward {

    private static final ResourceLocation BALANCED_DIET =
            ResourceLocation.fromNamespaceAndPath("minecraft", "husbandry/balanced_diet");
    private static final String TAG_GOT_STOMACH_POUCH = "godfall_got_stomach_pouch";

    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        Player player = event.getEntity();
        if (!BALANCED_DIET.equals(event.getAdvancement().id())) {
            return;
        }
        CompoundTag persistent = player.getPersistentData();
        if (persistent.getBoolean(TAG_GOT_STOMACH_POUCH)) {
            return;
        }
        player.addItem(new ItemStack(ModItems.STOMACH_POUCH.get()));
        persistent.putBoolean(TAG_GOT_STOMACH_POUCH, true);
    }
}
