package com.lazybones.godfalltrinkets;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 药效注册（阶段四）。
 * <p>
 * 与 1.20.1 的差异：
 * 1. 1.21 没有 {@code initializeClient}，客户端图标改到
 *    {@code client/ModMobEffectRenderers} 用 {@code RegisterClientExtensionsEvent} 注册；
 * 2. 注册表走 {@code Registries.MOB_EFFECT}，注册结果类型是 {@code Holder<MobEffect>}；
 * 3. {@code isDurationEffectTick} 改名 {@code shouldApplyEffectTickThisTick}。
 */
public final class ModMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, GodfallTrinkets.MOD_ID);

    /** 深渊标记：受伤增加 */
    public static final DeferredHolder<MobEffect, MobEffect> ABYSS_MARK =
            MOB_EFFECTS.register("abyss_mark", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x602090) {
                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
                    return false;
                }
            });

    /** 神陨怒层：可堆叠易伤 */
    public static final DeferredHolder<MobEffect, MobEffect> FALLEN_WRATH_STACK =
            MOB_EFFECTS.register("fallen_wrath_stack", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x903020) {
                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
                    return false;
                }
            });

    private ModMobEffects() {
    }
}
