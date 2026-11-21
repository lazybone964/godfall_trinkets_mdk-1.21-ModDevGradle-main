package com.lazybones.godfalltrinkets.client;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.network.OpenEnderChestPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** 客户端按键绑定（阶段四）：K 键打开末影箱（需破厄之核/神格 + 虚空碎片） */
public final class ModKeyBindings {

    public static final String CATEGORY = "key.category.godfall_trinkets";

    public static final KeyMapping OPEN_ENDER_CHEST = new KeyMapping(
            "key.godfall_trinkets.open_ender_chest",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    );

    private ModKeyBindings() {
    }

    @EventBusSubscriber(modid = GodfallTrinkets.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_ENDER_CHEST);
        }
    }

    @EventBusSubscriber(modid = GodfallTrinkets.MOD_ID, value = Dist.CLIENT)
    public static final class Input {
        private Input() {
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (OPEN_ENDER_CHEST.consumeClick()) {
                PacketDistributor.sendToServer(new OpenEnderChestPayload());
            }
        }
    }
}
