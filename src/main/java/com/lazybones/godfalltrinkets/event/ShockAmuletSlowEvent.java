package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.util.CurioHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 震斥护符：近战命中给目标挂 30 秒缓慢 I。
 * <p>
 * 1.20.1 的 {@code LivingAttackEvent} 在 NeoForge 1.21 已取消，
 * 统一改用 {@link LivingIncomingDamageEvent}（设置在护甲结算之前，时机等价）。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public final class ShockAmuletSlowEvent {

    private static final int SLOW_DURATION = 30 * 20;
    private static final int SLOW_AMP = 0;

    private ShockAmuletSlowEvent() {
    }

    @SubscribeEvent
    public static void onMeleeHit(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)
                || player == event.getEntity()) {
            return;
        }
        if (!CurioHelper.hasTrinket(player, ModItems.SHOCK_AMULET.get())) {
            return;
        }
        event.getEntity().addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION, SLOW_AMP, false, true, true));
    }
}
