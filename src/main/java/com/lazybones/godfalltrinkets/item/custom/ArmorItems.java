package com.lazybones.godfalltrinkets.item.custom;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.item.armor.ArmorMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * 神装护甲。1.21 起护甲没法再重写 {@code getAttributeModifiers(slot, stack)}，
 * 属性改成 {@link ItemAttributeModifiers} 组件，所以征伐/深渊套的攻击力加成就写成
 * 「基类属性 + 追加一条 ATTACK_DAMAGE 修饰符」的形式。
 */
public class ArmorItems extends ArmorItem {

    private final ArmorMaterials set;

    public ArmorItems(ArmorMaterials set, ArmorItem.Type type) {
        super(set.holder(), type,
                new Item.Properties()
                        .durability(set.durability(type))
                        .rarity(Rarity.UNCOMMON));
        this.set = set;
    }

    public ArmorMaterials getSet() {
        return set;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        ItemAttributeModifiers base = super.getDefaultAttributeModifiers();
        float percent = set.attackPercent(getType());
        if (percent <= 0F) {
            return base;
        }
        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                GodfallTrinkets.MOD_ID, "set_attack_" + set.assetName() + "_" + getType().getName());
        return base.withModifierAdded(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(modifierId, percent, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                EquipmentSlotGroup.bySlot(getType().getSlot()));
    }

    // ==================== 套装判定 ====================

    public static boolean isFullGodSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.GOD_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.GOD_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.GOD_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.GOD_BOOTS.get());
    }

    public static boolean isFullConquestSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CONQUEST_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CONQUEST_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.CONQUEST_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.CONQUEST_BOOTS.get());
    }

    public static boolean isFullAbyssConquestSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ABYSS_GOD_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ABYSS_GOD_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.ABYSS_GOD_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.ABYSS_GOD_BOOTS.get());
    }

    public static int getConquestPieceCount(Player player) {
        int count = 0;
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CONQUEST_HELMET.get())) count++;
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CONQUEST_CHESTPLATE.get())) count++;
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.CONQUEST_LEGGINGS.get())) count++;
        if (player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.CONQUEST_BOOTS.get())) count++;
        return count;
    }

    public static int getAbyssConquestPieceCount(Player player) {
        int count = 0;
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ABYSS_GOD_HELMET.get())) count++;
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ABYSS_GOD_CHESTPLATE.get())) count++;
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.ABYSS_GOD_LEGGINGS.get())) count++;
        if (player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.ABYSS_GOD_BOOTS.get())) count++;
        return count;
    }

    // ==================== 套装效果 ====================

    private static final int SET_EFFECT_DURATION = 400;

    /** 神陨全套：生命恢复 + 抗性 */
    public static void applyGodSetEffects(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, SET_EFFECT_DURATION, 9, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, SET_EFFECT_DURATION, 1, false, true));
    }

    /** 征伐全套：抗性（吸血在受击事件里结算） */
    public static void applyConquestSetEffects(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, SET_EFFECT_DURATION, 1, false, true));
    }

    /** 深渊征伐全套：抗性 II + 力量 + 残血攻速 */
    public static void applyAbyssConquestSetEffects(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, SET_EFFECT_DURATION, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, SET_EFFECT_DURATION, 11, false, true));
        if (player.getHealth() / player.getMaxHealth() <= 0.3F) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, SET_EFFECT_DURATION, 1, false, true));
        }
    }

    // ==================== Tooltip ====================

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal(""));
        switch (set) {
            case GOD_INGOT -> {
                tooltip.add(Component.translatable("armor.godfall_trinkets.set_god")
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable("armor.godfall_trinkets.god_1")
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("armor.godfall_trinkets.god_2")
                        .withStyle(ChatFormatting.GRAY));
            }
            case CONQUEST_INGOT -> {
                tooltip.add(Component.translatable("armor.godfall_trinkets.set_conquest")
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable("armor.godfall_trinkets.conquest_e1")
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("armor.godfall_trinkets.conquest_e2")
                        .withStyle(ChatFormatting.GRAY));
            }
            case ABYSS_GOD_INGOT -> {
                tooltip.add(Component.translatable("armor.godfall_trinkets.set_abyss_god")
                        .withStyle(ChatFormatting.DARK_PURPLE));
                tooltip.add(Component.translatable(switch (getType()) {
                    case HELMET -> "armor.godfall_trinkets.abyss_helmet";
                    case CHESTPLATE -> "armor.godfall_trinkets.abyss_chest";
                    case LEGGINGS -> "armor.godfall_trinkets.abyss_legs";
                    default -> "armor.godfall_trinkets.abyss_boots";
                }).withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.translatable("armor.godfall_trinkets.abyss_full1")
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("armor.godfall_trinkets.abyss_full2")
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("armor.godfall_trinkets.abyss_full3")
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("armor.godfall_trinkets.abyss_full4")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
