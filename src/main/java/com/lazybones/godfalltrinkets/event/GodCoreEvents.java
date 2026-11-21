package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.ModAttachments;
import com.lazybones.godfalltrinkets.config.GodfallConfig;
import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.item.custom.BrokenCoreItem;
import com.lazybones.godfalltrinkets.item.custom.GodCoreItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 神格 8 大终局权能。
 *
 * 1.21 迁移要点：
 * 1. {@code TickEvent.PlayerTickEvent(END)} → {@link PlayerTickEvent.Post}；
 * 2. {@code LivingHurtEvent} / {@code LivingAttackEvent} / {@code LivingDamageEvent} 合并为
 *    {@link LivingIncomingDamageEvent}，所以 8 条权能里与伤害相关的部分**收在一个处理器里**，
 *    自己控制「先免疫 → 再增伤/减伤 → 最后封顶」的顺序（旧版三个处理器之间的顺序其实是不确定的）；
 * 3. 属性修饰符 ID 用 ResourceLocation，{@code MULTIPLY_BASE} → {@code ADD_MULTIPLIED_BASE}；
 * 4. persistentData 的两个发放标记改用 Attachment（且已 copyOnDeath），所以
 *    旧版的 {@code PlayerEvent.Clone} 复制逻辑直接删掉；
 * 5. 旧版「破甲」把目标 {@code ARMOR} / {@code ARMOR_TOUGHNESS} 的 baseValue 直接设 0，
 *    这会把目标的护甲永久清空（是旧版的 bug）。1.21 版本只保留「清除正面药水效果」，
 *    破甲数值部分留到阶段四用 DamageContainer 正确实现。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public class GodCoreEvents {

    private static final ResourceLocation GOD_ATTR_ID = ResourceLocation.fromNamespaceAndPath(
            GodfallTrinkets.MOD_ID, "god_core_all_attr");

    /** 玩家是否佩戴着已进化的神格 */
    public static boolean hasGodCore(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(GodCoreItem::isGodCore))
                .isPresent();
    }

    // ==================== 权能 1：全属性 +35% ====================

    @SubscribeEvent
    public static void onPlayerTickAttr(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        boolean active = hasGodCore(player);
        float bonus = active ? GodfallConfig.GOD_CORE_ALL_ATTR_ADD.get().floatValue() : 0f;
        applyAttr(player, Attributes.ATTACK_DAMAGE, bonus);
        applyAttr(player, Attributes.ARMOR, bonus);
        applyAttr(player, Attributes.MOVEMENT_SPEED, bonus);
        applyAttr(player, Attributes.ATTACK_SPEED, bonus);
        applyAttr(player, Attributes.ARMOR_TOUGHNESS, bonus);
        applyAttr(player, Attributes.KNOCKBACK_RESISTANCE, bonus);
        applyAttr(player, Attributes.LUCK, bonus);
    }

    private static void applyAttr(Player player, Holder<Attribute> attribute, float bonus) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (bonus <= 0) {
            if (instance.getModifier(GOD_ATTR_ID) != null) {
                instance.removeModifier(GOD_ATTR_ID);
            }
            return;
        }
        if (instance.getModifier(GOD_ATTR_ID) == null) {
            instance.addPermanentModifier(new AttributeModifier(GOD_ATTR_ID, bonus,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    // ==================== 权能 3/4/5/8：伤害相关（合并处理） ====================

    @SubscribeEvent
    public static void onGodIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();

        // 权能 4：全类型环境伤害免疫（最优先，直接取消）
        if (GodfallConfig.GOD_IMMUNE_ALL_ENV_DAMAGE.get()
                && target instanceof Player victim && hasGodCore(victim) && isImmune(source)) {
            event.setCanceled(true);
            return;
        }

        // 权能 3（增伤）+ 权能 8（破甲：清零目标护甲/韧性 + 清正面药水）
        if (source.getEntity() instanceof Player attacker && hasGodCore(attacker)) {
            event.setAmount(event.getAmount()
                    * (1.0f + GodfallConfig.GOD_CORE_GLOBAL_DAMAGE_UP.get().floatValue()));
            if (GodfallConfig.ENABLE_ARMOR_PIERCE.get()) {
                pierceArmor(target);
                stripBeneficialEffects(target);
            }
        }

        // 权能 3（减伤）+ 权能 5（单次伤害封顶）
        if (target instanceof Player victim && hasGodCore(victim)) {
            event.setAmount(event.getAmount()
                    * (1.0f - GodfallConfig.GOD_CORE_DAMAGE_REDUCE.get().floatValue()));
            float cap = victim.getMaxHealth() * GodfallConfig.MAX_SINGLE_DAMAGE_RATIO.get().floatValue();
            if (event.getAmount() > cap) {
                event.setAmount(cap);
            }
        }
    }

    private static boolean isImmune(DamageSource source) {
        return source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.CACTUS) || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.LIGHTNING_BOLT) || source.is(DamageTypes.EXPLOSION)
                || source.is(DamageTypes.PLAYER_EXPLOSION) || source.is(DamageTypes.FALLING_ANVIL)
                || source.is(DamageTypes.FLY_INTO_WALL) || source.is(DamageTypes.FREEZE)
                || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.SWEET_BERRY_BUSH)
                || source.is(DamageTypes.STALAGMITE) || source.is(DamageTypes.STARVE);
    }

    /** 破甲：直接把目标的护甲/韧性基础值清零（原版设计如此，神格就是要这个逼格） */
    private static void pierceArmor(LivingEntity target) {
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(0);
        }
        AttributeInstance toughness = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness != null) {
            toughness.setBaseValue(0);
        }
    }

    private static void stripBeneficialEffects(LivingEntity target) {
        // 1.21 的 MobEffectInstance#getEffect 返回 Holder<MobEffect>，用 value() 比较
        for (MobEffectInstance eff : target.getActiveEffects()) {
            if (eff.getEffect().value().isBeneficial()
                    && eff.getEffect().value() != MobEffects.REGENERATION.value()
                    && eff.getEffect().value() != MobEffects.HEAL.value()) {
                target.removeEffect(eff.getEffect());
            }
        }
    }

    // ==================== 权能 6：无敌帧延长 ====================

    @SubscribeEvent
    public static void onGodInvulnerable(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !hasGodCore(player)) {
            return;
        }
        int invulnTick = GodfallConfig.INVULNERABLE_TICK.get();
        if (player.invulnerableTime > 0 && player.invulnerableTime < invulnTick) {
            player.invulnerableTime = invulnTick;
        }
    }

    // ==================== 权能 7：创造飞行移速 ====================

    @SubscribeEvent
    public static void onGodFlight(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!hasGodCore(player)) {
            return;
        }
        float flySpeed = GodfallConfig.CREATIVE_FLY_SPEED_MOD.get().floatValue();
        if (player.getAbilities().flying && !player.level().isClientSide()) {
            if (player.getAbilities().getFlyingSpeed() < flySpeed) {
                player.getAbilities().setFlyingSpeed(flySpeed);
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.onUpdateAbilities();
                }
            }
        }
    }

    // ==================== 破厄之核 / 进化石自动发放 ====================

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        tryGiveBrokenCore(player);
        tryGrantEvolutionStone(player);
    }

    @SubscribeEvent
    public static void onPlayerTickGive(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        // 每 5 秒兜底一次（覆盖登录事件没触发的情况）
        if (player.tickCount % 100 == 0) {
            tryGiveBrokenCore(player);
            tryGrantEvolutionStone(player);
        }
    }

    private static void tryGiveBrokenCore(Player player) {
        if (!GodfallConfig.AUTO_GIVE_BROKEN_CORE.get()) {
            return;
        }
        if (player.getData(ModAttachments.GOT_BROKEN_CORE)) {
            return;
        }
        boolean alreadyHas = CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(s -> s.is(ModItems.BROKEN_CORE.get())
                        || s.is(ModItems.GOD_CORE.get())))
                .isPresent();
        player.setData(ModAttachments.GOT_BROKEN_CORE, true);
        if (!alreadyHas) {
            player.getInventory().add(new ItemStack(ModItems.BROKEN_CORE.get()));
        }
    }

    private static void tryGrantEvolutionStone(Player player) {
        if (!GodfallConfig.AUTO_GOD_UPGRADE_STONE.get()) {
            return;
        }
        if (GodfallConfig.ENABLE_GOD_CORE_EVOLUTION.get() && hasGodCore(player)) {
            return;
        }
        if (player.getData(ModAttachments.GOT_EVOLUTION_STONE)) {
            return;
        }

        // 必须佩戴「非神格」的破厄之核，且境界已到 9
        var coreResult = CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(s -> s.is(ModItems.BROKEN_CORE.get())
                        && !GodCoreItem.isGodCore(s)));
        if (coreResult.isEmpty()) {
            return;
        }
        ItemStack core = coreResult.get().stack();
        if (core.getItem() instanceof BrokenCoreItem brokenCore && brokenCore.getTier(core) < 9) {
            return;
        }

        // 神位碎片槽里至少 13 格有东西
        var handlerOpt = CuriosApi.getCuriosInventory(player);
        if (handlerOpt.isEmpty()) {
            return;
        }
        var shardSlot = handlerOpt.get().getStacksHandler("god_shard_slot");
        if (shardSlot.isEmpty()) {
            return;
        }
        var stacks = shardSlot.get().getStacks();
        int count = 0;
        for (int i = 0; i < stacks.getSlots(); i++) {
            if (!stacks.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        if (count < 13) {
            return;
        }

        player.setData(ModAttachments.GOT_EVOLUTION_STONE, true);
        player.getInventory().add(new ItemStack(ModItems.GOD_EVOLUTION_STONE.get()));
    }
}
