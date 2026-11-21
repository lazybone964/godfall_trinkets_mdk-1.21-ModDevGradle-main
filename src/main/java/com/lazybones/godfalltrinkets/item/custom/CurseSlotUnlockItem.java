package com.lazybones.godfalltrinkets.item.custom;

import com.lazybones.godfalltrinkets.event.TrinketSlotSyncEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 恶咒令牌 —— 右键解锁 curse_transfer 槽位（1 格），可佩戴咒厄转移坠。
 * <p>
 * 解锁状态存在玩家 persistentData 里，槽位格数由 baseSize 迁移写入存档
 * （不用永久槽位修饰符：修饰符在读档路径上会丢，槽位一归零饰品就被判非法退回背包）。
 */
public class CurseSlotUnlockItem extends Item {

    public static final String TAG_UNLOCKED_CURSE_SLOT = "godfall_unlock_curse_slot";

    public CurseSlotUnlockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (player.getPersistentData().getBoolean(TAG_UNLOCKED_CURSE_SLOT)) {
            player.sendSystemMessage(Component.translatable("msg.godfall.slot.already_unlock"));
            return InteractionResultHolder.fail(stack);
        }
        player.getPersistentData().putBoolean(TAG_UNLOCKED_CURSE_SLOT, true);
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                TrinketSlotSyncEvents.ensureSlots(player, handler));

        stack.shrink(1);
        player.sendSystemMessage(Component.translatable("msg.godfall.slot.unlock_success"));
        return InteractionResultHolder.consume(stack);
    }
}
