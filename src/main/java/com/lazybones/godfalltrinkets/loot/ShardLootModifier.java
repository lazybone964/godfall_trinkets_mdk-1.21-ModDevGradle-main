package com.lazybones.godfalltrinkets.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;
import java.util.Random;

/**
 * 神位碎片宝箱掉落（阶段四）：按概率往指定结构宝箱里塞碎片，无需佩戴饰品。
 * JSON 见 data/godfall_trinkets/loot_modifiers/*.json，序列化 id 为 shard_chest_loot。
 */
public class ShardLootModifier extends LootModifier {

    public static final MapCodec<ShardLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance)
                    .and(ShardEntry.CODEC.listOf().fieldOf("shards").forGetter(m -> m.shards))
                    .apply(instance, ShardLootModifier::new));

    private static final Random RAND = new Random();

    private final List<ShardEntry> shards;

    public ShardLootModifier(LootItemCondition[] conditions, List<ShardEntry> shards) {
        super(conditions);
        this.shards = shards;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (ShardEntry entry : shards) {
            if (RAND.nextFloat() < entry.chance()) {
                generatedLoot.add(new ItemStack(entry.item()));
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    /** 碎片条目：物品 + 概率（0~1） */
    public record ShardEntry(Item item, float chance) {
        public static final Codec<ShardEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(e -> e.item),
                        Codec.FLOAT.fieldOf("chance").forGetter(e -> e.chance)
                ).apply(instance, ShardEntry::new));
    }
}
