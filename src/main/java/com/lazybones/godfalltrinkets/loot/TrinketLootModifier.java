package com.lazybones.godfalltrinkets.loot;

import com.lazybones.godfalltrinkets.util.CurioHelper;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;
import java.util.Random;

/**
 * 佩戴指定饰品后额外注入战利品（阶段四）。
 * 序列化 id 为 trinket_chest_loot。
 */
public class TrinketLootModifier extends LootModifier {

    public static final MapCodec<TrinketLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance)
                    .and(BuiltInRegistries.ITEM.byNameCodec().fieldOf("trinket").forGetter(m -> m.trinketItem))
                    .and(WeightedLootEntry.CODEC.listOf().fieldOf("extra_items").forGetter(m -> m.extraItems))
                    .apply(instance, TrinketLootModifier::new));

    private static final Random RAND = new Random();

    private final Item trinketItem;
    private final List<WeightedLootEntry> extraItems;

    public TrinketLootModifier(LootItemCondition[] conditions, Item trinket,
                               List<WeightedLootEntry> extraItems) {
        super(conditions);
        this.trinketItem = trinket;
        this.extraItems = extraItems;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof Player player)
                || !CurioHelper.hasTrinket(player, trinketItem)) {
            return generatedLoot;
        }

        int totalWeight = extraItems.stream().mapToInt(WeightedLootEntry::weight).sum();
        if (totalWeight <= 0) {
            return generatedLoot;
        }
        for (WeightedLootEntry entry : extraItems) {
            if (RAND.nextInt(totalWeight) < entry.weight()) {
                int count = entry.minCount() == entry.maxCount()
                        ? entry.minCount()
                        : entry.minCount() + RAND.nextInt(entry.maxCount() - entry.minCount() + 1);
                generatedLoot.add(new ItemStack(entry.item(), count));
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    /** 额外物品条目：物品 + 权重 + 数量范围 */
    public record WeightedLootEntry(Item item, int weight, int minCount, int maxCount) {
        public static final Codec<WeightedLootEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(e -> e.item),
                        Codec.INT.optionalFieldOf("weight", 1).forGetter(e -> e.weight),
                        Codec.INT.optionalFieldOf("min", 1).forGetter(e -> e.minCount),
                        Codec.INT.optionalFieldOf("max", 1).forGetter(e -> e.maxCount)
                ).apply(instance, WeightedLootEntry::new));
    }
}
