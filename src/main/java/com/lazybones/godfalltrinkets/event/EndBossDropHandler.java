package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

/**
 * 末影龙终焉龙饰掉落（1.20.1 EndBossDropHandler 的 1.21 版）。
 * <p>
 * {@code LivingEvent.LivingTickEvent} → {@link EntityTickEvent.Post}；
 * 末影水晶判定沿用 {@code EndCrystal.showsBottom()}（自然生成带底座的水晶为 true）。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public final class EndBossDropHandler {

    private static final String TAG_FLY_ONLY = "dragon_fight_no_land";

    private EndBossDropHandler() {
    }

    @SubscribeEvent
    public static void dragonTrackTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || dragon.level().isClientSide()) {
            return;
        }
        CompoundTag nbt = dragon.getPersistentData();
        if (!nbt.contains(TAG_FLY_ONLY)) {
            nbt.putBoolean(TAG_FLY_ONLY, true);
        }

        AABB searchBox = dragon.getBoundingBox().inflate(200.0D);
        List<Player> nearbyPlayers = dragon.level().getEntitiesOfClass(Player.class, searchBox);
        for (Player player : nearbyPlayers) {
            if (!player.isFallFlying() && player.onGround()) {
                nbt.putBoolean(TAG_FLY_ONLY, false);
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onDragonKilled(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        CompoundTag dragonNbt = dragon.getPersistentData();

        // 龙核之心：必掉
        spawnDrop(player, new ItemStack(ModItems.DRAGON_CORE_HEART.get()));

        // 终末之眼：击杀瞬间血量 < 20%
        if (player.getHealth() / player.getMaxHealth() < 0.20F) {
            spawnDrop(player, new ItemStack(ModItems.EYE_OF_THE_END.get()));
        }

        // 残晶冠冕：末地平台 10 颗带底座末影水晶全部完好
        AABB crystalBox = new AABB(-50, 50, -50, 50, 120, 50);
        List<EndCrystal> crystals = level.getEntitiesOfClass(EndCrystal.class, crystalBox);
        int crystalCount = 0;
        for (EndCrystal crystal : crystals) {
            if (crystal.showsBottom()) {
                crystalCount++;
            }
        }
        if (crystalCount >= 10) {
            spawnDrop(player, new ItemStack(ModItems.CRYSTAL_CROWN.get()));
        }

        // 寂灭龙脊：全程鞘翅不落地击杀
        if (dragonNbt.getBoolean(TAG_FLY_ONLY)) {
            spawnDrop(player, new ItemStack(ModItems.DRAGON_SPINE_END.get()));
        }
    }

    private static void spawnDrop(Player player, ItemStack stack) {
        player.spawnAtLocation(stack, 1.2F);
    }
}
