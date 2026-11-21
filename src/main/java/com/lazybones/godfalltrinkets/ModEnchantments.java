package com.lazybones.godfalltrinkets;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * 附魔键（阶段四）。
 * <p>
 * 1.21 的 Enchantment 是 final record、且属于数据包注册表，不能在代码里 DeferredRegister，
 * 只能写 data/&lt;ns&gt;/enchantment/*.json；这里只保留 ResourceKey 与查询辅助方法，
 * 实际数值（等级/权重/可附物品）见 src/main/resources/data/godfall_trinkets/enchantment/。
 */
public final class ModEnchantments {

    /** 神陨庇护：护甲，最大 5 级 */
    public static final ResourceKey<Enchantment> GODFALL_SHELTER = key("godfall_protection");
    /** 碎核共鸣：武器，最大 4 级 */
    public static final ResourceKey<Enchantment> SHARD_RESONANCE = key("shard_resonance");
    /** 虚空汲取：武器，最大 3 级 */
    public static final ResourceKey<Enchantment> VOID_DRAIN = key("void_drain");
    /** 不朽誓约：胸甲，最大 1 级 */
    public static final ResourceKey<Enchantment> IMMORTAL_OATH = key("immortal_oath");
    /** 深渊凝视：武器，最大 1 级 */
    public static final ResourceKey<Enchantment> ABYSS_GAZE = key("abyss_gaze");
    /** 神陨之怒：武器，最大 5 级 */
    public static final ResourceKey<Enchantment> FURY_OF_GODFALL = key("fury_of_godfall");

    private ModEnchantments() {
    }

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, name));
    }

    /** 从当前维度的注册表里取附魔 Holder；数据包没加载到就返回 null。 */
    public static Holder<Enchantment> get(HolderLookup.Provider provider, ResourceKey<Enchantment> key) {
        return provider.lookupOrThrow(Registries.ENCHANTMENT).get(key).orElse(null);
    }
}
