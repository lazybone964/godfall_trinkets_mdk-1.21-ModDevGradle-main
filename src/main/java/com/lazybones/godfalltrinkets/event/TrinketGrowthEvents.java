package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.item.custom.BrokenCoreItem;
import com.lazybones.godfalltrinkets.item.custom.CurseTransferTrinketItem;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 破厄之核的成长与诅咒系统（1.20.1 TrinketGrowthEvents 的 1.21 版）。
 * <p>
 * 与旧版差异：
 * 1. {@code TickEvent.PlayerTickEvent} → {@link PlayerTickEvent.Post}；
 * 2. {@code LivingHurtEvent} → {@link LivingIncomingDamageEvent}；
 * 3. {@code PlayerSleepInBedEvent} 在 NeoForge 1.21 换成了 {@link CanPlayerSleepEvent}，
 *    用 {@code setProblem(BedSleepingProblem)} 拦截入睡；
 * 4. 「没戴核就收回饰品」不再由本类负责，统一交给 {@link TrinketSlotSyncEvents}
 *    （旧版这里和 Curios 内部同步互相打架，会把人刚戴上的饰品弹回背包）。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public final class TrinketGrowthEvents {

    private static final Random RANDOM = new Random();

    private TrinketGrowthEvents() {
    }

    public record CoreInfo(ItemStack stack, BrokenCoreItem item, int tier) {
    }

    public static Optional<CoreInfo> findCore(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(
                        stack -> stack.getItem() instanceof BrokenCoreItem))
                .map(result -> {
                    ItemStack stack = result.stack();
                    BrokenCoreItem item = (BrokenCoreItem) stack.getItem();
                    return new CoreInfo(stack, item, item.getTier(stack));
                });
    }

    private static boolean isHarmfulEffect(MobEffectInstance effect) {
        Holder<MobEffect> id = effect.getEffect();
        return id == MobEffects.POISON
                || id == MobEffects.WITHER
                || id == MobEffects.WEAKNESS
                || id == MobEffects.DARKNESS
                || id == MobEffects.HUNGER
                || id == MobEffects.LEVITATION
                || id == MobEffects.BLINDNESS
                || id == MobEffects.MOVEMENT_SLOWDOWN
                || id == MobEffects.DIG_SLOWDOWN
                || id == MobEffects.CONFUSION;
    }

    // ==================== Tick：饥饿 / 负面药效诅咒 ====================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        findCore(player).ifPresent(core -> {
            applyHungerCurse(player, core);
            applyDebuffCurse(player, core);
        });
    }

    private static void applyHungerCurse(Player player, CoreInfo core) {
        if (core.tier() >= 9) {
            return;
        }
        if (CurseTransferTrinketItem.isAbsorbingCurse(player, "hunger_curse")) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            player.causeFoodExhaustion(0.2f);
            if (player.getFoodData().getFoodLevel() <= 0) {
                player.giveExperiencePoints(-1);
            }
        }
    }

    /** 玩家 UUID → 已经处理过的负面效果，避免每 20 tick 无限翻倍 */
    private static final Map<UUID, Set<Holder<MobEffect>>> PLAYER_PROCESSED_EFFECTS = new HashMap<>();

    private static void applyDebuffCurse(Player player, CoreInfo core) {
        if (CurseTransferTrinketItem.isAbsorbingCurse(player, "foul_body")) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        boolean rev = core.tier() >= 9;
        Set<Holder<MobEffect>> processed = PLAYER_PROCESSED_EFFECTS
                .computeIfAbsent(player.getUUID(), k -> new HashSet<>());

        List<MobEffectInstance> effects = new ArrayList<>(player.getActiveEffects());
        for (MobEffectInstance inst : effects) {
            Holder<MobEffect> id = inst.getEffect();
            if (!isHarmfulEffect(inst) || processed.contains(id)) {
                continue;
            }
            int orig = inst.getDuration();
            if (orig <= 20) {
                continue;
            }
            int newDuration = rev ? Math.max(20, orig / 2) : Math.min(orig * 2, 24000);
            if (newDuration == orig) {
                continue;
            }
            int amp = inst.getAmplifier();
            boolean ambient = inst.isAmbient();
            boolean visible = inst.isVisible();
            boolean icon = inst.showIcon();

            player.removeEffect(id);
            player.forceAddEffect(
                    new MobEffectInstance(id, newDuration, amp, ambient, visible, icon), player);
            processed.add(id);
        }

        processed.removeIf(id -> !player.hasEffect(id));
        PLAYER_PROCESSED_EFFECTS.keySet().removeIf(uuid ->
                player.level().getPlayerByUUID(uuid) == null && !uuid.equals(player.getUUID()));
    }

    // ==================== 受伤 ====================

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();

        if (victim instanceof Player player) {
            findCore(player).ifPresent(core -> {
                float damage = event.getAmount();
                damage = applyShield(core, damage);
                damage = applyDodge(core, player, damage, source);
                damage = applyDeepWoundAbsorbed(core, player, damage);
                damage = applyScarred(core, player, damage);
                if (damage <= 0) {
                    event.setCanceled(true);
                }
                event.setAmount(Math.max(0, damage));
            });
        }

        if (source.getEntity() instanceof Player attacker && attacker != victim) {
            findCore(attacker).ifPresent(core -> {
                float damage = applyCalamity(core, attacker, event.getAmount());
                event.setAmount(Math.max(0, damage));
            });
        }
    }

    private static float applyShield(CoreInfo core, float damage) {
        if (!core.item().isShieldActive(core.stack())) {
            return damage;
        }
        return core.item().absorbDamage(core.stack(), damage, core.tier());
    }

    private static float applyDodge(CoreInfo core, Player player, float damage, DamageSource source) {
        if (RANDOM.nextFloat() >= BrokenCoreItem.DODGE_CHANCE[core.tier()]) {
            return damage;
        }
        float reflect = BrokenCoreItem.DODGE_REFLECT[core.tier()];
        if (reflect > 0 && source.getEntity() instanceof LivingEntity attacker) {
            attacker.hurt(player.damageSources().thorns(player), damage * reflect);
        }
        return 0;
    }

    private static float applyDeepWoundAbsorbed(CoreInfo core, Player player, float damage) {
        if (CurseTransferTrinketItem.isAbsorbingCurse(player, "deep_wound")) {
            return damage;
        }
        return (float) (damage * BrokenCoreItem.getDeepWoundMultiplier(core.tier()));
    }

    private static float applyScarred(CoreInfo core, Player player, float damage) {
        if (CurseTransferTrinketItem.isAbsorbingCurse(player, "scarred")) {
            return damage;
        }
        if (core.tier() >= 9) {
            return Math.min(damage, player.getMaxHealth() * 0.3f);
        }
        return damage + (float) (player.getMaxHealth()
                * BrokenCoreItem.getScarredRatio(core.tier()));
    }

    private static float applyCalamity(CoreInfo core, Player player, float damage) {
        int count = countNearbyEnemies(player);
        float bonus = BrokenCoreItem.CALAMITY_RATE[core.tier()];
        float cap = BrokenCoreItem.CALAMITY_CAP[core.tier()];
        return damage * (1.0f + Math.min(count * bonus, cap));
    }

    private static int countNearbyEnemies(Player player) {
        AABB box = new AABB(player.blockPosition()).inflate(10);
        return player.level().getEntitiesOfClass(Mob.class, box,
                mob -> mob.isAlive() && mob.getTarget() != null).size();
    }

    // ==================== 击杀 ====================

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof Player player)) {
            return;
        }
        LivingEntity victim = event.getEntity();

        findCore(player).ifPresent(core -> {
            int exp = Math.max(5, (int) victim.getMaxHealth());
            exp = (int) (exp * (1.0f + core.tier() * 0.1f + core.tier() * core.tier() * 0.01f));
            if (core.tier() >= 6) {
                exp = (int) (exp * (1.0f + 0.1f * core.tier()));
            }
            core.item().addExp(core.stack(), exp, player);

            applySoulDrain(player, core);
            applyShatter(player, core, victim);
            dropGodFragments(core, victim);
        });
    }

    private static void applySoulDrain(Player player, CoreInfo core) {
        float[] data = BrokenCoreItem.SOUL_DRAIN[core.tier()];
        player.heal(data[0] * 0.5f);
        player.getFoodData().eat(1, data[1] * 0.5f);
        int repair = (int) data[2];
        if (repair > 0) {
            ItemStack held = player.getMainHandItem();
            if (held.isDamageableItem()) {
                held.setDamageValue(Math.max(0, held.getDamageValue() - repair));
            }
        }
    }

    private static void applyShatter(Player player, CoreInfo core, LivingEntity victim) {
        int tier = core.tier();
        if (tier < 4) {
            return;
        }
        float baseDamage = victim.getMaxHealth();
        float ratio;
        int range;
        boolean ignite = false;
        if (tier >= 9) {
            ratio = 0.70f;
            range = 6;
            ignite = true;
        } else if (tier >= 6) {
            ratio = 0.45f;
            range = 4;
        } else {
            ratio = 0.30f;
            range = 3;
        }

        AABB box = new AABB(victim.blockPosition()).inflate(range);
        for (LivingEntity target : victim.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != victim && e instanceof Mob && e.isAlive())) {
            target.hurt(player.damageSources().playerAttack(player), baseDamage * ratio);
            if (ignite) {
                target.igniteForSeconds(3);
            }
            if (tier >= 6 && RANDOM.nextFloat() < 0.25f) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
            }
        }
        if (tier >= 7) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED, 100, 0, false, false, true));
            player.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SPEED, 100, 0, false, false, true));
        }
    }

    private static void dropGodFragments(CoreInfo core, LivingEntity victim) {
        int tier = core.tier();
        int count;
        if (tier >= 9) {
            count = 4;
        } else if (tier >= 7) {
            count = 3;
        } else if (tier >= 4) {
            count = 2;
        } else {
            count = 1;
        }
        if (RANDOM.nextInt(100) < 10 + tier * 5) {
            count++;
        }
        victim.spawnAtLocation(new ItemStack(ModItems.GODFRAGMENT.get(), count));
    }

    // ==================== 睡眠 ====================

    @SubscribeEvent
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        findCore(player).ifPresent(core -> {
            if (core.tier() < 9
                    && !CurseTransferTrinketItem.isAbsorbingCurse(player, "sleepless")) {
                player.displayClientMessage(
                        Component.translatable("message.godfall_trinkets.cant_sleep"), true);
                event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            }
        });
    }
}
