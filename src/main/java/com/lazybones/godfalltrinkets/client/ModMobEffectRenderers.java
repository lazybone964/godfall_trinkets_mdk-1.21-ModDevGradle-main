package com.lazybones.godfalltrinkets.client;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.ModMobEffects;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * 药效图标（阶段四）。1.21 取消了 MobEffect#initializeClient，
 * 客户端图标统一在模组总线的 RegisterClientExtensionsEvent 里注册。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModMobEffectRenderers {

    private static final ResourceLocation ABYSS_MARK_TEX =
            ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, "textures/mob_effect/abyss_mark.png");
    private static final ResourceLocation FALLEN_WRATH_TEX =
            ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, "textures/mob_effect/fallen_wrath_stack.png");

    private ModMobEffectRenderers() {
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerMobEffect(new IconExtensions(ABYSS_MARK_TEX), ModMobEffects.ABYSS_MARK.value());
        event.registerMobEffect(new IconExtensions(FALLEN_WRATH_TEX), ModMobEffects.FALLEN_WRATH_STACK.value());
    }

    private static final class IconExtensions implements IClientMobEffectExtensions {

        private final ResourceLocation texture;

        private IconExtensions(ResourceLocation texture) {
            this.texture = texture;
        }

        @Override
        public boolean renderInventoryIcon(MobEffectInstance instance,
                                           EffectRenderingInventoryScreen<?> screen,
                                           GuiGraphics guiGraphics, int x, int y, int blitOffset) {
            guiGraphics.blit(this.texture, x, y + 7, blitOffset, 0.0f, 0.0f, 18, 18, 18, 18);
            return true;
        }

        @Override
        public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics guiGraphics,
                                     int x, int y, float z, float alpha) {
            guiGraphics.blit(this.texture, x + 3, y + 3, 0, 0.0f, 0.0f, 18, 18, 18, 18);
            return true;
        }
    }
}
