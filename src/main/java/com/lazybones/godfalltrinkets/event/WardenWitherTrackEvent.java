package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 凋零 / 监守者战斗条件追踪（1.20.1 WardenWitherTrackEvent 的 1.21 版）。
 * <p>
 * 用 {@link EntityTickEvent.Post} 代替已移除的 {@code LivingEvent.LivingTickEvent}；
 * 战斗标记依旧写在实体自身的 {@code getPersistentData()}（NeoForge 21.1 仍保留该补丁接口）。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public final class WardenWitherTrackEvent {

    private static final String TAG_WITHER_TOUCH_GROUND = "wither_touch_ground";
    private static final String TAG_WITHER_HALF_SHIELD_TRIGGER = "wither_half_shield";
    private static final String TAG_WITHER_HAS_WITHER_EFFECT = "target_has_wither_debuff";
    private static final String TAG_LAST_KILLER = "last_killer_uuid";

    private WardenWitherTrackEvent() {
    }

    @SubscribeEvent
    public static void onWitherTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof WitherBoss wither) || wither.level().isClientSide()) {
            return;
        }
        CompoundTag nbt = wither.getPersistentData();
        if (wither.onGround()) {
            nbt.putBoolean(TAG_WITHER_TOUCH_GROUND, true);
        }
        if (wither.getHealth() / wither.getMaxHealth() <= 0.5F) {
            nbt.putBoolean(TAG_WITHER_HALF_SHIELD_TRIGGER, true);
        }
    }

    @SubscribeEvent
    public static void onBossDeathRecordKiller(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) {
            return;
        }
        LivingEntity boss = event.getEntity();
        CompoundTag tag = boss.getPersistentData();
        tag.putUUID(TAG_LAST_KILLER, killer.getUUID());
        if (boss instanceof WitherBoss wither) {
            tag.putBoolean(TAG_WITHER_HAS_WITHER_EFFECT, wither.hasEffect(MobEffects.WITHER));
        }
    }

    public static Player getKillerFromBoss(LivingEntity boss) {
        CompoundTag tag = boss.getPersistentData();
        if (!tag.hasUUID(TAG_LAST_KILLER)) {
            return null;
        }
        Level level = boss.level();
        return level.getPlayerByUUID(tag.getUUID(TAG_LAST_KILLER));
    }

    public static int getBlockLight(Player player) {
        return player.level().getBrightness(LightLayer.BLOCK, player.blockPosition());
    }

    public static int getDarknessStack(Player player) {
        var instance = player.getEffect(MobEffects.DARKNESS);
        return instance == null ? 0 : instance.getAmplifier() + 1;
    }
}
