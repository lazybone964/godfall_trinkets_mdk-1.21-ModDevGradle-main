package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.config.GodfallConfig;
import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.util.ItemNbt;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * 九阶长剑：铁砧锻造进度、Tooltip、增伤、吸血。
 * <p>
 * 1.20.1 → 1.21.1：Forge 的 {@code LivingHurtEvent} 在 NeoForge 里换成
 * {@code LivingIncomingDamageEvent}（时机相同，都在护甲减伤之前）；
 * {@code ForgeRegistries.ITEMS.getKey} 换成 {@code BuiltInRegistries.ITEM.getKey}。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public class SwordForgeEvents {

    private static final String TAG_PROGRESS = "forge_progress";
    private static final String TAG_LAST_TYPE = "forge_last_type";
    private static final String TAG_GIVEN_BLANK = "given_white_sword";

    private record TierData(DeferredItem<Item> sword, Supplier<Item> material, String tierTooltipKey) {
    }

    private static final TierData[] TIERS = new TierData[]{
            new TierData(ModItems.WHITE_SWORD_BLANK, () -> Items.IRON_INGOT, "tooltip.sword.t0"),
            new TierData(ModItems.IRON_WHITE_SWORD, () -> Items.DIAMOND, "tooltip.sword.t1"),
            new TierData(ModItems.DIAMOND_WHITE_BLADE, ModItems.GODFRAGMENT, "tooltip.sword.t2"),
            new TierData(ModItems.SHADOW_SWORD, ModItems.GOD_INGOT, "tooltip.sword.t3"),
            new TierData(ModItems.CORE_CONQUEST_SWORD, ModItems.CONQUEST_INGOT, "tooltip.sword.t4"),
            new TierData(ModItems.WAR_BLADE, ModItems.WITHER_SHARD, "tooltip.sword.t5"),
            new TierData(ModItems.WITHER_BLADE, ModItems.WARDEN_HEART, "tooltip.sword.t6"),
            new TierData(ModItems.WARDEN_SWORD, ModItems.ABYSS_GOD_INGOT, "tooltip.sword.t7"),
            new TierData(ModItems.ABYSS_SWORD, ModItems.DRAGON_SOUL, "tooltip.sword.t8")
    };

    private static TierData getTier(ItemStack stack) {
        for (TierData tier : TIERS) {
            if (stack.is(tier.sword().get())) {
                return tier;
            }
        }
        return null;
    }

    // ==================== 铁砧锻造进度 +1 ====================

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        TierData tier = getTier(left);
        if (tier == null || !right.is(tier.material().get())) {
            return;
        }
        CompoundTag tag = ItemNbt.get(left);
        int progress = tag.getInt(TAG_PROGRESS);
        int maxProgress = GodfallConfig.SWORD_FORGE_MAX_PROGRESS.get();
        String currentId = left.getItem().builtInRegistryHolder().key().location().toString();
        String lastType = tag.getString(TAG_LAST_TYPE);
        // 跨阶 NBT 泄漏检测：进度满且物品已经换了一阶，进度归零重来
        if (progress >= maxProgress && !currentId.equals(lastType) && !lastType.isEmpty()) {
            progress = 0;
            tag.putInt(TAG_PROGRESS, 0);
            ItemNbt.save(left, tag);
        }
        if (progress >= maxProgress) {
            return;
        }
        ItemStack output = left.copy();
        ItemNbt.putInt(output, TAG_PROGRESS, progress + 1);
        ItemNbt.putString(output, TAG_LAST_TYPE, currentId);
        event.setOutput(output);
        event.setMaterialCost(1);
        event.setCost(1);
    }

    // ==================== Tooltip ====================

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        TierData tier = getTier(stack);
        if (tier == null) {
            return;
        }
        int progress = ItemNbt.getInt(stack, TAG_PROGRESS);
        int maxProgress = GodfallConfig.SWORD_FORGE_MAX_PROGRESS.get();
        event.getToolTip().add(Component.translatable("tooltip.sword_forge_progress", progress));
        if (progress < maxProgress) {
            event.getToolTip().add(Component.translatable("tooltip.sword_locked")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            event.getToolTip().add(Component.translatable("tooltip.sword_ready")
                    .withStyle(ChatFormatting.GREEN));
        }
        event.getToolTip().add(Component.translatable(tier.tierTooltipKey())
                .withStyle(ChatFormatting.AQUA));
        if (stack.is(ModItems.END_DRAGON_SWORD.get())) {
            event.getToolTip().add(Component.translatable("tooltip.sword.t9_1"));
            event.getToolTip().add(Component.translatable("tooltip.sword.t9_2"));
        }
    }

    // ==================== 新玩家赠送剑胚 ====================

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        CompoundTag persistent = player.getPersistentData();
        if (persistent.getBoolean(TAG_GIVEN_BLANK)) {
            return;
        }
        ItemStack blank = new ItemStack(ModItems.WHITE_SWORD_BLANK.get());
        ItemNbt.putInt(blank, TAG_PROGRESS, 0);
        player.getInventory().add(blank);
        persistent.putBoolean(TAG_GIVEN_BLANK, true);
        player.sendSystemMessage(Component.literal("§b获得纯白剑胚，在铁砧使用对应材料积累锻造进度！"));
    }

    // ==================== 系列剑增伤 ====================

    @SubscribeEvent
    public static void onSwordDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        ItemStack hand = player.getMainHandItem();
        TierData tier = getTier(hand);
        if (tier == null) {
            return;
        }
        LivingEntity target = event.getEntity();
        float multiplier = 1.0F;

        boolean t1 = hand.is(ModItems.IRON_WHITE_SWORD.get());
        boolean t2 = hand.is(ModItems.DIAMOND_WHITE_BLADE.get());
        boolean t3 = hand.is(ModItems.SHADOW_SWORD.get());
        boolean t4 = hand.is(ModItems.CORE_CONQUEST_SWORD.get());
        boolean t5 = hand.is(ModItems.WAR_BLADE.get());
        boolean t6 = hand.is(ModItems.WITHER_BLADE.get());
        boolean t7 = hand.is(ModItems.WARDEN_SWORD.get());
        boolean t8 = hand.is(ModItems.ABYSS_SWORD.get());
        boolean t9 = hand.is(ModItems.END_DRAGON_SWORD.get());

        // 亡灵
        if (target instanceof Zombie || target instanceof Skeleton || target instanceof ZombifiedPiglin) {
            if (t1) {
                multiplier *= 1.25F;
            }
            if (t2 || t3 || t4 || t5 || t6 || t7 || t8 || t9) {
                multiplier *= GodfallConfig.SWORD_UNDEAD_DMG.get().floatValue();
            }
        }
        // 下界生物
        if (target instanceof Piglin || target instanceof Blaze || target instanceof Ghast) {
            if (t3 || t4 || t5 || t6 || t7 || t8 || t9) {
                multiplier *= GodfallConfig.SWORD_NETHER_DMG.get().floatValue();
            }
        }
        // 普通怪物
        if (!(target instanceof EnderDragon) && !(target instanceof WitherBoss) && !(target instanceof Warden)) {
            if (t4) {
                multiplier *= 1.20F;
            }
            if (t5 || t6 || t7 || t8 || t9) {
                multiplier *= GodfallConfig.SWORD_MOB_DMG.get().floatValue();
            }
        }
        // 凋零类增伤 + 凋零Ⅱ
        if (target instanceof WitherBoss || target instanceof WitherSkeleton) {
            if (t6 || t7 || t8 || t9) {
                multiplier *= GodfallConfig.SWORD_WITHER_DMG.get().floatValue();
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            }
        }
        // 黑暗环境增伤
        if (player.level().getMaxLocalRawBrightness(player.blockPosition()) < 4) {
            if (t7 || t8 || t9) {
                multiplier *= GodfallConfig.SWORD_DARK_DMG.get().floatValue();
            }
        }
        // Boss 增伤
        if (target instanceof EnderDragon || target instanceof WitherBoss || target instanceof Warden) {
            if (t8) {
                multiplier *= GodfallConfig.SWORD_BOSS_DMG.get().floatValue();
            }
        }
        // 终末龙征：末地相关目标 + 鞘翅飞行
        if (t9) {
            if (target instanceof EnderDragon || target instanceof WitherBoss
                    || target instanceof Warden || target instanceof EnderMan
                    || target instanceof net.minecraft.world.entity.monster.Shulker) {
                multiplier *= GodfallConfig.SWORD_T9_BOSS_DMG.get().floatValue();
            }
            if (GodfallConfig.ENABLE_ELYTRA_BONUS.get() && player.isFallFlying()) {
                multiplier *= GodfallConfig.SWORD_T9_ELYTRA_DMG.get().floatValue();
            }
        }
        event.setAmount(event.getAmount() * multiplier
                * GodfallConfig.SWORD_GLOBAL_DAMAGE.get().floatValue());
    }

    // ==================== 吸血 ====================

    @SubscribeEvent
    public static void onSwordLifesteal(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        ItemStack hand = player.getMainHandItem();
        float mod = GodfallConfig.SWORD_LIFESTEAL_MOD.get().floatValue();
        float heal = 0F;
        if (hand.is(ModItems.WAR_BLADE.get()) || hand.is(ModItems.WITHER_BLADE.get())
                || hand.is(ModItems.WARDEN_SWORD.get()) || hand.is(ModItems.ABYSS_SWORD.get())) {
            heal = 1.2F * mod;
        }
        if (hand.is(ModItems.END_DRAGON_SWORD.get())) {
            heal = 2.0F * mod;
        }
        if (heal > 0) {
            player.heal(heal);
        }
    }
}
