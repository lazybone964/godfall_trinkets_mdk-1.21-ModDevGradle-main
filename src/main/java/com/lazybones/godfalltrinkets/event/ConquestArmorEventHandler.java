package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.custom.ArmorItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 征伐套 / 深渊征伐套的会心与吸血。1.21 的 {@code LivingHurtEvent} 已拆成
 * {@link LivingIncomingDamageEvent}（护甲减免前，和旧版同语义）。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public class ConquestArmorEventHandler {

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        int normalConquestCount = ArmorItems.getConquestPieceCount(player);
        int abyssConquestCount = ArmorItems.getAbyssConquestPieceCount(player);
        float finalDamage = event.getAmount();

        // 普通征伐：单件 6%，全套 24%，1.75 倍
        if (normalConquestCount > 0 && player.getRandom().nextFloat() < normalConquestCount * 0.06F) {
            finalDamage *= 1.75F;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        // 深渊征伐：单件 10%，全套 40%，2.0 倍
        if (abyssConquestCount > 0 && player.getRandom().nextFloat() < abyssConquestCount * 0.1F) {
            finalDamage *= 2.0F;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.2F);
        }

        // 全套吸血，两套分开结算
        if (ArmorItems.isFullConquestSet(player)) {
            player.heal(finalDamage * 0.25F);
        }
        if (ArmorItems.isFullAbyssConquestSet(player)) {
            player.heal(finalDamage * 0.35F);
        }

        event.setAmount(finalDamage);
    }
}
