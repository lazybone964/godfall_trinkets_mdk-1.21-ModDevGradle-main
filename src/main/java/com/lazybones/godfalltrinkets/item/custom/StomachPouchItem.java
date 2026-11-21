package com.lazybones.godfalltrinkets.item.custom;

import com.google.common.collect.Multimap;
import com.lazybones.godfalltrinkets.config.GodfallConfig;
import com.lazybones.godfalltrinkets.util.ItemNbt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.loading.FMLEnvironment;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;
import java.util.function.Consumer;

/**
 * 大胃袋（body 槽）。
 * <p>
 * 进食时把食物的营养存进袋子里（不落到玩家自己的饥饿条上），
 * 储存量按比例转化为伤害 / 生命上限 / 护甲 / 韧性 / 击退抗性 / 幸运 / 攻速加成。
 * 死亡按配置比例蒸发一部分储存。
 */
public class StomachPouchItem extends GodfallTrinketItem {

    private static final String TAG_HUNGER = "stored_hunger";
    private static final String TAG_SATURATION = "stored_saturation";

    public StomachPouchItem(Properties properties, String slotId, String[] descLines,
                            Consumer<Multimap<Holder<Attribute>, AttributeModifier>> attributeSupplier) {
        super(properties, slotId, descLines, attributeSupplier);
    }

    public static int getStoredHunger(ItemStack stack) {
        return ItemNbt.getInt(stack, TAG_HUNGER);
    }

    public static void setStoredHunger(ItemStack stack, int val) {
        ItemNbt.putInt(stack, TAG_HUNGER, val);
    }

    public static float getStoredSaturation(ItemStack stack) {
        return ItemNbt.getFloat(stack, TAG_SATURATION);
    }

    public static void setStoredSaturation(ItemStack stack, float val) {
        ItemNbt.putFloat(stack, TAG_SATURATION, val);
    }

    /** 饥饿储存系数 0~1 */
    public static float getHungerCoeff(ItemStack stack) {
        int max = GodfallConfig.STOMACH_MAX_HUNGER.get();
        if (max <= 0) {
            return 0F;
        }
        return Math.min(1F, (float) getStoredHunger(stack) / max);
    }

    /** 饱和度储存系数 0~1 */
    public static float getSatCoeff(ItemStack stack) {
        float max = GodfallConfig.STOMACH_MAX_SATURATION.get().floatValue();
        if (max <= 0) {
            return 0F;
        }
        return Math.min(1F, getStoredSaturation(stack) / max);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.empty());
        int hunger = getStoredHunger(stack);
        float saturation = getStoredSaturation(stack);
        tooltip.add(Component.translatable("tooltip.godfall_trinkets.stomach_pouch.store",
                        hunger, String.format("%.1f", saturation))
                .withStyle(ChatFormatting.GRAY));

        if (!isShiftDown()) {
            return;
        }
        float ch = getHungerCoeff(stack);
        float cs = getSatCoeff(stack);
        float dmg = (float) (ch * GodfallConfig.STOMACH_FULL_DMG_BONUS.get() * 100F);
        float hp = (float) (ch * GodfallConfig.STOMACH_FULL_MAXHP_BONUS.get() * 100F);
        float armor = (float) (cs * GodfallConfig.STOMACH_FULL_ARMOR_BONUS.get() * 100F);
        float tough = (float) (cs * GodfallConfig.STOMACH_FULL_TOUGH_BONUS.get() * 100F);
        float kr = (float) (cs * GodfallConfig.STOMACH_FULL_KNOCKBACK_RESIST.get() * 100F);
        float luck = (float) (cs * GodfallConfig.STOMACH_FULL_LUCK_BONUS.get() * 100F);
        float atkSpd = (float) (cs * GodfallConfig.STOMACH_FULL_ATKSPEED_BONUS.get() * 100F);

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.godfall_trinkets.stomach_pouch.bonus_dmg",
                String.format("%.1f", dmg)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.godfall_trinkets.stomach_pouch.bonus_hp",
                String.format("%.1f", hp)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.godfall_trinkets.stomach_pouch.bonus_armor",
                String.format("%.1f", armor)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.godfall_trinkets.stomach_pouch.bonus_tough",
                String.format("%.1f", tough)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.godfall_trinkets.stomach_pouch.bonus_kr",
                String.format("%.1f", kr)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.godfall_trinkets.stomach_pouch.bonus_luck",
                String.format("%.1f", luck)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.godfall_trinkets.stomach_pouch.bonus_atk_spd",
                String.format("%.1f", atkSpd)).withStyle(ChatFormatting.YELLOW));
    }

    /** 客户端才按 Shift 展开详细加成；服务端 tooltip 生成时不能碰 Screen 类 */
    private static boolean isShiftDown() {
        return FMLEnvironment.dist.isClient() && net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        String identifier = slotContext.identifier();
        String path = identifier.indexOf(':') >= 0
                ? identifier.substring(identifier.indexOf(':') + 1)
                : identifier;
        return path.endsWith(getSlotId());
    }

    @Override
    public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source,
                                       boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }
}
