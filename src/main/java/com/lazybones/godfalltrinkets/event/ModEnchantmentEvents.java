package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.ModEnchantments;
import com.lazybones.godfalltrinkets.ModMobEffects;
import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.util.AdvancementHelper;
import com.lazybones.godfalltrinkets.util.CurioHelper;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 六个附魔的运行时逻辑（阶段四）。
 * <p>
 * 1.21 的附魔本身是数据包定义（见 data/godfall_trinkets/enchantment/），
 * 这里只负责“拿到等级之后干什么”，取等级改成 {@code stack.getEnchantments().getLevel(Holder)}。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public final class ModEnchantmentEvents {

    private static final String OATH_COOLDOWN_TAG = "immortal_oath_cooldown";
    private static final int OATH_COOLDOWN_TICKS = 1200;
    private static final String WRATH_LAST_HIT_TAG = "fallen_wrath_last_hit_tick";
    private static final int WRATH_LINGER_TICKS = 60;

    private ModEnchantmentEvents() {
    }

    private static int level(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment == null ? 0 : stack.getEnchantments().getLevel(enchantment);
    }

    /** 不朽誓约：每 tick 递减冷却 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag tag = player.getPersistentData();
        int cooldown = tag.getInt(OATH_COOLDOWN_TAG);
        if (cooldown > 0) {
            tag.putInt(OATH_COOLDOWN_TAG, cooldown - 1);
        }
    }

    /** 不朽誓约：HIGHEST 优先级，跑在原版不死图腾之前 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onImmortalOathRevive(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag tag = player.getPersistentData();
        if (tag.getInt(OATH_COOLDOWN_TAG) > 0) {
            return;
        }
        Holder<Enchantment> oath = ModEnchantments.get(player.level().registryAccess(),
                ModEnchantments.IMMORTAL_OATH);
        if (level(player.getInventory().getArmor(2), oath) < 1) {
            return;
        }

        event.setCanceled(true);
        player.setHealth(10.0F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.level().broadcastEntityEvent(player, (byte) 35);
        tag.putInt(OATH_COOLDOWN_TAG, OATH_COOLDOWN_TICKS);
    }

    /** 神陨之怒：脱战 3 秒清空怒层 */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity target) || target.level().isClientSide()) {
            return;
        }
        CompoundTag tag = target.getPersistentData();
        long lastHitTick = tag.getLong(WRATH_LAST_HIT_TAG);
        if (lastHitTick > 0 && target.level().getGameTime() - lastHitTick > WRATH_LINGER_TICKS) {
            if (target.hasEffect(ModMobEffects.FALLEN_WRATH_STACK)) {
                target.removeEffect(ModMobEffects.FALLEN_WRATH_STACK);
            }
        }
    }

    /** 附魔主逻辑：集中在一个伤害事件里，避免多次 setAmount 互相覆盖 */
    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        var provider = victim.level().registryAccess();
        ItemStack weapon = attacker.getMainHandItem();

        // 神陨庇护：只有佩戴破厄之核才生效，四件护甲等级和（上限 5）每级 -1% 承伤
        Holder<Enchantment> shelter = ModEnchantments.get(provider, ModEnchantments.GODFALL_SHELTER);
        if (attacker instanceof Player && victim instanceof Player hurtPlayer
                && CurioHelper.hasTrinket(hurtPlayer, ModItems.BROKEN_CORE.get())) {
            int shelterLevel = 0;
            for (int slot = 0; slot < 4; slot++) {
                shelterLevel += level(hurtPlayer.getInventory().getArmor(slot), shelter);
            }
            shelterLevel = Math.min(shelterLevel, 5);
            event.setAmount(event.getAmount() * (1.0F - shelterLevel * 0.01F));
        }

        // 碎核共鸣：等级 ×5% × 佩戴碎片数量
        int resonance = level(weapon, ModEnchantments.get(provider, ModEnchantments.SHARD_RESONANCE));
        if (resonance > 0 && attacker instanceof ServerPlayer serverAttacker) {
            int shardCount = AdvancementHelper.countEquippedShards(serverAttacker);
            event.setAmount(event.getAmount() * (1.0F + resonance * 0.05F * shardCount));
        }

        // 深渊凝视：目标血量 ≤30% 时挂深渊标记
        int gaze = level(weapon, ModEnchantments.get(provider, ModEnchantments.ABYSS_GAZE));
        if (gaze > 0 && victim.getMaxHealth() > 0.0F
                && victim.getHealth() / victim.getMaxHealth() <= 0.30F) {
            victim.addEffect(new MobEffectInstance(ModMobEffects.ABYSS_MARK, 100, gaze - 1, false, true));
        }

        // 神陨之怒：连续命中叠怒层，并刷新上次命中时间
        int fury = level(weapon, ModEnchantments.get(provider, ModEnchantments.FURY_OF_GODFALL));
        if (fury > 0) {
            victim.getPersistentData().putLong(WRATH_LAST_HIT_TAG, victim.level().getGameTime());
            MobEffectInstance wrath = victim.getEffect(ModMobEffects.FALLEN_WRATH_STACK);
            int amplifier = wrath == null ? 0 : Math.min(wrath.getAmplifier() + 1, 4);
            victim.addEffect(new MobEffectInstance(ModMobEffects.FALLEN_WRATH_STACK, 100, amplifier, false, true));
        }

        // 易伤结算：深渊标记每层 +15%，神陨怒层每层 +10%
        float damage = event.getAmount();
        MobEffectInstance abyssMark = victim.getEffect(ModMobEffects.ABYSS_MARK);
        if (abyssMark != null) {
            damage *= 1.0F + (abyssMark.getAmplifier() + 1) * 0.15F;
        }
        MobEffectInstance wrathStack = victim.getEffect(ModMobEffects.FALLEN_WRATH_STACK);
        if (wrathStack != null) {
            damage *= 1.0F + (wrathStack.getAmplifier() + 1) * 0.10F;
        }
        event.setAmount(damage);

        // 虚空汲取：每级按 8% 伤害吸血，不超过目标剩余血量
        int drain = level(weapon, ModEnchantments.get(provider, ModEnchantments.VOID_DRAIN));
        if (drain > 0 && !victim.level().isClientSide()) {
            float heal = Math.min(event.getAmount() * (drain * 0.08F), victim.getHealth());
            attacker.heal(heal);
        }
    }
}
