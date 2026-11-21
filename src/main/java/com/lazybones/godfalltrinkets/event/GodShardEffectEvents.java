package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.ModAttachments;
import com.lazybones.godfalltrinkets.ModDataComponents;
import com.lazybones.godfalltrinkets.config.GodfallConfig;
import com.lazybones.godfalltrinkets.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import top.theillusivec4.curios.api.CuriosApi;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 13 件神位碎片的效果。
 *
 * 1.21 迁移要点：
 * 1. {@code TickEvent.PlayerTickEvent} → {@link PlayerTickEvent.Post}；
 * 2. {@code LivingHurtEvent} → {@link LivingIncomingDamageEvent}（护甲减免前，语义与旧版一致）；
 * 3. {@code LivingDamageEvent} → {@link LivingDamageEvent.Pre}（减免后，用 {@code getNewDamage/setNewDamage}）；
 * 4. 敏捷/韧性 UUID → ResourceLocation，{@code MULTIPLY_BASE} → {@code ADD_MULTIPLIED_BASE}；
 * 5. 铭纹碎片旧版用 NBT 存「原始附魔」，1.21 改为两个 DataComponent
 *    （{@link ModDataComponents#RUNE_BOOSTED} + {@link ModDataComponents#RUNE_ORIGINAL_ENCHANTS}），
 *    附魔本身也从 Map 变成 {@link ItemEnchantments}；
 * 6. 生命碎片复活冷却改用 {@link ModAttachments#SHARD_LIFE_CD}；
 * 7. {@code setSecondsOnFire(5)} → {@code igniteForSeconds(5)}。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public class GodShardEffectEvents {

    private static final ResourceLocation FATE_LUCK_ID = ResourceLocation.fromNamespaceAndPath(
            GodfallTrinkets.MOD_ID, "shard_fate_luck");
    private static final ResourceLocation BARRIER_SPEED_ID = ResourceLocation.fromNamespaceAndPath(
            GodfallTrinkets.MOD_ID, "shard_barrier_speed");

    /** 防递归：影杀背刺 target.hurt() 会再次触发伤害事件 */
    private static final ThreadLocal<Boolean> HURT_PROCESSING = ThreadLocal.withInitial(() -> false);

    private static boolean hasShard(Player player, DeferredItem<? extends Item> shard) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(s -> s.is(shard.get())))
                .isPresent();
    }

    private static boolean hasBrokenCore(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(s -> s.is(ModItems.BROKEN_CORE.get())
                        || s.is(ModItems.GOD_CORE.get())))
                .isPresent();
    }

    /** 神格加持：佩戴神格时碎片效果 × 配置倍率 */
    private static double getGodCoreBonus(Player player) {
        return GodCoreEvents.hasGodCore(player) ? GodfallConfig.GOD_CORE_SHARD_POWER_MOD.get() : 1.0;
    }

    // ==================== Tick ====================

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean isClient = player.level().isClientSide();
        boolean hasCore = hasBrokenCore(player);

        if (!isClient) {
            handleSkyFlight(player, hasCore);
        }
        if (isClient) {
            return;
        }

        // 铭纹附魔同步
        syncRuneEnchantments(player, hasCore && hasShard(player, ModItems.SHARD_RUNE));

        // 壁垒举盾不减速（放在 hasCore 判断之前，确保卸下时能移除修饰符）
        handleBarrierBlocking(player, hasCore);

        AttributeInstance luckAttr = player.getAttribute(Attributes.LUCK);
        if (!hasCore) {
            if (luckAttr != null && luckAttr.getModifier(FATE_LUCK_ID) != null) {
                luckAttr.removeModifier(FATE_LUCK_ID);
            }
            return;
        }

        // 烈阳白天回血
        if (hasShard(player, ModItems.SHARD_SUN)
                && player.level().getBrightness(LightLayer.SKY, player.blockPosition()) > 12) {
            player.heal(0.1F);
        }

        // 生命常驻再生 X
        if (hasShard(player, ModItems.SHARD_LIFE)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20, 9, true, false));
        }

        // 疾风游泳/攀爬加速
        if (hasShard(player, ModItems.SHARD_WIND)
                && (player.onClimbable() || player.isInWater())) {
            player.setDeltaMovement(player.getDeltaMovement().scale(1.2D));
        }

        // 影杀潜行隐身
        if (hasShard(player, ModItems.SHARD_SHADOW) && player.isCrouching()) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, true, false));
        }

        // 命运幸运 +200%（神格加持后 ×1.5 → 300%）
        if (hasShard(player, ModItems.SHARD_FATE)) {
            double expected = 2.0 * getGodCoreBonus(player);
            if (luckAttr != null) {
                if (luckAttr.getModifier(FATE_LUCK_ID) != null) {
                    luckAttr.removeModifier(FATE_LUCK_ID);
                }
                luckAttr.addPermanentModifier(new AttributeModifier(FATE_LUCK_ID, expected,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        } else if (luckAttr != null && luckAttr.getModifier(FATE_LUCK_ID) != null) {
            luckAttr.removeModifier(FATE_LUCK_ID);
        }
    }

    /** 天穹碎片：生存模式解锁创造飞行 */
    private static void handleSkyFlight(Player player, boolean hasCore) {
        boolean shouldFly = hasCore && hasShard(player, ModItems.SHARD_SKY)
                && GodfallConfig.SHARD_ENABLE[0].get();
        if (shouldFly) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.onUpdateAbilities();
                }
            }
        } else if (!player.isCreative() && !player.isSpectator()) {
            if (player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.onUpdateAbilities();
                }
            }
        }
    }

    /** 壁垒碎片：举盾不减速（原版举盾 ×0.2，这里补 ×4） */
    private static void handleBarrierBlocking(Player player, boolean hasCore) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean shouldApply = hasCore && hasShard(player, ModItems.SHARD_BARRIER)
                && GodfallConfig.SHARD_ENABLE[7].get() && player.isBlocking();
        if (shouldApply) {
            if (speed.getModifier(BARRIER_SPEED_ID) == null) {
                speed.addTransientModifier(new AttributeModifier(BARRIER_SPEED_ID, 4.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        } else if (speed.getModifier(BARRIER_SPEED_ID) != null) {
            speed.removeModifier(BARRIER_SPEED_ID);
        }
    }

    // ==================== 攻击 ====================

    @SubscribeEvent
    public static void onPlayerHurt(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (!hasBrokenCore(player)) {
            return;
        }
        if (HURT_PROCESSING.get()) {
            return;
        }
        HURT_PROCESSING.set(true);
        try {
            ItemStack hand = player.getMainHandItem();
            LivingEntity target = event.getEntity();
            float dmg = event.getAmount();

            if (hasShard(player, ModItems.SHARD_STRENGTH)) {
                dmg *= 1.5F * (float) getGodCoreBonus(player);
            }
            if ((hand.getItem() instanceof BowItem || hand.getItem() instanceof TridentItem)
                    && hasShard(player, ModItems.SHARD_STAR)) {
                dmg *= 1.4F * (float) getGodCoreBonus(player);
            }
            if (hasShard(player, ModItems.SHARD_SUN)) {
                target.igniteForSeconds(5);
            }

            // 落雷只在服务端召唤
            if (!player.level().isClientSide()
                    && hasShard(player, ModItems.SHARD_THUNDER)
                    && player.getRandom().nextFloat() <= GodfallConfig.SHARD_THUNDER_LIGHTNING_CHANCE.get().floatValue()) {
                LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, player.level());
                bolt.setPos(target.getX(), target.getY(), target.getZ());
                bolt.setVisualOnly(true);
                player.level().addFreshEntity(bolt);
            }

            if (hasShard(player, ModItems.SHARD_SHADOW)) {
                Vec3 lookDir = target.getViewVector(1F);
                Vec3 toPlayer = player.position().subtract(target.position()).normalize();
                if (lookDir.dot(toPlayer) > 0) {
                    target.hurt(event.getSource(), dmg
                            * GodfallConfig.SHARD_SHADOW_BACKSTAB_MULT.get().floatValue()
                            * (float) getGodCoreBonus(player));
                }
            }
            event.setAmount(dmg * GodfallConfig.SHARD_GLOBAL_POWER.get().floatValue());
        } finally {
            HURT_PROCESSING.set(false);
        }
    }

    // ==================== 防御 ====================

    @SubscribeEvent
    public static void shieldReflect(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || !player.isBlocking()) {
            return;
        }
        if (!hasBrokenCore(player) || !hasShard(player, ModItems.SHARD_BARRIER)) {
            return;
        }
        float dmg = event.getNewDamage();
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            attacker.hurt(event.getSource(), dmg);
        }
        player.heal(1F);
    }

    @SubscribeEvent
    public static void fallImmune(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getSource().is(DamageTypes.FALL)
                && hasBrokenCore(player) && hasShard(player, ModItems.SHARD_WIND)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void fireImmune(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        var src = event.getSource();
        if ((src.is(DamageTypes.IN_FIRE) || src.is(DamageTypes.ON_FIRE) || src.is(DamageTypes.LAVA))
                && hasBrokenCore(player) && hasShard(player, ModItems.SHARD_SUN)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void thunderProtect(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !hasBrokenCore(player)) {
            return;
        }
        if (!hasShard(player, ModItems.SHARD_THUNDER)) {
            return;
        }
        if (event.getSource().is(DamageTypes.LIGHTNING_BOLT)) {
            event.setCanceled(true);
            return;
        }
        if (player.level().isThundering()) {
            event.setAmount(event.getAmount() * 1.4F * (float) getGodCoreBonus(player));
        }
    }

    @SubscribeEvent
    public static void noKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player player && hasBrokenCore(player)
                && hasShard(player, ModItems.SHARD_STRENGTH)) {
            event.setCanceled(true);
        }
    }

    // ==================== 星陨：弓弩三叉戟蓄力加速 ====================

    @SubscribeEvent
    public static void onRangedCharge(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!hasBrokenCore(player) || !hasShard(player, ModItems.SHARD_STAR)) {
            return;
        }
        if (!GodfallConfig.SHARD_ENABLE[11].get()) {
            return;
        }
        ItemStack stack = event.getItem();
        boolean isRanged = stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem;
        if (!isRanged) {
            return;
        }
        int remaining = event.getDuration();
        if (remaining > 2) {
            event.setDuration(remaining - 1);
        }
    }

    // ==================== 生命：致死不屈 ====================

    @SubscribeEvent
    public static void reviveOnDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!hasBrokenCore(player) || !hasShard(player, ModItems.SHARD_LIFE)) {
            return;
        }
        long gameTime = player.level().getGameTime();
        if (gameTime < player.getData(ModAttachments.SHARD_LIFE_CD)) {
            return;
        }
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();
        player.setData(ModAttachments.SHARD_LIFE_CD,
                gameTime + GodfallConfig.SHARD_LIFE_REVIVE_CD_SECONDS.get() * 20L);
    }

    // ==================== 灵韵 ====================

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!hasBrokenCore(player) || !hasShard(player, ModItems.SHARD_SPIRIT)) {
            return;
        }
        MobEffectInstance effect = event.getEffectInstance();
        if (effect.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) {
            return;
        }
        try {
            // 1.21 的 MobEffectInstance 只有 getter，时长/等级仍靠反射改
            Field durField = MobEffectInstance.class.getDeclaredField("duration");
            durField.setAccessible(true);
            durField.setInt(effect, (int) (durField.getInt(effect) * 2 * getGodCoreBonus(player)));

            Field ampField = MobEffectInstance.class.getDeclaredField("amplifier");
            ampField.setAccessible(true);
            ampField.setInt(effect, ampField.getInt(effect) + 1);
        } catch (Exception ex) {
            GodfallTrinkets.LOGGER.warn("神位碎片·灵韵 修改药水效果失败", ex);
        }
    }

    @SubscribeEvent
    public static void onPotionDrink(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!hasBrokenCore(player) || !hasShard(player, ModItems.SHARD_SPIRIT)) {
            return;
        }
        if (!(event.getItem().getItem() instanceof PotionItem)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));
    }

    // ==================== 铭纹 ====================

    @SubscribeEvent
    public static void runeArmorReduce(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!hasBrokenCore(player) || !hasShard(player, ModItems.SHARD_RUNE)) {
            return;
        }
        for (ItemStack armor : player.getInventory().armor) {
            if (!armor.isEmpty()) {
                event.setNewDamage(event.getNewDamage() * 0.75F);
                return;
            }
        }
    }

    private static void syncRuneEnchantments(Player player, boolean active) {
        List<ItemStack> allStacks = new ArrayList<>(player.getInventory().items);
        allStacks.addAll(player.getInventory().armor);
        allStacks.addAll(player.getInventory().offhand);
        for (ItemStack stack : allStacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (active) {
                boostRuneEnchants(stack);
            } else {
                restoreRuneEnchants(stack);
            }
        }
    }

    private static void boostRuneEnchants(ItemStack stack) {
        if (stack.getOrDefault(ModDataComponents.RUNE_BOOSTED.get(), false)) {
            return;
        }
        // 1.21 没有 EnchantmentHelper.getEnchantments(ItemStack)，改为读对应的数据组件
        ItemEnchantments current = stack.get(EnchantmentHelper.getComponentType(stack));
        if (current == null || current.isEmpty()) {
            return;
        }
        int bonus = GodfallConfig.SHARD_RUNE_ENCHANT_LEVEL_BONUS.get();
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
        for (Holder<Enchantment> holder : current.keySet()) {
            mutable.set(holder, Math.min(current.getLevel(holder) + bonus, 255));
        }
        ItemEnchantments boosted = mutable.toImmutable();

        stack.set(ModDataComponents.RUNE_ORIGINAL_ENCHANTS.get(), current);
        stack.set(ModDataComponents.RUNE_BOOSTED.get(), true);
        EnchantmentHelper.setEnchantments(stack, boosted);
    }

    private static void restoreRuneEnchants(ItemStack stack) {
        if (!stack.getOrDefault(ModDataComponents.RUNE_BOOSTED.get(), false)) {
            return;
        }
        ItemEnchantments original = stack.get(ModDataComponents.RUNE_ORIGINAL_ENCHANTS.get());
        if (original != null) {
            EnchantmentHelper.setEnchantments(stack, original);
        }
        stack.remove(ModDataComponents.RUNE_BOOSTED.get());
        stack.remove(ModDataComponents.RUNE_ORIGINAL_ENCHANTS.get());
    }
}
