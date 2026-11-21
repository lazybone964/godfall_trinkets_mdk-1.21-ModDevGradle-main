package com.lazybones.godfalltrinkets.curse;

import com.lazybones.godfalltrinkets.ModAttachments;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 咒厄转移坠的「已吸收诅咒」状态。
 *
 * 1.20.1 里这份数据放在玩家的 persistentData 里（CurseTransferTrinketItem 读写），
 * 1.21 改由 {@link ModAttachments#ABSORBED_CURSES} 承载。
 * 阶段三先提供读写接口，诅咒转移物品本体随后迁移时直接复用。
 */
public final class CurseTransferState {

    private CurseTransferState() {
    }

    public static List<String> getAbsorbedCurses(Player player) {
        return player.getData(ModAttachments.ABSORBED_CURSES);
    }

    /** 该诅咒是否已被吸收（吸收后破厄之核对应诅咒失效） */
    public static boolean isAbsorbing(Player player, String curse) {
        return getAbsorbedCurses(player).contains(curse);
    }

    public static void setAbsorbedCurses(Player player, Collection<String> curses) {
        player.setData(ModAttachments.ABSORBED_CURSES, List.copyOf(new ArrayList<>(curses)));
    }

    public static void addAbsorbedCurse(Player player, String curse) {
        List<String> list = new ArrayList<>(getAbsorbedCurses(player));
        if (!list.contains(curse)) {
            list.add(curse);
        }
        setAbsorbedCurses(player, list);
    }

    public static void clear(Player player) {
        player.setData(ModAttachments.ABSORBED_CURSES, List.of());
    }
}
