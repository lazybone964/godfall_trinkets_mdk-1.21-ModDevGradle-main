package com.lazybones.godfalltrinkets.item.custom;

import com.lazybones.godfalltrinkets.event.TrinketSlotSyncEvents;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.lazybones.godfalltrinkets.ModDataComponents;
import com.lazybones.godfalltrinkets.config.GodfallConfig;
import com.lazybones.godfalltrinkets.curse.CurseTransferState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 破厄之核（1.21.1 版）。
 *
 * 与 1.20.1 的差异：
 * 1. {@code stack.getOrCreateTag()} 全部换成 DataComponent（{@link ModDataComponents}）；
 * 2. 属性修饰符 ID 由 UUID 改为 ResourceLocation，{@code MULTIPLY_BASE} → {@code ADD_MULTIPLIED_BASE}、
 *    {@code ADDITION} → {@code ADD_VALUE}；
 * 3. 临时槽位修饰符 {@code addTransientSlotModifier(String, ResourceLocation, double, Operation)}
 *    少了旧的 name 参数；
 * 4. {@code getDropRule(LivingEntity, ItemStack)} 已被 Curios 9 删除，只保留 SlotContext 版；
 * 5. {@code appendHoverText} 用 {@link Item.TooltipContext}。
 */
public class BrokenCoreItem extends Item implements ICurioItem {

    private static final String MOD_ID = "godfall_trinkets";
    private static final ResourceLocation DAMAGE_ID = id("broken_core_attack");
    private static final ResourceLocation HEALTH_ID = id("broken_core_health");
    private static final ResourceLocation ARMOR_ID = id("broken_core_armor");
    private static final ResourceLocation SPEED_ID = id("broken_core_speed");
    private static final ResourceLocation ATK_SPEED_ID = id("broken_core_attack_speed");
    private static final ResourceLocation TOUGH_ID = id("broken_core_toughness");
    private static final ResourceLocation KB_RESIST_ID = id("broken_core_knockback");
    private static final ResourceLocation LUCK_ID = id("broken_core_luck");
    private static final ResourceLocation SHARD_SLOT_ID = id("broken_core_shard_slots");
    private static final ResourceLocation AMULET_SLOT_ID = id("broken_core_amulet_slots");

    private static final ResourceLocation CURIO_ID = id("broken_core");

    public static final int[] EXP_THRESHOLDS = {0, 300, 800, 1800, 3500, 6000, 10000, 16000, 25000, 40000};
    public static final String[] TIER_NAMES = {"凡骨", "染咒", "噬魂", "堕渊", "寂灭", "咒骨", "渊主", "堕神", "灾厄", "神陨"};

    public static final float[] ALL_ATTRIBUTE_BONUS = {
            0.05f, 0.10f, 0.16f, 0.23f, 0.31f, 0.40f, 0.50f, 0.61f, 0.73f, 0.85f
    };

    public static final float[][] SOUL_DRAIN = {
            {1.5f, 0.5f, 0}, {2.5f, 0.8f, 0}, {4f, 1.2f, 0}, {5.5f, 1.6f, 0},
            {7f, 2f, 0}, {9f, 2.5f, 0}, {11f, 3f, 1}, {13f, 3.5f, 2},
            {16f, 4f, 3}, {20f, 5f, 5}
    };

    public static final float[] DODGE_CHANCE = {0.05f, 0.08f, 0.12f, 0.16f, 0.21f, 0.26f, 0.31f, 0.36f, 0.41f, 0.50f};
    public static final float[] DODGE_REFLECT = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.10f, 0.15f, 0.25f};

    public static final float[] SHIELD_MAX = {3, 5, 8, 12, 16, 21, 27, 34, 42, 50};
    public static final int[] SHIELD_CD = {25, 23, 21, 19, 18, 17, 16, 15, 14, 12};

    public static final float[] CALAMITY_RATE = {0.015f, 0.02f, 0.03f, 0.04f, 0.05f, 0.06f, 0.07f, 0.08f, 0.09f, 0.10f};
    public static final float[] CALAMITY_CAP = {0.08f, 0.15f, 0.25f, 0.35f, 0.48f, 0.60f, 0.75f, 0.90f, 1.10f, 1.30f};

    private static final double CURSE_DAMAGE = -0.3;
    private static final double CURSE_HEALTH = -4.0;
    private static final double CURSE_DEEP_WOUND = 0.2;
    private static final double CURSE_SCARRED = 0.3;

    public BrokenCoreItem(Properties properties) {
        super(properties);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static int[] getExpThresholds() {
        return GodfallConfig.tierExp();
    }

    // ==================== 状态读写（DataComponent） ====================

    public int getExp(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CORE_EXP.get(), 0);
    }

    public int getTier(ItemStack stack) {
        int exp = getExp(stack);
        int[] thresholds = getExpThresholds();
        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (exp >= thresholds[i]) {
                return i;
            }
        }
        return 0;
    }

    public static ResourceLocation curioId() {
        return CURIO_ID;
    }

    // ==================== 属性 ====================

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create();
        int tier = getTier(stack);
        boolean rev = tier >= GodfallConfig.REVERSE_CURSE_TIER.get();
        float b = ALL_ATTRIBUTE_BONUS[tier];

        double curseAtk = rev ? +0.30 : -0.30;
        map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(DAMAGE_ID, curseAtk,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        map.put(Attributes.ARMOR, new AttributeModifier(ARMOR_ID, b,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        map.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(SPEED_ID, b,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        map.put(Attributes.ATTACK_SPEED, new AttributeModifier(ATK_SPEED_ID, b,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        map.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(TOUGH_ID, b,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        map.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(KB_RESIST_ID, b,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        map.put(Attributes.LUCK, new AttributeModifier(LUCK_ID, b,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        double curseHp = rev ? +4.0 : CURSE_HEALTH;
        double hp = curseHp + 0.5 * tier;
        map.put(Attributes.MAX_HEALTH, new AttributeModifier(HEALTH_ID, hp,
                AttributeModifier.Operation.ADD_VALUE));

        return map;
    }

    // ==================== tick：诅咒 / 深渊视界 / 护盾 ====================

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity().level().isClientSide()) {
            return;
        }
        if (!(slotContext.entity() instanceof Player player)) {
            return;
        }

        int tier = getTier(stack);
        boolean rev = tier >= GodfallConfig.REVERSE_CURSE_TIER.get();

        if (GodfallConfig.ENABLE_CURSE_SYSTEM.get() && player.tickCount % 20 == 0) {
            if (rev) {
                player.heal(0.6f);
            } else if (!CurseTransferState.isAbsorbing(player, "agony")) {
                player.hurt(player.damageSources().magic(), 0.5f);
            }
        }

        if (GodfallConfig.ENABLE_ABYSS_VISION.get()) {
            applyAbyssVision(player, tier);
        }
        tickShield(stack, tier);
    }

    private void tickShield(ItemStack stack, int tier) {
        int cd = stack.getOrDefault(ModDataComponents.CORE_SHIELD_CD.get(), 0);
        float max = GodfallConfig.SHIELD_MAX_BASE.get() + SHIELD_MAX[tier];
        int cooldownTicks = GodfallConfig.SHIELD_COOLDOWN_TICK.get();
        if (cd > 0) {
            stack.set(ModDataComponents.CORE_SHIELD_CD.get(), cd - 1);
        } else {
            stack.set(ModDataComponents.CORE_SHIELD_HP.get(), max);
            stack.set(ModDataComponents.CORE_SHIELD_CD.get(), cooldownTicks * 20);
        }
    }

    public float absorbDamage(ItemStack stack, float incoming, int tier) {
        float shield = stack.getOrDefault(ModDataComponents.CORE_SHIELD_HP.get(), 0f);
        if (shield <= 0) {
            return incoming;
        }
        float absorbed = Math.min(shield, incoming);
        stack.set(ModDataComponents.CORE_SHIELD_HP.get(), shield - absorbed);
        return incoming - absorbed;
    }

    public boolean isShieldActive(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CORE_SHIELD_HP.get(), 0f) > 0;
    }

    private void applyAbyssVision(Player player, int tier) {
        if (tier < 1) {
            return;
        }
        int dur = 400;
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, dur, 0, false, false, true));
        if (tier >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, dur, 0, false, false, true));
            if (player.isUnderWater()) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, dur, 0, false, false, true));
            }
        }
        if (tier >= 3 && player.tickCount % 40 == 0) {
            int range = tier >= 5 ? 15 : 10;
            AABB box = new AABB(player.blockPosition()).inflate(range);
            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e instanceof Mob && e.isAlive() && !e.isAlliedTo(player))) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
            }
        }
        if (tier >= 8 && player.tickCount % 30 == 0) {
            int amp = tier >= 9 ? 1 : 0;
            AABB box = new AABB(player.blockPosition()).inflate(20);
            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e instanceof Mob && e.isAlive() && !e.isAlliedTo(player))) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, amp, false, false));
            }
        }
    }

    // ==================== 诅咒相关静态工具 ====================

    public static double getDeepWoundMultiplier(int tier) {
        boolean rev = tier >= GodfallConfig.REVERSE_CURSE_TIER.get();
        return rev ? 1.0 - CURSE_DEEP_WOUND : 1.0 + CURSE_DEEP_WOUND;
    }

    public static double getScarredRatio(int tier) {
        boolean rev = tier >= GodfallConfig.REVERSE_CURSE_TIER.get();
        return rev ? 0 : CURSE_SCARRED;
    }

    public static boolean isReverse(int tier) {
        return tier >= GodfallConfig.REVERSE_CURSE_TIER.get();
    }

    public static boolean hasCalamityDR(int tier) {
        return tier >= 5;
    }

    /**
     * 是否同时承受全部 8 条诅咒：佩戴破厄之核、未到反转境界、且咒厄转移坠没有吸收任何诅咒。
     */
    public static boolean hasAllEightCursesActive(Player player) {
        if (player.level().isClientSide()) {
            return false;
        }
        var coreOpt = CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(s -> s.getItem() instanceof BrokenCoreItem));
        if (coreOpt.isEmpty()) {
            return false;
        }
        ItemStack core = coreOpt.get().stack();
        int tier = ((BrokenCoreItem) core.getItem()).getTier(core);
        if (tier >= GodfallConfig.REVERSE_CURSE_TIER.get()) {
            return false;
        }
        return CurseTransferState.getAbsorbedCurses(player).isEmpty();
    }

    // ==================== 佩戴 / 卸下 ====================
    // 槽位格数与 1.20.1 保持一致：神位碎片槽裸配 0 格、护符槽裸配 2 格，
    // 戴上破厄之核 / 神格后才由 TrinketSlotSyncEvents 把 baseSize 撑到 13 / 5；
    // 摘下核则饰品收回背包 + 格数收回。格数绝不能靠槽位修饰符：修饰符在
    // 「摘下核 → 存档 → 重进」这一轮会丢，槽位一旦变 0 格，碎片就会被判非法退回背包。
    //
    // 注意：这里不能实现 onUnequip 去「收回全部饰品」。
    // Curios 自己在同步/整理饰品栏时会临时清空再写回槽位，那时也会触发 onUnequip，
    // 一触发就会把刚戴上去的饰品全弹回背包（表现出来就是「饰品戴不上」）。
    // 「必须先戴破厄之核」统一交给 TrinketSlotSyncEvents 每 20 tick 检查一次。

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack currentStack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                TrinketSlotSyncEvents.ensureSlots(player, handler));
    }

    // ==================== 经验 ====================

    public void addExp(ItemStack stack, int amount, Player player) {
        int oldTier = getTier(stack);
        int scaledAmount = (int) (amount * GodfallConfig.CORE_EXP_MULTIPLIER.get());
        if (scaledAmount <= 0) {
            return;
        }
        int[] thresholds = getExpThresholds();
        int newExp = Math.min(getExp(stack) + scaledAmount, thresholds[9]);
        stack.set(ModDataComponents.CORE_EXP.get(), newExp);

        int newTier = getTier(stack);
        if (newTier > oldTier) {
            if (GodfallConfig.ENABLE_BREAKTHROUGH_NOTICE.get()) {
                player.sendSystemMessage(Component.translatable(
                        "message.godfall_trinkets.breakthrough", TIER_NAMES[newTier])
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
            if (GodfallConfig.ENABLE_BREAKTHROUGH_SOUND.get()) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
    }

    // ==================== Tooltip ====================

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int tier = getTier(stack);
        int exp = getExp(stack);
        int[] thresholds = getExpThresholds();

        tooltip.add(Component.translatable("tooltip.godfall_trinkets.tier", TIER_NAMES[tier])
                .withStyle(ChatFormatting.GOLD));
        if (tier < 9) {
            tooltip.add(Component.translatable("tooltip.godfall_trinkets.exp", exp, thresholds[tier + 1])
                    .withStyle(ChatFormatting.GRAY));
        }

        if (Screen.hasShiftDown()) {
            boolean rev = tier >= GodfallConfig.REVERSE_CURSE_TIER.get();
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("tooltip.godfall_trinkets.curses_header")
                    .withStyle(rev ? ChatFormatting.GREEN : ChatFormatting.DARK_RED));
            addCurseLines(tooltip, rev);
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("tooltip.godfall_trinkets.blessings_header")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            addBlessingLines(tooltip, tier);
        } else if (!Screen.hasControlDown()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("tooltip.godfall_trinkets.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
            if (stack.getItem() instanceof GodCoreItem) {
                tooltip.add(Component.translatable("tooltip.godfall_trinkets.hold_ctrl")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private void addCurseLines(List<Component> tooltip, boolean rev) {
        String prefix = "tooltip.godfall_trinkets.curse.";
        String suffix = rev ? "_reversed" : "";
        for (int i = 1; i <= 8; i++) {
            tooltip.add(Component.translatable(prefix + i + suffix).withStyle(ChatFormatting.RED));
        }
    }

    private void addBlessingLines(List<Component> tooltip, int tier) {
        String prefix = "tooltip.godfall_trinkets.blessing.";
        float b = ALL_ATTRIBUTE_BONUS[tier] * 100;
        tooltip.add(Component.translatable(prefix + "1", String.format("%.0f", b))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        for (int i = 2; i <= 8; i++) {
            tooltip.add(Component.translatable(prefix + i, tier).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, Item.TooltipContext context, ItemStack stack) {
        // Curios 传进来的 list 可能是不可变的（创造模式搜索树会传 List.of()），不能直接 add
        List<Component> result = new ArrayList<>(tooltips);
        result.add(Component.translatable("curios.identifier.broken_core_slot")
                .withStyle(ChatFormatting.DARK_GREEN));
        return result;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        String identifier = slotContext.identifier();
        String path = identifier.indexOf(':') >= 0
                ? identifier.substring(identifier.indexOf(':') + 1)
                : identifier;
        return "broken_core_slot".equals(path);
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        // 只有创造模式能取下，防止玩家把已养成的核随随便便摘掉
        return slotContext.entity() instanceof Player player && player.isCreative();
    }

    @Override
    public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source,
                                       boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }
}
