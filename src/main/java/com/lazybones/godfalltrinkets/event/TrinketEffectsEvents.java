package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.ModAttachments;
import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.util.CurioHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 饰品战斗/成长效果（阶段二）。
 * <p>
 * 只搬 1.20.1 里已经存在于当前项目的那几件饰品：烬肤镯、濒生戒、渊纹铭刻戒、龙核之心。
 * 其余效果依赖还没迁移的物品，等阶段三搬完物品再往这里追加。
 * <p>
 * 与 1.20.1 的差异：
 * 1. 1.21 的受伤事件是 {@link LivingIncomingDamageEvent}（旧版 LivingHurtEvent），
 *    金额还是 getAmount/setAmount，但多了 {@code setInvulnerabilityTicks}；
 * 2. {@code player.getPersistentData()} 已移除，冷却改用 {@link ModAttachments#DEATH_DEFY_COOLDOWN}；
 * 3. {@code hasTrinket} 走 1.21 的 {@code CuriosApi.getCuriosInventory(...).map(...)}；
 * 4. 1.20.1 里这些判断分散在 TrinketEffectsEvents 与 EndTrinketEffectEvents 两个类，
 *    现在合并到一个受伤事件里，避免两个处理器互相覆盖 setAmount。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public final class TrinketEffectsEvents {

    private static final float DEATH_DEFY_HEALTH_THRESHOLD = 0.35f;
    private static final long DEATH_DEFY_COOLDOWN_TICKS = 45L * 20L;
    private static final int DEATH_DEFY_INVULN_TICKS = 60;
    private static final java.util.Random RANDOM = new java.util.Random();
    private static final float DEATH_DEFY_DAMAGE_REDUCTION = 0.72f;

    private TrinketEffectsEvents() {
    }

    // ======================== 受伤 ========================

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // ---- 玩家作为受害者 ----
        if (event.getEntity() instanceof Player player) {
            handleDefensive(player, event);
        }

        // ---- 玩家作为攻击者 ----
        if (event.getSource().getEntity() instanceof Player attacker
                && attacker != event.getEntity()) {
            event.setAmount(applyOffensive(attacker, event.getEntity(), event.getAmount()));
        }
    }

    private static void handleDefensive(Player player, LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();

        // 烬肤镯：火焰伤害完全免疫 + 中毒/凋零类伤害减半
        if (CurioHelper.hasTrinket(player, ModItems.EMBER_BRACELET.get())) {
            if (isFireDamage(source)) {
                event.setCanceled(true);
                return;
            }
            if (source.is(DamageTypes.MAGIC)
                    || source.is(DamageTypes.WITHER)
                    || source.is(DamageTypes.INDIRECT_MAGIC)) {
                event.setAmount(event.getAmount() * 0.5f);
            }
        }

        // 龙核之心：免疫龙息与末影龙冲撞
        if (CurioHelper.hasTrinket(player, ModItems.DRAGON_CORE_HEART.get())
                && (source.is(DamageTypes.DRAGON_BREATH) || source.getEntity() instanceof EnderDragon)) {
            event.setCanceled(true);
            return;
        }

        // 濒生戒：残血减伤 / 濒死无敌
        if (CurioHelper.hasTrinket(player, ModItems.DEATH_DEFY_RING.get())) {
            float healthRatio = player.getMaxHealth() > 0.0f
                    ? player.getHealth() / player.getMaxHealth()
                    : 1.0f;
            if (healthRatio <= DEATH_DEFY_HEALTH_THRESHOLD) {
                long now = player.level().getGameTime();
                long last = player.getData(ModAttachments.DEATH_DEFY_COOLDOWN);
                if (now - last > DEATH_DEFY_COOLDOWN_TICKS) {
                    // 冷却就绪：本次伤害清零 + 3 秒无敌
                    player.setData(ModAttachments.DEATH_DEFY_COOLDOWN, now);
                    event.setAmount(0.0f);
                    event.setInvulnerabilityTicks(DEATH_DEFY_INVULN_TICKS);
                    return;
                }
                event.setAmount(event.getAmount() * DEATH_DEFY_DAMAGE_REDUCTION);
            }
        }

        // 壁垒腕：爆炸减伤 22%；举盾时免疫投射物（投射物另有 ProjectileImpactEvent 拦截）
        if (CurioHelper.hasTrinket(player, ModItems.BARRIER_BRACELET.get())) {
            if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
                event.setAmount(event.getAmount() * 0.78f);
            }
            if (player.isBlocking() && source.getDirectEntity() instanceof Projectile) {
                event.setCanceled(true);
                return;
            }
        }

        // 生纹护符：受击立刻回 2 点
        if (CurioHelper.hasTrinket(player, ModItems.VITAL_AMULET.get()) && event.getAmount() > 0) {
            player.heal(2.0f);
        }
    }

    private static float applyOffensive(Player attacker, LivingEntity victim, float damage) {
        // 渊纹铭刻戒：造成的伤害 +8%
        if (CurioHelper.hasTrinket(attacker, ModItems.ABYSS_RUNE_RING.get())) {
            damage *= 1.08f;
        }

        // 龙核之心：对末影龙 / 凋灵 / 监守者 / 末地生物 +40%
        if (CurioHelper.hasTrinket(attacker, ModItems.DRAGON_CORE_HEART.get())
                && isBossOrEndMob(victim)) {
            damage *= 1.4f;
        }

        // 噬魂血戒：命中偷取 15% 伤害
        if (CurioHelper.hasTrinket(attacker, ModItems.SOUL_DRAIN_RING.get())) {
            attacker.heal(damage * 0.15f);
        }

        // 追猎戒：目标血量高于 50% 时 +16%
        if (CurioHelper.hasTrinket(attacker, ModItems.HUNTER_RING.get())
                && victim.getMaxHealth() > 0
                && victim.getHealth() / victim.getMaxHealth() > 0.5f) {
            damage *= 1.16f;
        }

        return damage;
    }

    // ======================== 经验 ========================

    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        int amount = event.getAmount();
        if (amount > 0 && CurioHelper.hasTrinket(player, ModItems.ABYSS_RUNE_RING.get())) {
            event.setAmount((int) (amount * 1.12f));
        }
        // 熔渊锻符：损失经验 -25%
        if (amount < 0 && CurioHelper.hasTrinket(player, ModItems.FORGE_AMULET.get())) {
            event.setAmount((int) (amount * 0.75f));
        }
    }

    // ======================== 击杀 ========================

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        // 濒生戒：低血量击杀额外回复 4 点生命
        if (CurioHelper.hasTrinket(player, ModItems.DEATH_DEFY_RING.get())
                && player.getMaxHealth() > 0.0f
                && player.getHealth() / player.getMaxHealth() < 0.35f) {
            player.heal(4.0f);
        }
        // 噬魂血戒 / 追猎戒：击杀回血
        if (CurioHelper.hasTrinket(player, ModItems.SOUL_DRAIN_RING.get())) {
            player.heal(3.0f);
        }
        if (CurioHelper.hasTrinket(player, ModItems.HUNTER_RING.get())) {
            player.heal(3.5f);
        }
    }

    // ======================== 每秒被动 ========================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        if (player.tickCount % 20 == 0) {
            tickCleanse(player);
            tickVital(player);
            tickShockAmulet(player);
            tickDeepRock(player);
            tickProsperityGrowth(player);
        }

        // 壁垒腕：举盾每 2 秒回 0.8
        if (player.isBlocking()
                && CurioHelper.hasTrinket(player, ModItems.BARRIER_BRACELET.get())
                && player.tickCount % 40 == 0) {
            player.heal(0.8f);
        }
    }

    private static void tickCleanse(Player player) {
        if (!CurioHelper.hasTrinket(player, ModItems.PURIFYING_BRACELET.get())) {
            return;
        }
        for (var instance : player.getActiveEffects()) {
            if (!instance.getEffect().value().isBeneficial()) {
                player.removeEffect(instance.getEffect());
                break;
            }
        }
    }

    private static void tickVital(Player player) {
        if (CurioHelper.hasTrinket(player, ModItems.VITAL_AMULET.get())) {
            player.heal(3.0f);
        }
    }

    private static void tickShockAmulet(Player player) {
        // 震斥护符：10 格内每只敌怪回 1 点血（上限 6）
        if (!CurioHelper.hasTrinket(player, ModItems.SHOCK_AMULET.get())) {
            return;
        }
        AABB box = new AABB(player.blockPosition()).inflate(10);
        int count = (int) player.level().getEntitiesOfClass(Mob.class, box,
                mob -> mob.isAlive() && mob.getTarget() != null).size();
        if (count > 0) {
            player.heal(Math.min(count, 6));
        }
    }

    private static void tickDeepRock(Player player) {
        // 深岩寻矿戒：免疫挖掘疲劳
        if (CurioHelper.hasTrinket(player, ModItems.DEEP_ROCK_RING.get())) {
            player.removeEffect(MobEffects.DIG_SLOWDOWN);
        }
    }

    private static void tickProsperityGrowth(Player player) {
        // 繁生腕镯：10 格内幼崽生长加速
        if (!CurioHelper.hasTrinket(player, ModItems.PROSPERITY_BRACELET.get())) {
            return;
        }
        AABB box = new AABB(player.blockPosition()).inflate(10);
        for (AgeableMob mob : player.level().getEntitiesOfClass(AgeableMob.class, box,
                ageable -> ageable.isBaby() && ageable.isAlive())) {
            mob.setAge(mob.getAge() + 1);
        }
    }

    // ======================== 投射物 / 作物 / 挖矿 / 掉落 ========================

    /** 壁垒腕：举盾时投射物不再命中玩家，反弹 30% 并销毁 */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof Player player)
                || player.level().isClientSide()) {
            return;
        }
        if (!player.isBlocking()
                || !CurioHelper.hasTrinket(player, ModItems.BARRIER_BRACELET.get())) {
            return;
        }
        event.setCanceled(true);
        Projectile projectile = event.getProjectile();
        if (projectile.getOwner() instanceof LivingEntity owner) {
            owner.hurt(player.damageSources().thorns(player),
                    0.3f * (float) projectile.getDeltaMovement().length());
        }
        projectile.discard();
    }

    /** 沃壤芽镯：8 格内作物额外随机生长一次 */
    @SubscribeEvent
    public static void onCropGrow(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (Player player : level.players()) {
            if (player.distanceToSqr(event.getPos().getCenter()) < 64
                    && CurioHelper.hasTrinket(player, ModItems.FERTILE_BRACELET.get())) {
                level.getBlockState(event.getPos())
                        .randomTick(level, event.getPos(), level.random);
                break;
            }
        }
    }

    /** 深岩寻矿戒：25% 概率双倍原矿 */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()
                || !CurioHelper.hasTrinket(player, ModItems.DEEP_ROCK_RING.get())) {
            return;
        }
        BlockState state = event.getState();
        if (!(state.is(BlockTags.COAL_ORES) || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES)
                || state.is(BlockTags.REDSTONE_ORES) || state.is(BlockTags.LAPIS_ORES))) {
            return;
        }
        if (RANDOM.nextFloat() >= 0.25f) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        var blockEntity = level.getBlockEntity(event.getPos());
        for (ItemStack drop : Block.getDrops(state, level, event.getPos(), blockEntity,
                player, player.getMainHandItem())) {
            Block.popResource(level, event.getPos(), drop);
        }
    }

    /** 繁生腕镯：宰杀动物 20% 双倍掉落 */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || !(event.getEntity() instanceof Animal)
                || !CurioHelper.hasTrinket(player, ModItems.PROSPERITY_BRACELET.get())
                || RANDOM.nextFloat() >= 0.2f) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        for (ItemEntity drop : event.getDrops()) {
            level.addFreshEntity(new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(),
                    drop.getItem().copy()));
        }
    }

    /** 繁生腕镯：繁殖冷却 -25% */
    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        Player player = event.getCausedByPlayer();
        if (player == null
                || !CurioHelper.hasTrinket(player, ModItems.PROSPERITY_BRACELET.get())) {
            return;
        }
        if (event.getParentA() instanceof Animal parentA) {
            parentA.setAge(Math.max(0, parentA.getAge() + 3000));
        }
        if (event.getParentB() instanceof Animal parentB) {
            parentB.setAge(Math.max(0, parentB.getAge() + 3000));
        }
    }

    /** 深岩寻矿戒：阻止挖掘疲劳被施加 */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance() == null
                || event.getEffectInstance().getEffect() != MobEffects.DIG_SLOWDOWN
                || !(event.getEntity() instanceof Player player)
                || !CurioHelper.hasTrinket(player, ModItems.DEEP_ROCK_RING.get())) {
            return;
        }
        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    // ======================== 工具 ========================

    private static boolean isFireDamage(DamageSource source) {
        return source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.HOT_FLOOR);
    }

    private static boolean isBossOrEndMob(LivingEntity entity) {
        return entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity instanceof Warden
                || entity instanceof EnderMan
                || entity instanceof Shulker;
    }
}
