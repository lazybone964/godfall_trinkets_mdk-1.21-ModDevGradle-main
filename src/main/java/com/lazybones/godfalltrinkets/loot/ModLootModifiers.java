package com.lazybones.godfalltrinkets.loot;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 战利品修饰器序列化器注册（阶段四）。
 * id 必须和 data/godfall_trinkets/loot_modifiers/*.json 里的 "type" 后半段一致。
 */
public final class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    GodfallTrinkets.MOD_ID);

    static {
        SERIALIZERS.register("shard_chest_loot", () -> ShardLootModifier.CODEC);
        SERIALIZERS.register("trinket_chest_loot", () -> TrinketLootModifier.CODEC);
    }

    private ModLootModifiers() {
    }
}
