package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.custom.CurseSlotUnlockItem;
import com.lazybones.godfalltrinkets.item.custom.GodfallTrinketItem;
import com.lazybones.godfalltrinkets.util.RestoreGuard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.function.Consumer;

/**
 * 破厄之核槽位维护 + 饰品栏恢复保护。
 * <p>
 * 槽位规则（与 1.20.1 一致）：神位碎片槽裸配 0 格、护符槽裸配 2 格；
 * 戴上破厄之核 / 神格后才解锁到 13 格与 5 格（2 + 3）。摘下核 → 饰品收回背包、格数收回。
 * <p>
 * 为什么需要「恢复标记」：Curios 9 在玩家重进存档时会走
 * {@code OnDatapackSyncEvent → writeTag() → 所有槽位 setStackInSlot(EMPTY) 清空 → readTag() 还原}，
 * 死亡重生走 {@code PlayerEvent.Clone}。这两个阶段里破厄之核还没回到饰品栏，
 * 如果此时照常校验「必须戴核」就会把饰品判成非法退回背包。
 * 所以：HIGHEST 优先级先打标记并把槽位撑好 → Curios 自己的同步逻辑执行 → LOWEST 优先级解除标记。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public class TrinketSlotSyncEvents {

    private static final ResourceLocation SHARD_SLOT_ID = ResourceLocation.fromNamespaceAndPath(
            GodfallTrinkets.MOD_ID, "broken_core_shard_slots");
    private static final ResourceLocation AMULET_SLOT_ID = ResourceLocation.fromNamespaceAndPath(
            GodfallTrinkets.MOD_ID, "broken_core_amulet_slots");
    private static final ResourceLocation CURSE_SLOT_ID = ResourceLocation.fromNamespaceAndPath(
            GodfallTrinkets.MOD_ID, "curse_slot_unlock");
    /** 只用来触发 Curios 重算格数，amount 恒为 0，不参与格数计算 */
    private static final ResourceLocation SLOT_REFRESH_ID = ResourceLocation.fromNamespaceAndPath(
            GodfallTrinkets.MOD_ID, "slot_refresh");

    // ============================ 恢复标记 + 槽位校正入口 ============================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        RestoreGuard.begin(player);
        maintain(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClone(PlayerEvent.Clone event) {
        Player player = event.getEntity();
        RestoreGuard.begin(player);
        maintain(player);
    }

    /** 重进存档：Curios 在这个事件的 NORMAL 优先级里清空并还原饰品栏，所以我们必须抢在它前面 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDatapackSyncStart(OnDatapackSyncEvent event) {
        forEachPlayer(event, player -> {
            RestoreGuard.begin(player);
            maintain(player);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDatapackSyncEnd(OnDatapackSyncEvent event) {
        forEachPlayer(event, player -> {
            RestoreGuard.end(player);
            // 恢复期间是按「戴核」的宽度撑开的，恢复一结束必须立刻按真实状态收一次格子，
            // 否则没戴核的玩家会一直看到 13 / 5 格。
            maintain(player);
        });
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof Player player) {
            maintain(player);
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        RestoreGuard.tick(player);
        if (player.tickCount % 20 == 0) {
            maintain(player);
        }
    }


    // ============================ 具体逻辑 ============================

    private static void maintain(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        boolean hasCore = GodfallTrinketItem.wearingCore(player);
        boolean wide = hasCore || RestoreGuard.isRestoring(player);
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            // 没核 → 先把饰品（含 13 枚神位碎片）收回背包，再收格子。
            // 顺序不能反：Curios 缩格走 resize() → loseStacks()，会把末尾格子里的东西直接甩到地上。
            if (!wide) {
                ejectTrinkets(player, handler);
            }
            // 槽位格数必须来自存档里的 baseSize，绝不能靠修饰符临时加成：
            // 修饰符在「摘下核 → 存档 → 重新登录」这一轮里会丢，一丢槽位就变 0 格，
            // 13 枚碎片当场被判非法退回背包（这就是「摘核再戴回去后饰品全戴不上」的成因）。
            ensureSlots(player, handler);
        });
    }

    /**
     * 按「是否戴核」把各槽位的「基础格数」写进存档，并清掉旧版遗留的槽位修饰符。
     * <ul>
     * <li>god_shard_slot：裸配 0 → 戴核 13</li>
     * <li>charm：裸配 2 → 戴核 5（2 + 核解锁 3）</li>
     * <li>curse_transfer = 0/1：恶咒令牌解锁后才有 1 格（解锁标记在玩家 persistentData 里）</li>
     * </ul>
     * 恢复饰品栏期间一律按「戴核」的宽度撑开，否则 readTag 还原时碎片没有格子可放。
     */
    public static void ensureSlots(Player player, ICuriosItemHandler handler) {
        boolean wide = player != null
                && (GodfallTrinketItem.wearingCore(player) || RestoreGuard.isRestoring(player));
        migrateBaseSize(handler, "god_shard_slot", wide ? 13 : 0, SHARD_SLOT_ID);
        migrateBaseSize(handler, "charm", wide ? 5 : 2, AMULET_SLOT_ID);
        boolean curseUnlocked = player != null
                && player.getPersistentData().getBoolean(CurseSlotUnlockItem.TAG_UNLOCKED_CURSE_SLOT);
        migrateBaseSize(handler, "curse_transfer", curseUnlocked ? 1 : 0, CURSE_SLOT_ID);
    }

    /**
     * Curios 9 的槽位总数 = SavedBaseSize + 修饰符。
     * 旧存档里这两个槽位的 SavedBaseSize 是 0（当时槽位定义写的是 0），槽位格数全靠修饰符撑着；
     * 而修饰符在「读档 / 重进 / 死亡重生」这条路径上不能保证先于槽位校验生效，
     * 一旦那一刻算出 0 格，13 枚碎片就被判非法退回背包（重进存档全掉光）。
     *
     * 处理顺序很重要：先把 baseSize 抬到 13 / 5，让格数在任何时刻都不小于实际格数，
     * 再移除旧修饰符把格数收回 13 / 5 —— 中间不会出现「0 格」的空窗，饰品不会掉。
     */
    private static void migrateBaseSize(ICuriosItemHandler handler, String slot,
                                        int desired, ResourceLocation legacyModifierId) {
        handler.getStacksHandler(slot).ifPresent(stacks -> {
            int slotsNow = stacks.getSlots();
            // 缩格方向的安全阀：格子里还有东西就绝不动格数。
            // Curios 缩格走 resize() → loseStacks()，会把末尾格子里的东西直接甩到地上，
            // 而 ensureSlots 也会被「卸核」那条路径直接调到，那时饰品可能还戴着，
            // 所以这里直接返回，等 maintain() 把饰品收回背包后的下一轮再缩。
            if (desired < slotsNow && !isEmpty(stacks)) {
                return;
            }
            CompoundTag tag = stacks.serializeNBT();
            boolean dirty = false;
            if (tag.getInt("SavedBaseSize") != desired) {
                tag.putInt("SavedBaseSize", desired);
                dirty = true;
            }
            // 关键一：Curios 槽位定义 size=0 时 visible 也是 false，而 visible 存在 NBT 的 "Visible" 里。
            // 只抬 SavedBaseSize 不抬 Visible，结果就是「格数有 1 但整组槽位不显示」。
            boolean wantVisible = desired > 0;
            if (tag.getBoolean("Visible") != wantVisible) {
                tag.putBoolean("Visible", wantVisible);
                dirty = true;
            }
            // 关键二：Curios 格数是惰性重算的（update() 要有标记才会跑 resize()），
            // 光改 baseSize 不会让底层 Stacks 真的长出格子 —— 存档里会出现 SavedBaseSize=1 但 Stacks.Size=0，
            // 而 CuriosContainer 是按底层 Stacks 的格数画格子的，于是「解锁了却一个格子都看不到」。
            // 所以判脏时补一个 amount=0 的永久修饰符去 flagUpdate，逼 update() 走一次 resize()。
            if (dirty) {
                stacks.deserializeNBT(tag);
            }
            if (dirty || stacks.getStacks().getSlots() != desired) {
                handler.addPermanentSlotModifier(slot, SLOT_REFRESH_ID, 0.0,
                        AttributeModifier.Operation.ADD_VALUE);
                // getSlots() 是 Curios 的惰性重算入口：它内部调 update() → resize()，
                // resize() 会同时把 Stacks / Cosmetics / Renders / ActiveStates 四张表一起对齐。
                stacks.getSlots();
                // 兜底：万一这次重算没生效，直接把底层格数补齐，至少让格子画得出来。
                int current = stacks.getStacks().getSlots();
                if (current < desired) {
                    stacks.getStacks().grow(desired - current);
                    stacks.getCosmeticStacks().grow(desired - current);
                }
            }
            // 旧版用 +13 / +3 的槽位修饰符，和 baseSize 叠加会翻倍，最后再清掉
            boolean hasLegacy = stacks.getModifiers().containsKey(legacyModifierId)
                    || stacks.getPermanentModifiers().stream()
                    .anyMatch(m -> m.id().equals(legacyModifierId));
            if (hasLegacy) {
                handler.removeSlotModifier(slot, legacyModifierId);
            }
        });
    }

    /** 该槽位的格子里是否还装着东西 */
    private static boolean isEmpty(ICurioStacksHandler stacks) {
        var handler = stacks.getStacks();
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 没有破厄之核时，把饰品栏里的饰品退回背包（不直接掉落） */
    public static void ejectTrinkets(Player player, ICuriosItemHandler handler) {
        for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {
            var stacks = stacksHandler.getStacks();
            for (int i = stacksHandler.getSlots() - 1; i >= 0; i--) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof GodfallTrinketItem) {
                    player.getInventory().placeItemBackInInventory(stack.copy());
                    stacks.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
    }

    private static void forEachPlayer(OnDatapackSyncEvent event, Consumer<ServerPlayer> action) {
        ServerPlayer single = event.getPlayer();
        if (single != null) {
            action.accept(single);
            return;
        }
        for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            action.accept(player);
        }
    }
}
