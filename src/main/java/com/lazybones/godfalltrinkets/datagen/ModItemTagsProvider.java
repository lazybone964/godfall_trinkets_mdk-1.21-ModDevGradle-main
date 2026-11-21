package com.lazybones.godfalltrinkets.datagen;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider.TagLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * Curios 槽位标签生成器。
 * <p>
 * Curios 判定「某件饰品能不能进某个槽」靠的是物品标签 {@code curios:<槽位id>}，
 * {@code ICurioItem#canEquip} 只是二次校验。所以只实现 canEquip 而没生成标签时，
 * 物品在 curios 界面里根本拖不进任何槽位。
 * <p>
 * 新增饰品时，把物品加进 addTags 里对应槽位的 {@code tag(...).add(...)} 即可。
 */
public class ModItemTagsProvider extends ItemTagsProvider {

    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                               CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, GodfallTrinkets.MOD_ID, existingFileHelper);
    }

    /** 构造 {@code curios:<slot>} 物品标签 */
    private static TagKey<Item> curio(String slot) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", slot));
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(curio("ring")).add(
                ModItems.ABYSS_RUNE_RING.get(),
                ModItems.DEATH_DEFY_RING.get(),
                ModItems.WARDEN_EYE.get(),
                ModItems.WARDEN_PULSE.get(),
                ModItems.SHARP_EDGE_RING.get(),
                ModItems.SOUL_DRAIN_RING.get(),
                ModItems.TOUGH_HEART_RING.get(),
                ModItems.HUNTER_RING.get(),
                ModItems.DEEP_ROCK_RING.get());

        tag(curio("bracelet")).add(
                ModItems.EMBER_BRACELET.get(),
                ModItems.KINGS_GRIP.get(),
                ModItems.WARDEN_ARMORBREAK.get(),
                ModItems.PURIFYING_BRACELET.get(),
                ModItems.BARRIER_BRACELET.get(),
                ModItems.FERTILE_BRACELET.get(),
                ModItems.PROSPERITY_BRACELET.get());

        tag(curio("charm")).add(
                ModItems.BLUNTING_AMULET.get(),
                ModItems.SHOCK_AMULET.get(),
                ModItems.EYE_OF_THE_END.get(),
                ModItems.TOUGH_BONE_AMULET.get(),
                ModItems.VITAL_AMULET.get(),
                ModItems.FORGE_AMULET.get(),
                ModItems.TREASURE_AMULET.get());

        tag(curio("body")).add(
                ModItems.DRAGON_CORE_HEART.get(),
                ModItems.STOMACH_POUCH.get(),
                ModItems.WITHER_HEART.get(),
                ModItems.WARDEN_WEIGHT.get());

        tag(curio("head")).add(
                ModItems.CRYSTAL_CROWN.get(),
                ModItems.WITHER_CROWN.get());

        // 项链槽：凋零脊骨坠
        tag(curio("necklace")).add(
                ModItems.WITHER_SPINE.get());

        // 背饰槽：寂灭龙脊
        tag(curio("back")).add(
                ModItems.DRAGON_SPINE_END.get());

        // 破厄之核槽：破厄之核与进化后的神格都能戴（核本身 1 格）
        tag(curio("broken_core_slot")).add(
                ModItems.BROKEN_CORE.get(),
                ModItems.GOD_CORE.get());

        // 神位碎片槽：初始 0 格，破厄之核 onEquip 时 +13 格
        tag(curio("god_shard_slot")).add(
                ModItems.SHARD_SKY.get(),
                ModItems.SHARD_SUN.get(),
                ModItems.SHARD_LIFE.get(),
                ModItems.SHARD_STRENGTH.get(),
                ModItems.SHARD_FATE.get(),
                ModItems.SHARD_WIND.get(),
                ModItems.SHARD_SPIRIT.get(),
                ModItems.SHARD_BARRIER.get(),
                ModItems.SHARD_VOID.get(),
                ModItems.SHARD_SHADOW.get(),
                ModItems.SHARD_THUNDER.get(),
                ModItems.SHARD_STAR.get(),
                ModItems.SHARD_RUNE.get());

        // 咒厄转移槽：0 格，用恶咒令牌解锁 1 格后才能佩戴
        tag(curio("curse_transfer")).add(
                ModItems.CURSE_TRANSFER_TRINKET.get());

        // 功业徽记：badge 槽
        tag(curio("badge")).add(
                ModItems.ADVANCEMENT_TALLY_TRINKET.get());

        // 阶段四：十二件神装护甲要进原版附魔可作用标签，否则神陨庇护 / 不朽誓约对它们无效
        tag(ItemTags.ARMOR_ENCHANTABLE).add(
                ModItems.GOD_HELMET.get(), ModItems.GOD_CHESTPLATE.get(),
                ModItems.GOD_LEGGINGS.get(), ModItems.GOD_BOOTS.get(),
                ModItems.CONQUEST_HELMET.get(), ModItems.CONQUEST_CHESTPLATE.get(),
                ModItems.CONQUEST_LEGGINGS.get(), ModItems.CONQUEST_BOOTS.get(),
                ModItems.ABYSS_GOD_HELMET.get(), ModItems.ABYSS_GOD_CHESTPLATE.get(),
                ModItems.ABYSS_GOD_LEGGINGS.get(), ModItems.ABYSS_GOD_BOOTS.get());

        tag(ItemTags.CHEST_ARMOR_ENCHANTABLE).add(
                ModItems.GOD_CHESTPLATE.get(),
                ModItems.CONQUEST_CHESTPLATE.get(),
                ModItems.ABYSS_GOD_CHESTPLATE.get());

        tag(ItemTags.HEAD_ARMOR_ENCHANTABLE).add(
                ModItems.GOD_HELMET.get(),
                ModItems.CONQUEST_HELMET.get(),
                ModItems.ABYSS_GOD_HELMET.get());

        tag(ItemTags.LEG_ARMOR_ENCHANTABLE).add(
                ModItems.GOD_LEGGINGS.get(),
                ModItems.CONQUEST_LEGGINGS.get(),
                ModItems.ABYSS_GOD_LEGGINGS.get());

        tag(ItemTags.FOOT_ARMOR_ENCHANTABLE).add(
                ModItems.GOD_BOOTS.get(),
                ModItems.CONQUEST_BOOTS.get(),
                ModItems.ABYSS_GOD_BOOTS.get());
    }
}
