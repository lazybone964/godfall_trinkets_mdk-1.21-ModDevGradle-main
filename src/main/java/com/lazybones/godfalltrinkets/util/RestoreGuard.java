package com.lazybones.godfalltrinkets.util;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「饰品栏恢复中」标记。
 * <p>
 * 死亡重生（{@code PlayerEvent.Clone}）和重进存档（Curios 9 走 {@code OnDatapackSyncEvent}：
 * 先 {@code writeTag()} 存档，再把所有槽位 {@code setStackInSlot(EMPTY)} 清空，最后 {@code readTag()} 还原）
 * 这两个场景里，破厄之核都还没回到饰品栏，此时如果 {@code canEquip} 照常校验「必须戴核」、
 * 或者按「没核就弹回饰品」的规则处理，就会把饰品全退回背包 —— 这正是当年花很久修掉的那个 bug。
 * <p>
 * 所以恢复期间打上标记，让这两条规则临时放行；恢复结束后（下一次 tick 或数据包同步结束）清除。
 */
public final class RestoreGuard {

    /** 正在恢复饰品栏的玩家 */
    private static final Set<UUID> RESTORING = ConcurrentHashMap.newKeySet();
    /** 打标记时的 tickCount，用于「恢复完成后自动清除」 */
    private static final Map<UUID, Integer> BEGIN_TICK = new ConcurrentHashMap<>();

    private RestoreGuard() {
    }

    public static void begin(Player player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUUID();
        RESTORING.add(id);
        BEGIN_TICK.put(id, player.tickCount);
    }

    public static void end(Player player) {
        if (player == null) {
            return;
        }
        RESTORING.remove(player.getUUID());
        BEGIN_TICK.remove(player.getUUID());
    }

    public static boolean isRestoring(@Nullable Player player) {
        return player != null && RESTORING.contains(player.getUUID());
    }

    /** 每 tick 调一次：标记打上后过 3 tick 仍未被清（说明不是数据包同步路径）就自动解除 */
    public static void tick(Player player) {
        Integer begin = BEGIN_TICK.get(player.getUUID());
        if (begin != null && player.tickCount > begin + 3) {
            end(player);
        }
    }
}
