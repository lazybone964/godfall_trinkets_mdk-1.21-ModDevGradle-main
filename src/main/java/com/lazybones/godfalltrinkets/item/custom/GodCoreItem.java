package com.lazybones.godfalltrinkets.item.custom;

import com.lazybones.godfalltrinkets.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 神格——破厄之核的终局进化形态，继承全部境界/诅咒/祝福，额外解锁 8 条神格权能。
 * 旧版的 NBT 标记 {@code is_god_core} 改为 {@link ModDataComponents#IS_GOD_CORE}。
 */
public class GodCoreItem extends BrokenCoreItem {

    public GodCoreItem(Properties properties) {
        super(properties);
    }

    public static void markAsGodCore(ItemStack stack) {
        stack.set(ModDataComponents.IS_GOD_CORE.get(), true);
    }

    public static boolean isGodCore(ItemStack stack) {
        return stack.getItem() instanceof GodCoreItem
                && stack.getOrDefault(ModDataComponents.IS_GOD_CORE.get(), false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (Screen.hasControlDown()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("tooltip.godfall_trinkets.godcore_header")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            for (int i = 1; i <= 8; i++) {
                tooltip.add(Component.translatable("tooltip.godfall_trinkets.godcore." + i)
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
    }
}
