package com.lazybones.godfalltrinkets.network;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.util.CurioHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 模组网络通道（阶段四）。
 * <p>
 * 1.21 NeoForge 取消了 SimpleChannel，改成 RegisterPayloadHandlersEvent + PayloadRegistrar，
 * 一个 payload 就是一条通道；这里只有「打开末影箱」一条客户端→服务端请求。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(OpenEnderChestPayload.TYPE, OpenEnderChestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && canOpenEnderChest(player)) {
                        player.openMenu(new SimpleMenuProvider(
                                (id, inv, p) -> ChestMenu.threeRows(id, inv, player.getEnderChestInventory()),
                                Component.translatable("container.enderchest")));
                    }
                }));
    }

    /** 服务端校验：佩戴破厄之核/神格 + 虚空碎片才可打开 */
    private static boolean canOpenEnderChest(ServerPlayer player) {
        return CurioHelper.hasTrinket(player,
                stack -> stack.is(ModItems.BROKEN_CORE.get()) || stack.is(ModItems.GOD_CORE.get()))
                && CurioHelper.hasTrinket(player, ModItems.SHARD_VOID.get());
    }
}
