package com.lazybones.godfalltrinkets.item;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.armor.ArmorMaterials;
import com.lazybones.godfalltrinkets.item.custom.ArmorItems;
import com.lazybones.godfalltrinkets.item.custom.AdvancementTallyTrinketItem;
import com.lazybones.godfalltrinkets.item.custom.BrokenCoreItem;
import com.lazybones.godfalltrinkets.item.custom.CurseSlotUnlockItem;
import com.lazybones.godfalltrinkets.item.custom.CurseTransferTrinketItem;
import com.lazybones.godfalltrinkets.item.custom.GodCoreItem;
import com.lazybones.godfalltrinkets.item.custom.GodShardItem;
import com.lazybones.godfalltrinkets.item.custom.GodEvolutionStoneItem;
import com.lazybones.godfalltrinkets.item.custom.GodfallTrinketItem;
import com.lazybones.godfalltrinkets.item.custom.StomachPouchItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * 物品注册。阶段一：材料 + 示例饰品；阶段二起按 1.20.1 的数值补齐真实属性；
 * 其余物品在阶段三逐个迁移。
 */
public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(GodfallTrinkets.MOD_ID);

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, path);
    }

    // ==================== 材料 ====================
    public static final DeferredItem<Item> GOD_INGOT =
            ITEMS.registerItem("god_ingot", Item::new);
    public static final DeferredItem<Item> CONQUEST_INGOT =
            ITEMS.registerItem("conquest_ingot", Item::new);
    public static final DeferredItem<Item> ABYSS_GOD_INGOT =
            ITEMS.registerItem("abyss_god_ingot", Item::new);

    // ==================== 破厄之核 / 神格 / 进化石 ====================
    public static final DeferredItem<BrokenCoreItem> BROKEN_CORE =
            ITEMS.registerItem("broken_core",
                    props -> new BrokenCoreItem(props.stacksTo(1).fireResistant()));

    public static final DeferredItem<GodCoreItem> GOD_CORE =
            ITEMS.registerItem("god_core",
                    props -> new GodCoreItem(props.stacksTo(1).fireResistant()));

    public static final DeferredItem<Item> GOD_EVOLUTION_STONE =
            ITEMS.registerItem("god_evolution_stone",
                    props -> new GodEvolutionStoneItem());

    // ==================== 13 件神位碎片（god_shard_slot） ====================
    private static DeferredItem<GodShardItem> shard(String name) {
        return ITEMS.registerItem("shard_" + name, props -> new GodShardItem(props, name));
    }

    public static final DeferredItem<GodShardItem> SHARD_SKY = shard("sky");
    public static final DeferredItem<GodShardItem> SHARD_SUN = shard("sun");
    public static final DeferredItem<GodShardItem> SHARD_LIFE = shard("life");
    public static final DeferredItem<GodShardItem> SHARD_STRENGTH = shard("strength");
    public static final DeferredItem<GodShardItem> SHARD_FATE = shard("fate");
    public static final DeferredItem<GodShardItem> SHARD_WIND = shard("wind");
    public static final DeferredItem<GodShardItem> SHARD_SPIRIT = shard("spirit");
    public static final DeferredItem<GodShardItem> SHARD_BARRIER = shard("barrier");
    public static final DeferredItem<GodShardItem> SHARD_VOID = shard("void");
    public static final DeferredItem<GodShardItem> SHARD_SHADOW = shard("shadow");
    public static final DeferredItem<GodShardItem> SHARD_THUNDER = shard("thunder");
    public static final DeferredItem<GodShardItem> SHARD_STAR = shard("star");
    public static final DeferredItem<GodShardItem> SHARD_RUNE = shard("rune");

    // ==================== 示例饰品 ====================
    public static final DeferredItem<GodfallTrinketItem> ABYSS_RUNE_RING =
            ITEMS.registerItem("abyss_rune_ring", props -> new GodfallTrinketItem(
                    props, "ring",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.abyss_rune_ring.line1",
                            "tooltip.godfall_trinkets.desc.abyss_rune_ring.line2",
                            "tooltip.godfall_trinkets.desc.abyss_rune_ring.line3"},
                    map -> map.put(Attributes.LUCK,
                            new AttributeModifier(id("abyss_rune_luck"), 0.2,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE))));

    public static final DeferredItem<GodfallTrinketItem> EMBER_BRACELET =
            ITEMS.registerItem("ember_bracelet", props -> new GodfallTrinketItem(
                    props, "bracelet",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.ember_bracelet.line1",
                            "tooltip.godfall_trinkets.desc.ember_bracelet.line2"},
                    null));

    public static final DeferredItem<GodfallTrinketItem> BLUNTING_AMULET =
            ITEMS.registerItem("blunting_amulet", props -> new GodfallTrinketItem(
                    props, "charm",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.blunting_amulet.line1",
                            "tooltip.godfall_trinkets.desc.blunting_amulet.line2"},
                    map -> {
                        // 护甲 +3
                        map.put(Attributes.ARMOR, new AttributeModifier(
                                id("blunting_armor"), 3.0,
                                AttributeModifier.Operation.ADD_VALUE));
                        // 全伤害 +8%
                        map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                                id("blunting_atk"), 0.08,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                    }));

    public static final DeferredItem<GodfallTrinketItem> DEATH_DEFY_RING =
            ITEMS.registerItem("death_defy_ring", props -> new GodfallTrinketItem(
                    props, "ring",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.death_defy_ring.line1",
                            "tooltip.godfall_trinkets.desc.death_defy_ring.line2",
                            "tooltip.godfall_trinkets.desc.death_defy_ring.line3"},
                    null));

    public static final DeferredItem<GodfallTrinketItem> DRAGON_CORE_HEART =
            ITEMS.registerItem("dragon_core_heart", props -> new GodfallTrinketItem(
                    props, "body",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.dragon_core_heart.line1",
                            "tooltip.godfall_trinkets.desc.dragon_core_heart.line2",
                            "tooltip.godfall_trinkets.desc.dragon_core_heart.line3"},
                    null));

    public static final DeferredItem<GodfallTrinketItem> CRYSTAL_CROWN =
            ITEMS.registerItem("crystal_crown", props -> new GodfallTrinketItem(
                    props, "head",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.crystal_crown.line1",
                            "tooltip.godfall_trinkets.desc.crystal_crown.line2",
                            "tooltip.godfall_trinkets.desc.crystal_crown.line3"},
                    map -> {
                        // 护甲 +200%（×3）：对「基础护甲」按倍率加成，装备时自动生效、卸下自动移除
                        map.put(Attributes.ARMOR, new AttributeModifier(
                                id("crystal_crown_armor"), 2.0,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                        // 盔甲韧性 +100%（×2）
                        map.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                                id("crystal_crown_toughness"), 1.0,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                    }));

    // ==================== 阶段 3-3：锻造材料 ====================
    public static final DeferredItem<Item> GODFRAGMENT =
            ITEMS.registerItem("godfragment", props -> new Item(props.stacksTo(64)));
    public static final DeferredItem<Item> WITHER_SHARD =
            ITEMS.registerItem("wither_shard", props -> new Item(props.stacksTo(64)));
    public static final DeferredItem<Item> WARDEN_HEART =
            ITEMS.registerItem("warden_heart", props -> new Item(props.stacksTo(64)));
    public static final DeferredItem<Item> DRAGON_SOUL =
            ITEMS.registerItem("dragon_soul", props -> new Item(props.stacksTo(64)));

    // ==================== 阶段 3-3：九阶长剑 ====================

    /** 1.21 的剑伤害/攻速不再走构造参数，改用 ItemAttributeModifiers 组件 */
    private static DeferredItem<Item> sword(String name, Tier tier, int attackDamage, float attackSpeed) {
        return ITEMS.registerItem(name, props -> new SwordItem(
                tier,
                new Item.Properties()
                        .attributes(SwordItem.createAttributes(tier, attackDamage, attackSpeed))
                        .stacksTo(1)
                        .fireResistant()));
    }

    public static final DeferredItem<Item> WHITE_SWORD_BLANK =
            sword("white_sword_blank", Tiers.WOOD, 3, -1.6F);
    public static final DeferredItem<Item> IRON_WHITE_SWORD =
            sword("iron_white_sword", Tiers.IRON, 7, -2.3F);
    public static final DeferredItem<Item> DIAMOND_WHITE_BLADE =
            sword("diamond_white_blade", Tiers.DIAMOND, 10, -2.1F);
    public static final DeferredItem<Item> SHADOW_SWORD =
            sword("shadow_sword", Tiers.NETHERITE, 14, -2.0F);
    public static final DeferredItem<Item> CORE_CONQUEST_SWORD =
            sword("core_conquest_sword", Tiers.NETHERITE, 18, -1.9F);
    public static final DeferredItem<Item> WAR_BLADE =
            sword("war_blade", Tiers.NETHERITE, 23, -1.7F);
    public static final DeferredItem<Item> WITHER_BLADE =
            sword("wither_blade", Tiers.NETHERITE, 28, -1.6F);
    public static final DeferredItem<Item> WARDEN_SWORD =
            sword("warden_sword", Tiers.NETHERITE, 34, -1.5F);
    public static final DeferredItem<Item> ABYSS_SWORD =
            sword("abyss_sword", Tiers.NETHERITE, 42, -1.4F);
    public static final DeferredItem<Item> END_DRAGON_SWORD =
            sword("end_dragon_sword", Tiers.NETHERITE, 55, -1.2F);

    // ==================== 阶段 3-3：大胃袋 ====================
    public static final DeferredItem<StomachPouchItem> STOMACH_POUCH =
            ITEMS.registerItem("stomach_pouch", props -> new StomachPouchItem(
                    props, "body",
                    new String[]{"tooltip.godfall_trinkets.stomach_pouch.desc"},
                    null));

    // ==================== 阶段 3-3：咒厄转移 ====================
    public static final DeferredItem<Item> CURSE_TRANSFER_BLANK =
            ITEMS.registerItem("curse_transfer_blank", props -> new Item(props.stacksTo(16)));
    public static final DeferredItem<CurseTransferTrinketItem> CURSE_TRANSFER_TRINKET =
            ITEMS.registerItem("curse_transfer_trinket",
                    props -> new CurseTransferTrinketItem(props.durability(1000)));
    public static final DeferredItem<CurseSlotUnlockItem> CURSE_SLOT_UNLOCK_TALISMAN =
            ITEMS.registerItem("curse_slot_unlock_talisman",
                    props -> new CurseSlotUnlockItem(props.stacksTo(1)));

    // ==================== 阶段 3-4：磨刀石 ====================
    public static final DeferredItem<Item> SHARP_EDGE_STONE =
            ITEMS.registerItem("sharp_edge_stone",
                    props -> new Item(props.stacksTo(1).fireResistant()));

    // ==================== 阶段 3-4：功业徽记（badge 饰品） ====================
    public static final DeferredItem<AdvancementTallyTrinketItem> ADVANCEMENT_TALLY_TRINKET =
            ITEMS.registerItem("advancement_tally_trinket",
                    props -> new AdvancementTallyTrinketItem(props));

    // ==================== 阶段 3-4：神陨锻造模板 ====================
    public static final DeferredItem<Item> GOD_TEMPLATE =
            ITEMS.registerItem("god_template", props -> new SmithingTemplateItem(
                    // appliesTo：适用装备
                    Component.translatable("tooltip.god_template.apply").withStyle(ChatFormatting.BLUE),
                    // ingredients：所需材料
                    Component.translatable("tooltip.god_template.material").withStyle(ChatFormatting.BLUE),
                    // title：锻造台标题
                    Component.translatable("upgrade.god_alloy"),
                    // 基底槽说明
                    Component.translatable("tooltip.god_template.desc").withStyle(ChatFormatting.GRAY),
                    // 材料槽说明
                    Component.translatable("item.godfall_trinkets.god_template"),
                    List.of(ResourceLocation.withDefaultNamespace("item/smithing_template_armor_slot")),
                    List.of(ResourceLocation.withDefaultNamespace("item/smithing_template_material_slot"))));

    // ==================== 阶段 3-4：三档神装护甲 ×4 件 ====================
    private static DeferredItem<ArmorItems> armor(String name, ArmorMaterials set, ArmorItem.Type type) {
        return ITEMS.registerItem(name, props -> new ArmorItems(set, type));
    }

    // 神陨诅咒耐受套
    public static final DeferredItem<ArmorItems> GOD_HELMET =
            armor("god_helmet", ArmorMaterials.GOD_INGOT, ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItems> GOD_CHESTPLATE =
            armor("god_chestplate", ArmorMaterials.GOD_INGOT, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItems> GOD_LEGGINGS =
            armor("god_leggings", ArmorMaterials.GOD_INGOT, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItems> GOD_BOOTS =
            armor("god_boots", ArmorMaterials.GOD_INGOT, ArmorItem.Type.BOOTS);

    // 征伐战斗套
    public static final DeferredItem<ArmorItems> CONQUEST_HELMET =
            armor("conquest_helmet", ArmorMaterials.CONQUEST_INGOT, ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItems> CONQUEST_CHESTPLATE =
            armor("conquest_chestplate", ArmorMaterials.CONQUEST_INGOT, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItems> CONQUEST_LEGGINGS =
            armor("conquest_leggings", ArmorMaterials.CONQUEST_INGOT, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItems> CONQUEST_BOOTS =
            armor("conquest_boots", ArmorMaterials.CONQUEST_INGOT, ArmorItem.Type.BOOTS);

    // 深渊征伐战套
    public static final DeferredItem<ArmorItems> ABYSS_GOD_HELMET =
            armor("abyss_god_helmet", ArmorMaterials.ABYSS_GOD_INGOT, ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItems> ABYSS_GOD_CHESTPLATE =
            armor("abyss_god_chestplate", ArmorMaterials.ABYSS_GOD_INGOT, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItems> ABYSS_GOD_LEGGINGS =
            armor("abyss_god_leggings", ArmorMaterials.ABYSS_GOD_INGOT, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItems> ABYSS_GOD_BOOTS =
            armor("abyss_god_boots", ArmorMaterials.ABYSS_GOD_INGOT, ArmorItem.Type.BOOTS);

    // ==================== 凋零四件极限考验饰品 ====================
    // 枯骨之心：body 槽
    public static final DeferredItem<GodfallTrinketItem> WITHER_HEART =
            ITEMS.registerItem("wither_heart", props -> new GodfallTrinketItem(
                    props, "body",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.wither_heart.line1",
                            "tooltip.godfall_trinkets.desc.wither_heart.line2"},
                    null));

    // 王者之握：bracelet 槽
    public static final DeferredItem<GodfallTrinketItem> KINGS_GRIP =
            ITEMS.registerItem("kings_grip", props -> new GodfallTrinketItem(
                    props, "bracelet",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.kings_grip.line1"},
                    null));

    // 凋零王冠：head 槽
    public static final DeferredItem<GodfallTrinketItem> WITHER_CROWN =
            ITEMS.registerItem("wither_crown", props -> new GodfallTrinketItem(
                    props, "head",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.wither_crown.line1",
                            "tooltip.godfall_trinkets.desc.wither_crown.line2"},
                    null));

    // 凋零脊骨坠：necklace 槽
    public static final DeferredItem<GodfallTrinketItem> WITHER_SPINE =
            ITEMS.registerItem("wither_spine", props -> new GodfallTrinketItem(
                    props, "necklace",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.wither_spine.line1"},
                    null));

    // ==================== 监守者四件极限反差饰品 ====================
    // 幽匿之眼：ring 槽
    public static final DeferredItem<GodfallTrinketItem> WARDEN_EYE =
            ITEMS.registerItem("warden_eye", props -> new GodfallTrinketItem(
                    props, "ring",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.warden_eye.line1"},
                    null));

    // 幽匿脉动：ring 槽
    public static final DeferredItem<GodfallTrinketItem> WARDEN_PULSE =
            ITEMS.registerItem("warden_pulse", props -> new GodfallTrinketItem(
                    props, "ring",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.warden_pulse.line1"},
                    null));

    // 幽匿重压：body 槽
    public static final DeferredItem<GodfallTrinketItem> WARDEN_WEIGHT =
            ITEMS.registerItem("warden_weight", props -> new GodfallTrinketItem(
                    props, "body",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.warden_weight.line1"},
                    null));

    // 幽匿破甲：bracelet 槽
    public static final DeferredItem<GodfallTrinketItem> WARDEN_ARMORBREAK =
            ITEMS.registerItem("warden_armorbreak", props -> new GodfallTrinketItem(
                    props, "bracelet",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.warden_armorbreak.line1"},
                    null));

    // ==================== 震斥护符（charm） ====================
    // 抗击退 18%（旧 MULTIPLY_BASE → 1.21 ADD_MULTIPLIED_BASE）
    public static final DeferredItem<GodfallTrinketItem> SHOCK_AMULET =
            ITEMS.registerItem("shock_amulet", props -> new GodfallTrinketItem(
                    props, "charm",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.shock_amulet.line1",
                            "tooltip.godfall_trinkets.desc.shock_amulet.line2",
                            "tooltip.godfall_trinkets.desc.shock_amulet.line3"},
                    map -> map.put(Attributes.KNOCKBACK_RESISTANCE,
                            new AttributeModifier(id("shock_amulet_kb"), 0.18,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE))));

    // ==================== 末影龙终焉龙饰 ====================
    // 终末之眼：charm 槽
    public static final DeferredItem<GodfallTrinketItem> EYE_OF_THE_END =
            ITEMS.registerItem("eye_of_the_end", props -> new GodfallTrinketItem(
                    props, "charm",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.eye_of_the_end.line1",
                            "tooltip.godfall_trinkets.desc.eye_of_the_end.line2",
                            "tooltip.godfall_trinkets.desc.eye_of_the_end.line3"},
                    null));

    // 寂灭龙脊：back 槽
    public static final DeferredItem<GodfallTrinketItem> DRAGON_SPINE_END =
            ITEMS.registerItem("dragon_spine_end", props -> new GodfallTrinketItem(
                    props, "back",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.dragon_spine_end.line1",
                            "tooltip.godfall_trinkets.desc.dragon_spine_end.line2",
                            "tooltip.godfall_trinkets.desc.dragon_spine_end.line3",
                            "tooltip.godfall_trinkets.desc.dragon_spine_end.line4"},
                    null));

    // ==================== 阶段四：基础饰品补齐（1~13） ====================
    // 1 韧骨护符：+3 最大生命、+2 护甲
    public static final DeferredItem<GodfallTrinketItem> TOUGH_BONE_AMULET =
            ITEMS.registerItem("tough_bone_amulet", props -> new GodfallTrinketItem(
                    props, "charm",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.tough_bone_amulet.line1",
                            "tooltip.godfall_trinkets.desc.tough_bone_amulet.line2"},
                    map -> {
                        map.put(Attributes.MAX_HEALTH, new AttributeModifier(
                                id("tough_bone_hp"), 3.0, AttributeModifier.Operation.ADD_VALUE));
                        map.put(Attributes.ARMOR, new AttributeModifier(
                                id("tough_bone_armor"), 2.0, AttributeModifier.Operation.ADD_VALUE));
                    }));

    // 2 锐锋残戒：全伤害 +12%
    public static final DeferredItem<GodfallTrinketItem> SHARP_EDGE_RING =
            ITEMS.registerItem("sharp_edge_ring", props -> new GodfallTrinketItem(
                    props, "ring",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.sharp_edge_ring.line1"},
                    map -> map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                            id("sharp_edge_atk"), 0.12,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE))));

    // 3 清秽腕饰：20 秒清一层负面
    public static final DeferredItem<GodfallTrinketItem> PURIFYING_BRACELET =
            ITEMS.registerItem("purifying_bracelet", props -> new GodfallTrinketItem(
                    props, "bracelet",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.purifying_bracelet.line1"},
                    null));

    // 4 噬魂血戒：15% 吸血 + 击杀回 3
    public static final DeferredItem<GodfallTrinketItem> SOUL_DRAIN_RING =
            ITEMS.registerItem("soul_drain_ring", props -> new GodfallTrinketItem(
                    props, "ring",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.soul_drain_ring.line1",
                            "tooltip.godfall_trinkets.desc.soul_drain_ring.line2"},
                    null));

    // 5 生纹护符：3/秒回复 + 受击回 2
    public static final DeferredItem<GodfallTrinketItem> VITAL_AMULET =
            ITEMS.registerItem("vital_amulet", props -> new GodfallTrinketItem(
                    props, "charm",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.vital_amulet.line1",
                            "tooltip.godfall_trinkets.desc.vital_amulet.line2"},
                    null));

    // 6 韧心指环：+4 最大生命
    public static final DeferredItem<GodfallTrinketItem> TOUGH_HEART_RING =
            ITEMS.registerItem("tough_heart_ring", props -> new GodfallTrinketItem(
                    props, "ring",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.tough_heart_ring.line1",
                            "tooltip.godfall_trinkets.desc.tough_heart_ring.line2"},
                    map -> map.put(Attributes.MAX_HEALTH, new AttributeModifier(
                            id("tough_heart_hp"), 4.0, AttributeModifier.Operation.ADD_VALUE))));

    // 7 壁垒腕：举盾免疫投射物 / 爆炸减伤 / 举盾回血
    public static final DeferredItem<GodfallTrinketItem> BARRIER_BRACELET =
            ITEMS.registerItem("barrier_bracelet", props -> new GodfallTrinketItem(
                    props, "bracelet",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.barrier_bracelet.line1",
                            "tooltip.godfall_trinkets.desc.barrier_bracelet.line2",
                            "tooltip.godfall_trinkets.desc.barrier_bracelet.line3"},
                    null));

    // 8 追猎戒：对高血量目标 +16%
    public static final DeferredItem<GodfallTrinketItem> HUNTER_RING =
            ITEMS.registerItem("hunter_ring", props -> new GodfallTrinketItem(
                    props, "ring",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.hunter_ring.line1",
                            "tooltip.godfall_trinkets.desc.hunter_ring.line2"},
                    null));

    // 9 沃壤芽镯：作物生长 / 双倍掉落
    public static final DeferredItem<GodfallTrinketItem> FERTILE_BRACELET =
            ITEMS.registerItem("fertile_bracelet", props -> new GodfallTrinketItem(
                    props, "bracelet",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.fertile_bracelet.line1",
                            "tooltip.godfall_trinkets.desc.fertile_bracelet.line2"},
                    null));

    // 10 熔渊锻符：烧制/铁砧/耐久
    public static final DeferredItem<GodfallTrinketItem> FORGE_AMULET =
            ITEMS.registerItem("forge_amulet", props -> new GodfallTrinketItem(
                    props, "charm",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.forge_amulet.line1",
                            "tooltip.godfall_trinkets.desc.forge_amulet.line2",
                            "tooltip.godfall_trinkets.desc.forge_amulet.line3"},
                    null));

    // 11 深岩寻矿戒：+15% 攻击速度（沿用旧属性表）、双倍原矿、免疫疲劳
    public static final DeferredItem<GodfallTrinketItem> DEEP_ROCK_RING =
            ITEMS.registerItem("deep_rock_ring", props -> new GodfallTrinketItem(
                    props, "ring",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.deep_rock_ring.line1",
                            "tooltip.godfall_trinkets.desc.deep_rock_ring.line2",
                            "tooltip.godfall_trinkets.desc.deep_rock_ring.line3"},
                    map -> map.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                            id("deep_rock_dig"), 0.15,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE))));

    // 12 拾珍护符：幸运 +20%
    public static final DeferredItem<GodfallTrinketItem> TREASURE_AMULET =
            ITEMS.registerItem("treasure_amulet", props -> new GodfallTrinketItem(
                    props, "charm",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.treasure_amulet.line1",
                            "tooltip.godfall_trinkets.desc.treasure_amulet.line2"},
                    map -> map.put(Attributes.LUCK, new AttributeModifier(
                            id("treasure_luck"), 0.2,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE))));

    // 13 繁生腕镯：幼崽生长 / 繁殖冷却 / 双倍肉
    public static final DeferredItem<GodfallTrinketItem> PROSPERITY_BRACELET =
            ITEMS.registerItem("prosperity_bracelet", props -> new GodfallTrinketItem(
                    props, "bracelet",
                    new String[]{
                            "tooltip.godfall_trinkets.desc.prosperity_bracelet.line1",
                            "tooltip.godfall_trinkets.desc.prosperity_bracelet.line2",
                            "tooltip.godfall_trinkets.desc.prosperity_bracelet.line3"},
                    null));
}
