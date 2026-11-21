package com.lazybones.godfalltrinkets.item.custom;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.util.AdvancementHelper;
import com.lazybones.godfalltrinkets.util.ItemNbt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

/**
 * 功业徽记（badge 饰品）：按玩家已完成进度数量提供成长加成。
 *
 * <p>1.21 迁移要点：
 * <ol>
 *   <li>NBT 读写走 {@link ItemNbt}（CUSTOM_DATA 组件）；</li>
 *   <li>UUID 修饰符 → ResourceLocation 修饰符，{@code MULTIPLY_TOTAL} → {@code ADD_MULTIPLIED_TOTAL}；</li>
 *   <li>{@code Screen.hasShiftDown()} 只在客户端调用（用 {@link FMLEnvironment#dist} 兜底）；</li>
 *   <li>Curios 查询用 {@link CuriosApi#getCuriosInventory} 的 {@link java.util.Optional}。</li>
 * </ol>
 */
public class AdvancementTallyTrinketItem extends GodfallTrinketItem {

    public static final String TAG_TOTAL_COMPLETED_ADV = "tally_adv_count";

    /** 每完成一个进度的加成 */
    public static final double HP_PER_ADV = 0.003;      // +0.3% 最大生命值
    public static final double DAMAGE_PER_ADV = 0.0025; // +0.25% 伤害
    public static final double RESIST_PER_ADV = 0.002;  // +0.2% 减伤

    private static final ResourceLocation HP_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, "tally_max_health");

    public AdvancementTallyTrinketItem(Item.Properties properties) {
        super(properties, "badge",
                new String[]{
                        "tooltip.godfall_trinkets.desc.advancement_tally_trinket.line1",
                        "tooltip.godfall_trinkets.desc.advancement_tally_trinket.line2",
                        "tooltip.godfall_trinkets.desc.advancement_tally_trinket.line3"},
                null);
    }

    // ==================== 进度计数存取 ====================

    /** 服务端：把手上的徽记写入玩家当前已完成进度总数（只在佩戴时调用） */
    public static void updateTallyTag(ItemStack stack, Player player) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer sp)) {
            return;
        }
        ItemNbt.putInt(stack, TAG_TOTAL_COMPLETED_ADV, AdvancementHelper.countAllCompletedAdvancements(sp));
    }

    /** 读取徽记里存的进度数 */
    public static int getStoredAdvCount(ItemStack stack) {
        return ItemNbt.getInt(stack, TAG_TOTAL_COMPLETED_ADV);
    }

    /** 玩家佩戴中的徽记记录的进度数（没戴返回 0） */
    public static int getEquippedAdvCount(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(
                        stack -> stack.getItem() instanceof AdvancementTallyTrinketItem))
                .map(result -> getStoredAdvCount(result.stack()))
                .orElse(0);
    }

    /** 减伤比例（0.2% × 进度数，封顶 99%） */
    public static double getDamageResistance(int count) {
        return Math.min(count * RESIST_PER_ADV, 0.99);
    }

    // ==================== 属性加成 ====================

    /** 按进度数刷新最大生命值加成 */
    public static void applyTallyAttributes(Player player, int count) {
        if (player.level().isClientSide()) {
            return;
        }
        AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance != null) {
            instance.removeModifier(HP_MODIFIER_ID);
            double amount = count * HP_PER_ADV;
            if (amount > 0) {
                instance.addTransientModifier(new AttributeModifier(HP_MODIFIER_ID, amount,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ==================== Curios 钩子 ====================

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack currentStack) {
        if (slotContext.entity() instanceof Player player) {
            updateTallyTag(currentStack, player);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack currentStack) {
        if (slotContext.entity() instanceof Player player && !player.level().isClientSide()) {
            // 摘下保留 NBT，但立刻撤掉属性加成
            applyTallyAttributes(player, 0);
        }
    }

    // ==================== Tooltip ====================

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        int count = getStoredAdvCount(stack);
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.godfall_trinkets.hold_shift_view_stats")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        if (!net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            return;
        }
        tooltip.add(Component.literal("§6=== 当前生效加成 ==="));
        tooltip.add(Component.literal(String.format("§7已完成成就：%d 个", count)));
        tooltip.add(Component.literal(String.format("§7最大生命：+%.2f %%", count * HP_PER_ADV * 100D)));
        tooltip.add(Component.literal(String.format("§7伤害加成：+%.2f %%", count * DAMAGE_PER_ADV * 100D)));
        tooltip.add(Component.literal(String.format("§7伤害减免：-%.2f %%", getDamageResistance(count) * 100D)));
    }
}
