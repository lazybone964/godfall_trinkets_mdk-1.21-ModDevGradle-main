package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.ModAttachments;
import com.lazybones.godfalltrinkets.config.GodfallConfig;
import com.lazybones.godfalltrinkets.item.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 破厄之核佩戴时的「再生」长效 buff。
 * 旧版 {@code TickEvent.PlayerTickEvent(END)} + persistentData →
 * 1.21 用 {@link PlayerTickEvent.Post} + {@link ModAttachments#GIVEN_CORE_REGEN}。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public class CurioRegenBuffEvent {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !player.isAlive()) {
            return;
        }

        int regenAmp = GodfallConfig.BROKEN_CORE_REGEN_AMPLIFIER.get();
        int durationTicks = GodfallConfig.BROKEN_CORE_REGEN_DURATION_TICKS.get();

        boolean wearing = CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(s -> s.is(ModItems.BROKEN_CORE.get())))
                .isPresent();

        boolean given = player.getData(ModAttachments.GIVEN_CORE_REGEN);
        if (wearing) {
            // 佩戴中：只发一次，不刷新时长（与原版行为一致）
            if (!given) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, durationTicks, regenAmp,
                        false, true, true));
                player.setData(ModAttachments.GIVEN_CORE_REGEN, true);
            }
        } else if (given) {
            // 卸下：清标记，并移除「当前配置等级」的再生（避免误删别的来源的再生）
            player.setData(ModAttachments.GIVEN_CORE_REGEN, false);
            MobEffectInstance current = player.getEffect(MobEffects.REGENERATION);
            if (current != null && current.getAmplifier() == regenAmp) {
                player.removeEffect(MobEffects.REGENERATION);
            }
        }
    }
}
