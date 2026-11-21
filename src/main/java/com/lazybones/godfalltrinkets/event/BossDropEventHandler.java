package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 凋零 / 监守者击杀掉落（1.20.1 BossDropEventHandler 的 1.21 版）。
 * <p>
 * 旧版里落地标记有两套键名（本类 {@code wither_touch_ground_flag}、
 * {@link WardenWitherTrackEvent} 的 {@code wither_touch_ground}），这里按旧行为原样保留。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public final class BossDropEventHandler {

    private static final String TAG_WITHER_TOUCH_GROUND = "wither_touch_ground_flag";

    private BossDropEventHandler() {
    }

    @SubscribeEvent
    public static void trackWitherGroundState(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof WitherBoss wither) || wither.level().isClientSide()) {
            return;
        }
        if (wither.onGround()) {
            wither.getPersistentData().putBoolean(TAG_WITHER_TOUCH_GROUND, true);
        }
    }

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Entity killerSource = event.getSource().getEntity();
        if (!(dead.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(killerSource instanceof Player player)) {
            return;
        }

        if (dead instanceof WitherBoss wither) {
            handleWitherDrops(serverLevel, player, wither);
        } else if (dead instanceof Warden warden) {
            handleWardenDrops(serverLevel, player, warden);
        }
    }

    /** 枯骨之心必掉；王者之握 / 凋零王冠 / 凋零脊骨坠按战斗条件掉落 */
    private static void handleWitherDrops(ServerLevel level, Player killer, WitherBoss wither) {
        spawnDropItem(killer, new ItemStack(ModItems.WITHER_HEART.get()));

        CompoundTag witherNbt = wither.getPersistentData();
        if (!witherNbt.getBoolean(TAG_WITHER_TOUCH_GROUND)) {
            spawnDropItem(killer, new ItemStack(ModItems.KINGS_GRIP.get()));
        }
        if (wither.getHealth() <= 150F) {
            spawnDropItem(killer, new ItemStack(ModItems.WITHER_CROWN.get()));
        }
        if (wither.hasEffect(MobEffects.WITHER)) {
            spawnDropItem(killer, new ItemStack(ModItems.WITHER_SPINE.get()));
        }
    }

    /** 监守者四件幽系饰品，条件互相独立，可一次全掉 */
    private static void handleWardenDrops(ServerLevel level, Player killer, Warden warden) {
        BlockPos playerPos = killer.blockPosition();
        int blockLight = level.getBrightness(LightLayer.BLOCK, playerPos);
        float maxHealth = killer.getMaxHealth();
        float health = killer.getHealth();

        if (blockLight >= 12) {
            spawnDropItem(killer, new ItemStack(ModItems.WARDEN_EYE.get()));
        }
        if (health < maxHealth * 0.20F) {
            spawnDropItem(killer, new ItemStack(ModItems.WARDEN_PULSE.get()));
        }
        if (killer.hasEffect(MobEffects.DARKNESS)
                && killer.getEffect(MobEffects.DARKNESS).getAmplifier() >= 2) {
            spawnDropItem(killer, new ItemStack(ModItems.WARDEN_WEIGHT.get()));
        }
        if (blockLight == 0) {
            spawnDropItem(killer, new ItemStack(ModItems.WARDEN_ARMORBREAK.get()));
        }
    }

    private static void spawnDropItem(Player player, ItemStack stack) {
        player.spawnAtLocation(stack, 1.0F);
    }
}
