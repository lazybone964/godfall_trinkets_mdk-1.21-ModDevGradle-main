package com.lazybones.godfalltrinkets.item.custom;

import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.util.ItemNbt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 咒厄转移坠 —— 替玩家承受破厄之核的诅咒（curse_transfer 槽，需先用恶咒令牌解锁）。
 * <ul>
 * <li>每件物品随机绑定 2 条不重复诅咒（从 8 种里抽）</li>
 * <li>戴着破厄之核时，每秒按诅咒条数消耗耐久</li>
 * <li>耐久耗尽 → 碎裂、归还诅咒、粒子 + 音效</li>
 * </ul>
 */
public class CurseTransferTrinketItem extends Item implements ICurioItem {

    /** 8 条诅咒标识，与 BrokenCoreItem 的诅咒一一对应 */
    public static final List<String> ALL_CURSE_TAGS = List.of(
            "agony",          // 生命枯竭
            "deep_wound",     // 伤痛加深
            "blunt_edge",     // 锋芒钝化
            "hunger_curse",   // 永饥之噬
            "sleepless",      // 无眠诅咒
            "scarred",        // 重创烙印
            "weak_body",      // 身躯羸弱
            "foul_body"       // 秽体易伤
    );

    private static final Random RAND = new Random();
    private static final String TAG_LIST = "transfer_curse_list";
    private static final String TAG_LOCKED = "curse_locked";

    /** 这些 NBT 标签会在 tick 时被自动剥离（防止被铁砧/创造模式塞进附魔等） */
    private static final Set<String> NBT_BLACKLIST = Set.of(
            "Unbreakable", "Enchantments", "StoredEnchantments",
            "RepairCost", "AttributeModifiers", "CustomCreativeLock"
    );

    public CurseTransferTrinketItem(Properties properties) {
        super(properties);
    }

    // ==================== 初始化 & 诅咒绑定 ====================

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        initRandomCursePair(stack);
        return stack;
    }

    /** 为新物品随机绑定 2 条不重复诅咒 */
    public static void initRandomCursePair(ItemStack stack) {
        if (!(stack.getItem() instanceof CurseTransferTrinketItem)) {
            return;
        }
        CompoundTag tag = ItemNbt.get(stack);
        if (tag.contains(TAG_LIST)) {
            return;
        }
        List<String> pool = new ArrayList<>(ALL_CURSE_TAGS);
        ListTag list = new ListTag();
        list.add(StringTag.valueOf(pool.remove(RAND.nextInt(pool.size()))));
        list.add(StringTag.valueOf(pool.remove(RAND.nextInt(pool.size()))));
        tag.put(TAG_LIST, list);
        tag.putBoolean(TAG_LOCKED, true);
        ItemNbt.save(stack, tag);
    }

    /** 此饰品锁定的诅咒标识列表（首次调用时懒初始化） */
    public static List<String> getLockedCurseTags(ItemStack stack) {
        if (stack.getItem() instanceof CurseTransferTrinketItem) {
            initRandomCursePair(stack);
        }
        List<String> result = new ArrayList<>();
        CompoundTag tag = ItemNbt.get(stack);
        if (!tag.contains(TAG_LIST)) {
            return result;
        }
        ListTag list = tag.getList(TAG_LIST, 8);
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getString(i));
        }
        return result;
    }

    /** 玩家身上咒厄转移坠吸收的诅咒集合 */
    public static Set<String> getAbsorbedCurses(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(s -> s.getItem() instanceof CurseTransferTrinketItem))
                .map(result -> (Set<String>) new HashSet<>(getLockedCurseTags(result.stack())))
                .orElse(Collections.emptySet());
    }

    public static boolean isAbsorbingCurse(Player player, String curseTag) {
        return getAbsorbedCurses(player).contains(curseTag);
    }

    // ==================== Curios 槽位 ====================

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        String identifier = slotContext.identifier();
        String path = identifier.indexOf(':') >= 0
                ? identifier.substring(identifier.indexOf(':') + 1)
                : identifier;
        if (!"curse_transfer".equals(path)) {
            return false;
        }
        return slotContext.entity() instanceof Player player
                && player.getPersistentData().getBoolean(CurseSlotUnlockItem.TAG_UNLOCKED_CURSE_SLOT);
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
        tooltips.add(Component.translatable("curios.identifier.curse_transfer"));
        return tooltips;
    }

    // ==================== Tick：耐久损耗 ====================

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity().level().isClientSide()) {
            return;
        }
        if (!(slotContext.entity() instanceof Player player)) {
            return;
        }
        stripBlacklistedNbt(stack);

        // 只有戴着破厄之核（诅咒真正生效）时才消耗耐久
        if (!GodfallTrinketItem.wearingCore(player)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        int curseCount = getLockedCurseTags(stack).size();
        if (curseCount == 0) {
            return;
        }
        int newDamage = stack.getDamageValue() + curseCount;
        stack.setDamageValue(newDamage);
        if (newDamage >= stack.getMaxDamage() - 1) {
            destroyAndReturnCurses(stack, player);
        }
    }

    /** 销毁饰品，归还诅咒，播放粒子 + 音效 */
    private void destroyAndReturnCurses(ItemStack stack, Player player) {
        Level level = player.level();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    player.getX(), player.getY() + 1.2, player.getZ(),
                    15, 0.3, 0.3, 0.3, 0.05);
            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                    player.getX(), player.getY() + 1.5, player.getZ(),
                    8, 0.2, 0.2, 0.2, 0.01);
        }
        level.playSound(null, player.blockPosition(),
                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0f, 1.2f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.6f, 0.8f);
        player.sendSystemMessage(Component.translatable(
                "message.godfall_trinkets.curse_return").withStyle(ChatFormatting.DARK_RED));
        stack.setCount(0);
    }

    // ==================== Tooltip ====================

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        List<String> curses = getLockedCurseTags(stack);
        if (!curses.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.godfall_trinkets.absorbed_curses")
                    .withStyle(ChatFormatting.DARK_RED));
            for (String tag : curses) {
                tooltip.add(Component.literal("  - ")
                        .append(Component.translatable("curse.godfall_trinkets." + tag))
                        .withStyle(ChatFormatting.RED));
            }
        }
        tooltip.add(Component.translatable("tooltip.godfall_trinkets.curse_durability",
                        stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage())
                .withStyle(ChatFormatting.GRAY));
    }

    // ==================== 附魔 / 修复 / NBT 防御 ====================

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairMaterial) {
        return repairMaterial.is(ModItems.GODFRAGMENT.get());
    }

    private static void stripBlacklistedNbt(ItemStack stack) {
        CompoundTag tag = ItemNbt.get(stack);
        if (tag.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (String banned : NBT_BLACKLIST) {
            if (tag.contains(banned)) {
                tag.remove(banned);
                changed = true;
            }
        }
        if (changed) {
            ItemNbt.save(stack, tag);
        }
    }

    @Override
    public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source,
                                       boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }
}
