package com.lazybones.godfalltrinkets;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 1.21 的物品自定义数据走 DataComponent（NBT 的 {@code stack.getOrCreateTag()} 已经不能用了）。
 * 破厄之核需要持久化的几项状态都在这里。
 */
public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GodfallTrinkets.MOD_ID);

    /** 破厄经验（累计），对应旧 NBT 的 core_exp */
    public static final Supplier<DataComponentType<Integer>> CORE_EXP =
            DATA_COMPONENTS.register("core_exp", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** 护盾当前存量，对应旧 NBT 的 shield_hp */
    public static final Supplier<DataComponentType<Float>> CORE_SHIELD_HP =
            DATA_COMPONENTS.register("core_shield_hp", () -> DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build());

    /** 护盾刷新倒计时（刻），对应旧 NBT 的 shield_cd */
    public static final Supplier<DataComponentType<Integer>> CORE_SHIELD_CD =
            DATA_COMPONENTS.register("core_shield_cd", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** 是否已进化为神格，对应旧 NBT 的 is_god_core */
    public static final Supplier<DataComponentType<Boolean>> IS_GOD_CORE =
            DATA_COMPONENTS.register("is_god_core", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /** 铭纹碎片是否已经给这件装备加过附魔等级（对应旧 NBT godfall_rune_boosted） */
    public static final Supplier<DataComponentType<Boolean>> RUNE_BOOSTED =
            DATA_COMPONENTS.register("rune_boosted", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .build());

    /** 铭纹碎片加成前的原始附魔（对应旧 NBT godfall_rune_original） */
    public static final Supplier<DataComponentType<ItemEnchantments>> RUNE_ORIGINAL_ENCHANTS =
            DATA_COMPONENTS.register("rune_original_enchantments",
                    () -> DataComponentType.<ItemEnchantments>builder()
                            .persistent(ItemEnchantments.CODEC)
                            .networkSynchronized(ItemEnchantments.STREAM_CODEC)
                            .build());

    private ModDataComponents() {
    }
}
