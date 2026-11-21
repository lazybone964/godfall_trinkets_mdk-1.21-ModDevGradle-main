package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.util.CurioHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Random;

/**
 * 凋零 / 监守者系列饰品的被动效果（1.20.1 TrinketBossEffectEvents 的 1.21 版）。
 * <p>
 * 事件迁移：{@code LivingHurtEvent} → {@link LivingIncomingDamageEvent}；
 * {@code MobEffectEvent.Applicable.Result.DENY} → {@code DO_NOT_APPLY}；
 * {@code TickEvent.PlayerTickEvent} → {@link PlayerTickEvent.Post}。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public final class TrinketBossEffectEvents {

    private static final Random RANDOM = new Random();

    private TrinketBossEffectEvents() {
    }

    private static int blockLight(Player player) {
        return player.level().getBrightness(LightLayer.BLOCK, player.blockPosition());
    }

    // ==================== 枯骨之心 ====================

    @SubscribeEvent
    public static void onWitherApply(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || instance.getEffect() != MobEffects.WITHER) {
            return;
        }
        if (CurioHelper.hasTrinket(player, ModItems.WITHER_HEART.get())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (!CurioHelper.hasTrinket(player, ModItems.WITHER_HEART.get())) {
            return;
        }
        if (RANDOM.nextFloat() <= 0.2f && event.getTarget() instanceof LivingEntity target) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
        }
    }

    // ==================== 攻击方增伤 ====================

    @SubscribeEvent
    public static void onOutgoingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)
                || player == event.getEntity()) {
            return;
        }
        float damage = event.getAmount();

        // 王者之握：鞘翅飞行时近战增伤
        if (CurioHelper.hasTrinket(player, ModItems.KINGS_GRIP.get()) && player.isFallFlying()) {
            damage *= 1.25f;
        }

        // 凋零王冠：目标血量更高 / 目标带凋零，各自 ×1.3
        if (CurioHelper.hasTrinket(player, ModItems.WITHER_CROWN.get())) {
            LivingEntity target = event.getEntity();
            if (target.getMaxHealth() > player.getMaxHealth()) {
                damage *= 1.30f;
            }
            if (target.hasEffect(MobEffects.WITHER)) {
                damage *= 1.30f;
            }
        }

        // 幽匿破甲：亮度越低概率越高（亮度 0 时 75%），命中则把伤害抬到 9999 模拟无视护甲
        if (CurioHelper.hasTrinket(player, ModItems.WARDEN_ARMORBREAK.get())) {
            int light = blockLight(player);
            if (light < 15) {
                float chance = (15 - light) * 0.05f;
                if (RANDOM.nextFloat() <= chance) {
                    damage = 9999f;
                }
            }
        }

        event.setAmount(damage);
    }

    // ==================== 承受方 ====================

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        float damage = event.getAmount();

        // 凋零脊骨坠：受伤 30% 立刻转化为治疗
        if (CurioHelper.hasTrinket(player, ModItems.WITHER_SPINE.get())) {
            player.heal(damage * 0.30f);
        }

        // 幽匿重压：亮度不足时按缺口减伤，最高 15%
        if (CurioHelper.hasTrinket(player, ModItems.WARDEN_WEIGHT.get())) {
            int stack = Math.max(0, 15 - blockLight(player));
            damage *= 1f - (stack * 0.01f);
        }

        event.setAmount(damage);
    }

    // ==================== 监守者黑暗被动（持续 buff） ====================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        int stack = Math.max(0, 15 - blockLight(player));
        if (stack <= 0) {
            return;
        }

        // 幽匿之眼：幸运
        if (CurioHelper.hasTrinket(player, ModItems.WARDEN_EYE.get())) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 40, stack - 1, false, false));
        }

        // 幽匿脉动：生命恢复
        if (CurioHelper.hasTrinket(player, ModItems.WARDEN_PULSE.get())) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION, 40, stack - 1, false, false));
        }
    }
}
