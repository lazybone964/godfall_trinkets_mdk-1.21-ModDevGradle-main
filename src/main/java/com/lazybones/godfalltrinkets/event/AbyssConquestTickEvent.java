package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 深渊征伐套的单件效果。
 *
 * <p>1.21 迁移要点：
 * <ol>
 *   <<li>{@code TickEvent.PlayerTickEvent} → {@link PlayerTickEvent.Post}；</li>
 *   <li>{@code MobEffects.X} 从 {@code MobEffect} 变成 {@code Holder<MobEffect>}，
 *       所以 {@code removeEffect} 走 Holder 重载；</li>
 *   <li>{@code MobEffectEvent.Applicable.Result.DENY} → {@code Result.DO_NOT_APPLY}；</li>
 *   <li>{@code LivingHurtEvent} → {@link LivingIncomingDamageEvent}。</li>
 * </ol>
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public class AbyssConquestTickEvent {

    private static final int CLEANSE_TICK = 20;
    private static final String CLEANSE_TAG = "abyss_chest_cleanse_tick";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);

        // 深渊头盔：幸运 II
        if (head.is(ModItems.ABYSS_GOD_HELMET.get())) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 40, 1, false, false));
        }
        // 深渊护腿：移速 II
        if (legs.is(ModItems.ABYSS_GOD_LEGGINGS.get())) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
        }
        // 深渊胸甲：每 20 tick 清一次负面
        if (chest.is(ModItems.ABYSS_GOD_CHESTPLATE.get())) {
            int tick = player.getPersistentData().getInt(CLEANSE_TAG) + 1;
            player.getPersistentData().putInt(CLEANSE_TAG, tick);
            if (tick >= CLEANSE_TICK) {
                player.getPersistentData().putInt(CLEANSE_TAG, 0);
                List<Holder<MobEffect>> toRemove = new ArrayList<>();
                for (Holder<MobEffect> effect : player.getActiveEffectsMap().keySet()) {
                    if (effect == MobEffects.WITHER
                            || effect == MobEffects.POISON
                            || effect == MobEffects.MOVEMENT_SLOWDOWN
                            || effect == MobEffects.DIG_SLOWDOWN
                            || effect == MobEffects.BLINDNESS
                            || effect == MobEffects.DARKNESS
                            || effect == MobEffects.WEAKNESS
                            || effect == MobEffects.HUNGER) {
                        toRemove.add(effect);
                    }
                }
                for (Holder<MobEffect> effect : toRemove) {
                    player.removeEffect(effect);
                }
            }
        }
    }

    /** 深渊头盔：免疫黑暗 */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getEffectInstance().getEffect() != MobEffects.DARKNESS) {
            return;
        }
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ABYSS_GOD_HELMET.get())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** 深渊胸甲减伤 12%；深渊靴子免摔落 */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        float damage = event.getAmount();
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ABYSS_GOD_CHESTPLATE.get())) {
            damage *= 0.88F;
        }
        if (player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.ABYSS_GOD_BOOTS.get())
                && event.getSource().is(DamageTypes.FALL)) {
            damage = 0F;
        }
        event.setAmount(Math.max(0F, damage));
    }
}
