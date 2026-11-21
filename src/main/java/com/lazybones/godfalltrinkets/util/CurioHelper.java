package com.lazybones.godfalltrinkets.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Predicate;

/**
 * Curios 查询工具。
 * <p>
 * 1.21.1 的 Curios 9.x 里 {@code CuriosApi.getCuriosInventory(Player)} 直接返回
 * {@code Optional<ICuriosItemHandler>}（1.20.1 那套 LazyOptional#resolve() 没有了），
 * 用 {@code isEquipped} / {@code findFirstCurio} 判断是否佩戴。
 */
public final class CurioHelper {

    private CurioHelper() {
    }

    /** 玩家是否佩戴了指定物品 */
    public static boolean hasTrinket(Player player, Item item) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.isEquipped(item))
                .orElse(false);
    }

    /** 玩家是否有任意一件满足条件的饰品 */
    public static boolean hasTrinket(Player player, Predicate<ItemStack> predicate) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(predicate))
                .isPresent();
    }
}
