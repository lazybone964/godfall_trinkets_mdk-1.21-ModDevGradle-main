package com.lazybones.godfalltrinkets.item.armor;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * 三套神装材质。
 *
 * <p>1.21 起 {@link ArmorMaterial} 变成了 record，不再由注册表持有，所以这里是
 * 「直接 new + Holder.direct」的做法交给 {@link ArmorItem} 用。
 *
 * <p>纹理走 {@link ArmorMaterial.Layer}：assetName 用 {@code godfall_trinkets:god} /
 * {@code :conquest} / {@code :abyss_god}，解析出来正好是
 * {@code assets/godfall_trinkets/textures/models/armor/god_layer_1.png} 与 {@code _layer_2.png}，
 * 和 1.20.1 的贴图路径完全一致（旧代码里那堆匿名重写 getArmorTexture 在这版已经不需要了）。
 */
public enum ArmorMaterials {

    /** 神陨合金诅咒耐受套 */
    GOD_INGOT("god", 42,
            4, 9, 7, 4,
            18, 4.0F, 0.2F,
            0F, 0F, 0F, 0F,
            () -> ModItems.GOD_INGOT.get()),

    /** 征伐战斗套 */
    CONQUEST_INGOT("conquest", 45,
            5, 10, 8, 5,
            20, 4.5F, 0.25F,
            0.20F, 0.35F, 0.25F, 0.15F,
            () -> ModItems.CONQUEST_INGOT.get()),

    /** 深渊征伐战套 */
    ABYSS_GOD_INGOT("abyss_god", 50,
            6, 12, 9, 6,
            25, 5.0F, 0.3F,
            0.30F, 0.50F, 0.35F, 0.22F,
            () -> ModItems.ABYSS_GOD_INGOT.get());

    /** 单件耐久基数（顺序：头盔 / 胸甲 / 护腿 / 靴子），再乘 durabilityMultiplier */
    private static final int[] DURABILITY_BASE = {13, 15, 16, 11};

    private final String assetName;
    private final int durabilityMultiplier;
    private final int[] defense;
    private final int enchantmentValue;
    private final float toughness;
    private final float knockbackResistance;
    private final float[] attackPercents;
    private final Supplier<Item> repairItem;
    private Holder<ArmorMaterial> holder;

    ArmorMaterials(String assetName, int durabilityMultiplier,
                   int helmet, int chestplate, int leggings, int boots,
                   int enchantmentValue, float toughness, float knockbackResistance,
                   float atkHelmet, float atkChestplate, float atkLeggings, float atkBoots,
                   Supplier<Item> repairItem) {
        this.assetName = assetName;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = new int[]{helmet, chestplate, leggings, boots};
        this.enchantmentValue = enchantmentValue;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.attackPercents = new float[]{atkHelmet, atkChestplate, atkLeggings, atkBoots};
        this.repairItem = repairItem;
    }

    private static int index(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 0;
            case CHESTPLATE -> 1;
            case LEGGINGS -> 2;
            case BOOTS, BODY -> 3;
        };
    }

    /** 单件耐久：旧项目是「基数 13/15/16/11 × 倍率」 */
    public int durability(ArmorItem.Type type) {
        return DURABILITY_BASE[index(type)] * durabilityMultiplier;
    }

    /** 该槽位的攻击力百分比加成（0 表示这套不加） */
    public float attackPercent(ArmorItem.Type type) {
        return attackPercents[index(type)];
    }

    /** 材质名，用于 tooltip 分支与调试 */
    public String assetName() {
        return assetName;
    }

    public Holder<ArmorMaterial> holder() {
        if (holder == null) {
            EnumMap<ArmorItem.Type, Integer> def = new EnumMap<>(ArmorItem.Type.class);
            def.put(ArmorItem.Type.HELMET, defense[0]);
            def.put(ArmorItem.Type.CHESTPLATE, defense[1]);
            def.put(ArmorItem.Type.LEGGINGS, defense[2]);
            def.put(ArmorItem.Type.BOOTS, defense[3]);

            ResourceLocation layerId = ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, assetName);
            ArmorMaterial material = new ArmorMaterial(
                    def,
                    enchantmentValue,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(repairItem.get()),
                    List.of(new ArmorMaterial.Layer(layerId)),
                    toughness,
                    knockbackResistance);
            holder = Holder.direct(material);
        }
        return holder;
    }
}
