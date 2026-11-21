package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.item.custom.AdvancementTallyTrinketItem;
import com.lazybones.godfalltrinkets.item.custom.BrokenCoreItem;
import com.lazybones.godfalltrinkets.util.AdvancementHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 进度（成就）系统总事件：进度判定、隐藏挑战判定、功业徽记刷新与增益。
 *
 * <p>1.21 迁移要点：{@code TickEvent.PlayerTickEvent} → {@link PlayerTickEvent.Post}；
 * {@code LivingHurtEvent} → {@link LivingIncomingDamageEvent}；
 * {@code AdvancementEvent.AdvancementEarnEvent} → {@link AdvancementEvent.AdvanceEarnEvent}（取 {@code id()}）；
 * Curios 用 {@code Optional}，旧 {@code getArmor(int)} 改用 {@link Player#getItemBySlot}。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public class AdvancementTickHandler {

    private static int tickCounter = 0;

    /** 幽冥猎狩：本局击杀的 Boss */
    private static final Map<ServerPlayer, Set<ResourceLocation>> BOSS_KILL_TRACK = new WeakHashMap<>();
    public static final ResourceLocation BOSS_WITHER = ResourceLocation.parse("minecraft:wither");
    public static final ResourceLocation BOSS_DRAGON = ResourceLocation.parse("minecraft:ender_dragon");
    public static final ResourceLocation BOSS_WARDEN = ResourceLocation.parse("minecraft:warden");

    /** 厄难余生：八大诅咒同时激活的计时 */
    private static final Map<ServerPlayer, Integer> ALL_CURSE_TIMER = new WeakHashMap<>();
    private static final int REQUIRED_CURSE_TICKS = 288000; // 40 游戏分钟

    /** 裸身征伐：无盔甲击杀的 Boss */
    private static final Map<ServerPlayer, Set<ResourceLocation>> NO_ARMOR_BOSS = new WeakHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer sp)) {
            return;
        }
        ICuriosItemHandler handler = AdvancementHelper.getCurios(sp);

        // ① 破厄之核境界成就
        if (handler != null) {
            for (ICurioStacksHandler slotHandler : handler.getCurios().values()) {
                IDynamicStackHandler stacks = slotHandler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.is(ModItems.BROKEN_CORE.get())) {
                        AdvancementHelper.checkBrokenCoreTierAdvancements(stack, sp);
                    }
                }
            }
        }

        // ② 碎片集齐成就
        AdvancementHelper.checkShardAdvancements(sp);

        // ③ 锻刃前行：T3 及以上长剑（背包 + 饰品栏）
        for (ItemStack stack : sp.getInventory().items) {
            AdvancementHelper.checkSwordMidAdvancement(stack, sp);
        }
        if (handler != null) {
            for (ICurioStacksHandler slotHandler : handler.getCurios().values()) {
                IDynamicStackHandler stacks = slotHandler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    AdvancementHelper.checkSwordMidAdvancement(stacks.getStackInSlot(i), sp);
                }
            }
        }

        // ④ 神格觉醒
        AdvancementHelper.checkGodCoreAdvancement(sp);

        // ⑤ 厄难余生计时
        if (BrokenCoreItem.hasAllEightCursesActive(sp)) {
            int timer = ALL_CURSE_TIMER.getOrDefault(sp, 0) + 1;
            if (timer >= REQUIRED_CURSE_TICKS) {
                AdvancementHelper.grant(sp, AdvancementHelper.ADV_SECRET_CURSE_SURVIVOR);
            }
            ALL_CURSE_TIMER.put(sp, timer);
        } else {
            ALL_CURSE_TIMER.put(sp, 0);
        }

        // ⑥ 功业徽记每秒刷新
        if (++tickCounter >= 20) {
            tickCounter = 0;
            if (handler != null) {
                for (ICurioStacksHandler slotHandler : handler.getCurios().values()) {
                    IDynamicStackHandler stacks = slotHandler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        ItemStack stack = stacks.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof AdvancementTallyTrinketItem) {
                            AdvancementTallyTrinketItem.updateTallyTag(stack, sp);
                        }
                    }
                }
            }
            AdvancementTallyTrinketItem.applyTallyAttributes(sp,
                    AdvancementTallyTrinketItem.getEquippedAdvCount(sp));
        }
    }

    /** Boss 死亡相关隐藏成就 */
    @SubscribeEvent
    public static void onBossKilled(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());

        // 幽冥猎狩：同一条命内击杀三大 Boss
        if (BOSS_WITHER.equals(entityId) || BOSS_DRAGON.equals(entityId) || BOSS_WARDEN.equals(entityId)) {
            Set<ResourceLocation> killed = BOSS_KILL_TRACK.computeIfAbsent(sp, k -> new HashSet<>());
            killed.add(entityId);
            if (killed.contains(BOSS_WITHER) && killed.contains(BOSS_DRAGON) && killed.contains(BOSS_WARDEN)) {
                AdvancementHelper.grant(sp, AdvancementHelper.ADV_SECRET_BOSS_HUNTER);
                killed.clear();
            }
        }

        // 凡躯弑神 / 超限迸发：击杀监守者
        if (BOSS_WARDEN.equals(entityId)) {
            boolean foundTier1Core = false;
            ICuriosItemHandler handler = AdvancementHelper.getCurios(sp);
            if (handler != null) {
                outer:
                for (ICurioStacksHandler slotHandler : handler.getCurios().values()) {
                    IDynamicStackHandler stacks = slotHandler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        ItemStack stack = stacks.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof BrokenCoreItem core
                                && core.getTier(stack) == 1) {
                            foundTier1Core = true;
                            break outer;
                        }
                    }
                }
            }
            if (foundTier1Core) {
                AdvancementHelper.grant(sp, AdvancementHelper.ADV_SECRET_TIER1_VANQUISH);
            }
            if (AdvancementHelper.checkAll13ShardsEquipped(sp)) {
                AdvancementHelper.grant(sp, AdvancementHelper.ADV_SECRET_OVERLOAD_POWER);
            }
        }

        // 裸身征伐：无任何护甲击杀凋零 / 末影龙
        if (BOSS_WITHER.equals(entityId) || BOSS_DRAGON.equals(entityId)) {
            boolean noArmor = sp.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                    && sp.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                    && sp.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                    && sp.getItemBySlot(EquipmentSlot.FEET).isEmpty();
            if (noArmor) {
                Set<ResourceLocation> set = NO_ARMOR_BOSS.computeIfAbsent(sp, k -> new HashSet<>());
                set.add(entityId);
                if (set.contains(BOSS_WITHER) && set.contains(BOSS_DRAGON)) {
                    AdvancementHelper.grant(sp, AdvancementHelper.ADV_SECRET_NO_ARMOR_CHAMPION);
                    set.clear();
                }
            }
        }
    }

    /** 玩家死亡：清空内存临时记录 */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            clearPlayer(sp);
        }
    }

    /** 登出：清理内存，避免泄漏 */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            clearPlayer(sp);
        }
    }

    private static void clearPlayer(ServerPlayer sp) {
        BOSS_KILL_TRACK.remove(sp);
        ALL_CURSE_TIMER.remove(sp);
        NO_ARMOR_BOSS.remove(sp);
    }

    /** 任意隐藏成就完成 → 校验 7 个是否集齐，发放功业徽记 */
    @SubscribeEvent
    public static void onHiddenAdvComplete(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        ResourceLocation earnedId = event.getAdvancement().id();
        if (!AdvancementHelper.HIDDEN_TALLY_ADVS.contains(earnedId)) {
            return;
        }
        if (countItem(sp, ModItems.ADVANCEMENT_TALLY_TRINKET.get()) > 0) {
            return;
        }
        if (AdvancementHelper.hasCompletedAllHiddenTallyAdvancements(sp)) {
            ItemStack sigil = new ItemStack(ModItems.ADVANCEMENT_TALLY_TRINKET.get());
            sp.getInventory().add(sigil);
            sp.sendSystemMessage(Component.literal("§6【秘力觉醒】你完成了全部隐秘挑战，获得了功业徽记！"));
        }
    }

    private static int countItem(ServerPlayer sp, net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemStack stack : sp.getInventory().items) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : sp.getInventory().offhand) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** 功业徽记：攻击加成 */
    @SubscribeEvent
    public static void onPlayerDealDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        int count = AdvancementTallyTrinketItem.getEquippedAdvCount(sp);
        if (count <= 0) {
            return;
        }
        double multiplier = 1.0D + count * AdvancementTallyTrinketItem.DAMAGE_PER_ADV;
        event.setAmount((float) (event.getAmount() * multiplier));
    }

    /** 功业徽记：受到伤害减免 */
    @SubscribeEvent
    public static void onPlayerTakeDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        int count = AdvancementTallyTrinketItem.getEquippedAdvCount(player);
        if (count <= 0) {
            return;
        }
        float reduction = (float) AdvancementTallyTrinketItem.getDamageResistance(count);
        event.setAmount(event.getAmount() * (1.0F - reduction));
    }
}
