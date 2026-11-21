package com.lazybones.godfalltrinkets.util;

import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.item.custom.BrokenCoreItem;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 进度（成就）相关工具。
 *
 * <p>1.21 迁移要点：
 * <ol>
 *   <li>{@code ServerAdvancementManager#getAdvancement} → {@link net.minecraft.server.ServerAdvancementManager#get}，
 *       返回 {@link AdvancementHolder}（不只是 {@code Advancement}）；</li>
 *   <li>{@code CuriosApi.getCuriosInventory} 返回 {@link Optional}，没有 LazyOptional/resolve；</li>
 *   <li>{@code ItemStack.getTag()} → {@link ItemNbt}（CUSTOM_DATA 组件）。</li>
 * </ol>
 */
public final class AdvancementHelper {

    // -------------------- 原有进度 ID 常量 --------------------
    public static final ResourceLocation ADV_TWELVE_CONSTELLATION = ResourceLocation.parse("godfall_trinkets:twelve_constellation");
    public static final ResourceLocation ADV_THIRTEEN_RETURN = ResourceLocation.parse("godfall_trinkets:thirteen_return");
    public static final ResourceLocation ADV_TIER9_REVERSE = ResourceLocation.parse("godfall_trinkets:tier9_reverse");
    public static final ResourceLocation ADV_SWORD_MID_FORGE = ResourceLocation.parse("godfall_trinkets:sword_mid_forge");
    public static final ResourceLocation ADV_HARDSHIP_TEMPER = ResourceLocation.parse("godfall_trinkets:hardship_temper");
    public static final ResourceLocation ADV_CHEST_FIND_SHARD = ResourceLocation.parse("godfall_trinkets:chest_find_shard");
    public static final ResourceLocation ADV_GODHOOD_AWAKEN = ResourceLocation.parse("godfall_trinkets:godhood_awaken");

    // 5 个功业徽记条件的隐藏成就
    public static final ResourceLocation ADV_SECRET_CURSE_SURVIVOR = ResourceLocation.parse("godfall_trinkets:secret_curse_survivor");
    public static final ResourceLocation ADV_SECRET_TIER1_VANQUISH = ResourceLocation.parse("godfall_trinkets:secret_tier1_vanquish");
    public static final ResourceLocation ADV_SECRET_NO_ARMOR_CHAMPION = ResourceLocation.parse("godfall_trinkets:secret_no_armor_champion");
    public static final ResourceLocation ADV_SECRET_BOSS_HUNTER = ResourceLocation.parse("godfall_trinkets:secret_boss_hunter");
    public static final ResourceLocation ADV_SECRET_OVERLOAD_POWER = ResourceLocation.parse("godfall_trinkets:secret_overload_power");

    /** 全部 7 个隐藏成就，全部完成才发放功业徽记 */
    public static final List<ResourceLocation> HIDDEN_TALLY_ADVS = List.of(
            ADV_HARDSHIP_TEMPER,
            ADV_CHEST_FIND_SHARD,
            ADV_SECRET_CURSE_SURVIVOR,
            ADV_SECRET_TIER1_VANQUISH,
            ADV_SECRET_NO_ARMOR_CHAMPION,
            ADV_SECRET_BOSS_HUNTER,
            ADV_SECRET_OVERLOAD_POWER);

    private AdvancementHelper() {
    }

    /** 玩家是否完成全部 7 个隐藏挑战成就 */
    public static boolean hasCompletedAllHiddenTallyAdvancements(ServerPlayer sp) {
        for (ResourceLocation rl : HIDDEN_TALLY_ADVS) {
            AdvancementHolder holder = getAdvancement(sp, rl);
            if (holder == null) {
                return false;
            }
            if (!sp.getAdvancements().getOrStartProgress(holder).isDone()) {
                return false;
            }
        }
        return true;
    }

    /** god_shard_slot 槽是否同时佩戴全部 13 枚神位碎片 */
    public static boolean checkAll13ShardsEquipped(ServerPlayer sp) {
        ICuriosItemHandler handler = getCurios(sp);
        if (handler == null) {
            return false;
        }
        ICurioStacksHandler slot = handler.getCurios().get("god_shard_slot");
        if (slot == null) {
            return false;
        }
        IDynamicStackHandler stacks = slot.getStacks();
        for (int i = 0; i < 13; i++) {
            if (stacks.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 取玩家的 Curios 容器（不存在返回 null） */
    public static ICuriosItemHandler getCurios(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player).orElse(null);
    }

    /** 按 ID 取进度（1.21 返回 AdvancementHolder） */
    public static AdvancementHolder getAdvancement(ServerPlayer player, ResourceLocation advId) {
        return player.getServer() == null ? null : player.getServer().getAdvancements().get(advId);
    }

    /** 授予进度（找不到进度就静默跳过） */
    public static void grant(ServerPlayer player, ResourceLocation advId) {
        AdvancementHolder holder = getAdvancement(player, advId);
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            for (String criterion : player.getAdvancements().getOrStartProgress(holder).getRemainingCriteria()) {
                player.getAdvancements().award(holder, criterion);
            }
        }
    }

    /** 撤销进度（调试用） */
    public static void revoke(ServerPlayer player, ResourceLocation advId) {
        AdvancementHolder holder = getAdvancement(player, advId);
        if (holder != null) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
            for (String criterion : progress.getCompletedCriteria()) {
                player.getAdvancements().revoke(holder, criterion);
            }
        }
    }

    /** 背包 + Curios 全部槽位里拥有的神位碎片 */
    public static Set<Item> getOwnedShards(ServerPlayer player) {
        Set<Item> set = new HashSet<>();
        for (ItemStack stack : player.getInventory().items) {
            addShardItem(set, stack);
        }
        ICuriosItemHandler handler = getCurios(player);
        if (handler != null) {
            for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                IDynamicStackHandler stackHandler = entry.getValue().getStacks();
                for (int i = 0; i < stackHandler.getSlots(); i++) {
                    addShardItem(set, stackHandler.getStackInSlot(i));
                }
            }
        }
        return set;
    }

    /** 神格觉醒：背包或任意 Curios 槽存在 god_core 即解锁 */
    public static void checkGodCoreAdvancement(ServerPlayer player) {
        boolean found = false;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.GOD_CORE.get())) {
                found = true;
                break;
            }
        }
        if (!found) {
            ICuriosItemHandler handler = getCurios(player);
            if (handler != null) {
                for (ICurioStacksHandler slotHandler : handler.getCurios().values()) {
                    IDynamicStackHandler stackHandler = slotHandler.getStacks();
                    for (int i = 0; i < stackHandler.getSlots(); i++) {
                        if (stackHandler.getStackInSlot(i).is(ModItems.GOD_CORE.get())) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                }
            }
        }
        if (found) {
            grant(player, ADV_GODHOOD_AWAKEN);
        }
    }

    private static void addShardItem(Set<Item> set, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        Item item = stack.getItem();
        if (item == ModItems.SHARD_SKY.get()
                || item == ModItems.SHARD_SUN.get()
                || item == ModItems.SHARD_LIFE.get()
                || item == ModItems.SHARD_STRENGTH.get()
                || item == ModItems.SHARD_FATE.get()
                || item == ModItems.SHARD_WIND.get()
                || item == ModItems.SHARD_SPIRIT.get()
                || item == ModItems.SHARD_BARRIER.get()
                || item == ModItems.SHARD_VOID.get()
                || item == ModItems.SHARD_SHADOW.get()
                || item == ModItems.SHARD_THUNDER.get()
                || item == ModItems.SHARD_STAR.get()
                || item == ModItems.SHARD_RUNE.get()) {
            set.add(item);
        }
    }

    /** 十二星罗 / 十三归位 */
    public static void checkShardAdvancements(ServerPlayer player) {
        Set<Item> owned = getOwnedShards(player);
        boolean hasTwelve = owned.contains(ModItems.SHARD_SUN.get())
                && owned.contains(ModItems.SHARD_LIFE.get())
                && owned.contains(ModItems.SHARD_STRENGTH.get())
                && owned.contains(ModItems.SHARD_FATE.get())
                && owned.contains(ModItems.SHARD_WIND.get())
                && owned.contains(ModItems.SHARD_SPIRIT.get())
                && owned.contains(ModItems.SHARD_BARRIER.get())
                && owned.contains(ModItems.SHARD_VOID.get())
                && owned.contains(ModItems.SHARD_SHADOW.get())
                && owned.contains(ModItems.SHARD_THUNDER.get())
                && owned.contains(ModItems.SHARD_STAR.get())
                && owned.contains(ModItems.SHARD_RUNE.get());
        if (hasTwelve) {
            grant(player, ADV_TWELVE_CONSTELLATION);
        }
        if (hasTwelve && owned.contains(ModItems.SHARD_SKY.get())) {
            grant(player, ADV_THIRTEEN_RETURN);
        }
    }

    /** 苦厄磨砺（境界 5）/ 境界逆转（境界 9） */
    public static void checkBrokenCoreTierAdvancements(ItemStack stack, ServerPlayer player) {
        if (!(stack.getItem() instanceof BrokenCoreItem coreItem)) {
            return;
        }
        int tier = coreItem.getTier(stack);
        if (tier >= 5) {
            grant(player, ADV_HARDSHIP_TEMPER);
        }
        if (tier >= 9) {
            grant(player, ADV_TIER9_REVERSE);
        }
    }

    /**
     * 锻刃前行：长剑达到 T3（碎神影剑及以上）。
     *
     * <p>旧版靠物品 NBT 的 {@code sword_tier}，1.21 版长剑是独立物品，直接按物品判定。
     */
    public static void checkSwordMidAdvancement(ItemStack swordStack, ServerPlayer player) {
        if (isMidOrHigherSword(swordStack)) {
            grant(player, ADV_SWORD_MID_FORGE);
        }
    }

    /** 是否 T3 及以上的长剑 */
    public static boolean isMidOrHigherSword(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item == ModItems.SHADOW_SWORD.get()
                || item == ModItems.CORE_CONQUEST_SWORD.get()
                || item == ModItems.WAR_BLADE.get()
                || item == ModItems.WITHER_BLADE.get()
                || item == ModItems.WARDEN_SWORD.get()
                || item == ModItems.ABYSS_SWORD.get()
                || item == ModItems.END_DRAGON_SWORD.get();
    }

    /** 宝匣寻神：从宝箱拿到碎片时调用 */
    public static void triggerChestFindShard(ServerPlayer player) {
        grant(player, ADV_CHEST_FIND_SHARD);
    }

    /** 统计玩家已完成进度（跳过 recipes/*） */
    public static int countAllCompletedAdvancements(ServerPlayer sp) {
        if (sp.getServer() == null) {
            return 0;
        }
        int count = 0;
        for (AdvancementHolder holder : sp.getServer().getAdvancements().getAllAdvancements()) {
            if (holder.id().getPath().startsWith("recipes/")) {
                continue;
            }
            if (sp.getAdvancements().getOrStartProgress(holder).isDone()) {
                count++;
            }
        }
        return count;
    }

    /** god_shard_slot 已装备碎片数（最多 13） */
    public static int countEquippedShards(ServerPlayer sp) {
        ICuriosItemHandler handler = getCurios(sp);
        if (handler == null) {
            return 0;
        }
        ICurioStacksHandler slot = handler.getCurios().get("god_shard_slot");
        if (slot == null) {
            return 0;
        }
        IDynamicStackHandler stacks = slot.getStacks();
        int count = 0;
        for (int i = 0; i < 13; i++) {
            if (!stacks.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
