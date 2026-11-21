package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.config.GodfallConfig;
import com.lazybones.godfalltrinkets.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/**
 * 神位碎片击杀掉落：怪物列表与概率全部读配置。
 * 1.21 差异：{@code ForgeRegistries} → {@link BuiltInRegistries}，{@code RegistryObject} → {@link DeferredItem}。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public class GodShardDropHandler {

    // 与 GodfallConfig 的 SHARD_DROP_MOBS / SHARD_DROP_CHANCES 索引一一对应
    private static final List<DeferredItem<? extends Item>> SHARD_ITEMS = List.of(
            ModItems.SHARD_SKY, ModItems.SHARD_SUN, ModItems.SHARD_LIFE, ModItems.SHARD_STRENGTH,
            ModItems.SHARD_FATE, ModItems.SHARD_WIND, ModItems.SHARD_SPIRIT, ModItems.SHARD_BARRIER,
            ModItems.SHARD_VOID, ModItems.SHARD_SHADOW, ModItems.SHARD_THUNDER, ModItems.SHARD_STAR,
            ModItems.SHARD_RUNE
    );

    @SubscribeEvent
    public static void onMobKilled(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof Player player)) {
            return;
        }
        if (dead.level().isClientSide()) {
            return;
        }

        for (int i = 0; i < 13; i++) {
            List<? extends String> mobs = GodfallConfig.SHARD_DROP_MOBS.get(i).get();
            List<? extends Double> chances = GodfallConfig.SHARD_DROP_CHANCES.get(i).get();
            double rateMod = GodfallConfig.SHARD_DROP_RATE_MOD.get();
            int size = Math.min(mobs.size(), chances.size());
            for (int j = 0; j < size; j++) {
                if (matchesMob(dead, mobs.get(j))
                        && player.getRandom().nextFloat() < chances.get(j) * rateMod) {
                    dropShard(player, SHARD_ITEMS.get(i));
                    break;
                }
            }
        }
    }

    /** 支持 :powered=带电苦力怕 / :captain=掠夺者队长，否则按注册名匹配 */
    private static boolean matchesMob(LivingEntity dead, String mobId) {
        if (mobId.endsWith(":powered")) {
            return dead instanceof Creeper creeper && creeper.isPowered();
        }
        if (mobId.endsWith(":captain")) {
            return dead instanceof Pillager pillager && isRaidCaptain(pillager);
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());
        return key != null && key.toString().equals(mobId);
    }

    /** 掠夺者队长：头上戴着灾厄旗帜 */
    private static boolean isRaidCaptain(Pillager pillager) {
        return pillager.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof BannerItem;
    }

    /** 已拥有（Curios 或背包）则不再掉落 */
    private static boolean hasShard(Player player, DeferredItem<? extends Item> shard) {
        Item item = shard.get();
        boolean inCurios = CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(s -> s.is(item)))
                .isPresent();
        if (inCurios) {
            return true;
        }
        return player.getInventory().contains(new ItemStack(item));
    }

    private static void dropShard(Player player, DeferredItem<? extends Item> shard) {
        if (hasShard(player, shard)) {
            return;
        }
        player.spawnAtLocation(new ItemStack(shard.get()), 1.0f);
    }
}
