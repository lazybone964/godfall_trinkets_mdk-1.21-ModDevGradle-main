package com.lazybones.godfalltrinkets.network;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 打开末影箱请求（无数据包体） */
public record OpenEnderChestPayload() implements CustomPacketPayload {

    public static final Type<OpenEnderChestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, "open_ender_chest"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenEnderChestPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenEnderChestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
