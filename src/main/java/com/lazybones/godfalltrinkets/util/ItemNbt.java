package com.lazybones.godfalltrinkets.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * ItemStack NBT 帮助类（1.21 版）。
 * <p>
 * 1.20.5 之后 {@code getOrCreateTag() / getTag() / setTag()} 全部被移除，自定义 NBT 统一挂在
 * {@code DataComponents.CUSTOM_DATA} 组件上。这里把「读 → 改 → 写回」封成一个动作，
 * 避免各处忘记 save 导致数据只在内存里改了、没写进物品。
 */
public final class ItemNbt {

    private ItemNbt() {
    }

    /** 只读副本：改它不会影响物品，需要 {@link #save} 才生效 */
    public static CompoundTag get(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    public static void save(ItemStack stack, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static int getInt(ItemStack stack, String key) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(key);
    }

    public static float getFloat(ItemStack stack, String key) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getFloat(key);
    }

    public static String getString(ItemStack stack, String key) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(key);
    }

    public static boolean getBoolean(ItemStack stack, String key) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBoolean(key);
    }

    public static void putInt(ItemStack stack, String key, int value) {
        CompoundTag tag = get(stack);
        tag.putInt(key, value);
        save(stack, tag);
    }

    public static void putFloat(ItemStack stack, String key, float value) {
        CompoundTag tag = get(stack);
        tag.putFloat(key, value);
        save(stack, tag);
    }

    public static void putString(ItemStack stack, String key, String value) {
        CompoundTag tag = get(stack);
        tag.putString(key, value);
        save(stack, tag);
    }

    public static void putBoolean(ItemStack stack, String key, boolean value) {
        CompoundTag tag = get(stack);
        tag.putBoolean(key, value);
        save(stack, tag);
    }

    public static boolean contains(ItemStack stack, String key) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(key);
    }
}
