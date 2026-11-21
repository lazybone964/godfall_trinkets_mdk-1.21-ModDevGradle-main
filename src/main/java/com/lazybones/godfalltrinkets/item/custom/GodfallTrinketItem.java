package com.lazybones.godfalltrinkets.item.custom;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.util.RestoreGuard;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.function.Consumer;

/**
 * 饰品基类（1.21.1 / Curios 9.x 版本）。
 *
 * 与 1.20.1 的差异：
 * 1. getAttributeModifiers 的 Multimap 泛型从 Attribute 变为 Holder&lt;Attribute&gt;，并改用 ResourceLocation 版签名；
 * 2. getDropRule(LivingEntity, ItemStack) 旧签名已被 Curios 9 移除，只保留 SlotContext 版；
 * 3. appendHoverText 多了 Item.TooltipContext 参数（1.21 的 tooltip 体系）。
 */
public class GodfallTrinketItem extends Item implements ICurioItem {

    private final String slotId;
    private final String[] descLines;
    private final Consumer<Multimap<Holder<Attribute>, AttributeModifier>> attributeSupplier;

    public GodfallTrinketItem(Properties properties, String slotId, String[] descLines,
                              Consumer<Multimap<Holder<Attribute>, AttributeModifier>> attributeSupplier) {
        super(properties.stacksTo(1));
        this.slotId = slotId;
        this.descLines = descLines;
        this.attributeSupplier = attributeSupplier;
    }

    public String getSlotId() {
        return this.slotId;
    }

    /** 身上是否戴着破厄之核 / 神格 */
    public static boolean wearingCore(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(h -> h.findFirstCurio(s -> s.is(ModItems.BROKEN_CORE.get())
                        || s.is(ModItems.GOD_CORE.get())))
                .isPresent();
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        // 只允许放进与自身匹配的 Curios 槽位。
        // slotContext.identifier() 在 Curios 9 里可能是 "ring"，也可能是 "godfall_trinkets:ring"，
        // 统一取 path 再比较，避免因为命名空间前缀导致判定失败。
        String identifier = slotContext.identifier();
        String path = identifier.indexOf(':') >= 0
                ? identifier.substring(identifier.indexOf(':') + 1)
                : identifier;
        if (!this.slotId.equals(path)) {
            return false;
        }
        // 只校验槽位匹配。
        // 「必须戴破厄之核」不放在这里：Curios 9 读档时（capability 反序列化阶段，早于任何
        // PlayerEvent / OnDatapackSyncEvent）会逐件调用 canEquip，此时破厄之核还没进饰品栏，
        // 一做核校验就会把所有饰品判定非法并退回背包（正是「重进存档 / 换世界全掉光」那个老 bug）。
        // 这条规则改由 TrinketSlotSyncEvents 维护：无核 → 收回饰品 + 撤掉核提供的槽位。
        return true;
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create();
        if (this.attributeSupplier != null) {
            this.attributeSupplier.accept(map);
        }
        return map;
    }

    @Override
    public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source,
                                       boolean recentlyHit, ItemStack stack) {
        // 死亡永远保留饰品，不掉落
        return ICurio.DropRule.ALWAYS_KEEP;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.godfall_trinkets.require_core")
                .withStyle(ChatFormatting.RED));
        if (this.descLines == null) {
            return;
        }
        for (String lineKey : this.descLines) {
            tooltip.add(Component.translatable(lineKey).withStyle(ChatFormatting.GRAY));
        }
    }
}
