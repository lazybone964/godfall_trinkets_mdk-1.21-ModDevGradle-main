package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.custom.ArmorItems;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 三套神装的套装效果。1.21 里 {@code LivingEvent.LivingTickEvent} 拆成了
 * {@code LivingTickEvent.Pre/Post}，玩家这边直接用 {@link PlayerTickEvent.Post}。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public class ArmorEvent {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (ArmorItems.isFullGodSet(player)) {
            ArmorItems.applyGodSetEffects(player);
        }
        if (ArmorItems.isFullConquestSet(player)) {
            ArmorItems.applyConquestSetEffects(player);
        }
        if (ArmorItems.isFullAbyssConquestSet(player)) {
            ArmorItems.applyAbyssConquestSetEffects(player);
        }
    }
}
