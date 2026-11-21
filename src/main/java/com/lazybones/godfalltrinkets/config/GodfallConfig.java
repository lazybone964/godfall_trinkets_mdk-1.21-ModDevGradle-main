package com.lazybones.godfalltrinkets.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 模组配置（阶段三先搬「模块一：破厄之核 / 神格」，其余模块等各自阶段再补）。
 * <p>
 * 1.20.1 → 1.21.1：Forge 的 {@code ForgeConfigSpec} 换成 NeoForge 的 {@link ModConfigSpec}，
 * 在 {@code GodfallTrinkets} 构造里用 {@code modContainer.registerConfig(ModConfig.Type.COMMON, SPEC)} 注册。
 * 数值默认值全部照抄旧配置。
 */
public final class GodfallConfig {

    // ==================== 模块一：破厄之核 / 神格核心养成 ====================

    public static final ModConfigSpec.BooleanValue AUTO_GIVE_BROKEN_CORE;
    public static final ModConfigSpec.DoubleValue CORE_EXP_MULTIPLIER;
    public static final ModConfigSpec.IntValue[] TIER_EXP = new ModConfigSpec.IntValue[10];
    public static final ModConfigSpec.IntValue SHIELD_MAX_BASE;
    public static final ModConfigSpec.IntValue SHIELD_COOLDOWN_TICK;
    public static final ModConfigSpec.IntValue REVERSE_CURSE_TIER;
    public static final ModConfigSpec.BooleanValue AUTO_GOD_UPGRADE_STONE;
    public static final ModConfigSpec.DoubleValue GOD_CORE_SHARD_POWER_MOD;
    public static final ModConfigSpec.DoubleValue GOD_CORE_ALL_ATTR_ADD;
    public static final ModConfigSpec.DoubleValue GOD_CORE_GLOBAL_DAMAGE_UP;
    public static final ModConfigSpec.DoubleValue GOD_CORE_DAMAGE_REDUCE;
    public static final ModConfigSpec.BooleanValue GOD_IMMUNE_ALL_ENV_DAMAGE;
    public static final ModConfigSpec.DoubleValue MAX_SINGLE_DAMAGE_RATIO;
    public static final ModConfigSpec.IntValue INVULNERABLE_TICK;
    public static final ModConfigSpec.DoubleValue CREATIVE_FLY_SPEED_MOD;
    public static final ModConfigSpec.DoubleValue CREATIVE_FLY_DAMAGE_MOD;
    public static final ModConfigSpec.BooleanValue ENABLE_ARMOR_PIERCE;
    public static final ModConfigSpec.IntValue BROKEN_CORE_REGEN_AMPLIFIER;
    public static final ModConfigSpec.IntValue BROKEN_CORE_REGEN_DURATION_TICKS;

    // ==================== 模块六：全局功能总开关（核心相关部分） ====================

    public static final ModConfigSpec.BooleanValue ENABLE_CURSE_SYSTEM;
    public static final ModConfigSpec.BooleanValue ENABLE_ABYSS_VISION;
    public static final ModConfigSpec.BooleanValue ENABLE_BREAKTHROUGH_SOUND;
    public static final ModConfigSpec.BooleanValue ENABLE_BREAKTHROUGH_NOTICE;
    public static final ModConfigSpec.BooleanValue ENABLE_GOD_CORE_EVOLUTION;

    // ==================== 模块二：13 件神位碎片 ====================

    public static final String[] SHARD_NAMES = {
            "sky", "sun", "life", "strength", "fate", "wind", "spirit",
            "barrier", "void", "shadow", "thunder", "star", "rune"
    };
    public static final ModConfigSpec.DoubleValue SHARD_GLOBAL_POWER;
    public static final ModConfigSpec.BooleanValue[] SHARD_ENABLE = new ModConfigSpec.BooleanValue[13];
    public static final ModConfigSpec.DoubleValue[] SHARD_MOD = new ModConfigSpec.DoubleValue[13];
    public static final ModConfigSpec.IntValue SHARD_LIFE_REVIVE_CD_SECONDS;
    public static final ModConfigSpec.DoubleValue SHARD_THUNDER_LIGHTNING_CHANCE;
    public static final ModConfigSpec.DoubleValue SHARD_SHADOW_BACKSTAB_MULT;
    public static final ModConfigSpec.IntValue SHARD_RUNE_ENCHANT_LEVEL_BONUS;
    public static final ModConfigSpec.DoubleValue SHARD_SUN_DAY_REGEN_SPEED;
    public static final ModConfigSpec.DoubleValue SHARD_DROP_RATE_MOD;
    public static final ModConfigSpec.BooleanValue ENDER_DRAGON_SKY_SHARD_GUARANTEED;
    public static final ModConfigSpec.BooleanValue SHOW_SHARD_COLLECTION_TIP;
    public static final List<ModConfigSpec.ConfigValue<List<? extends String>>> SHARD_DROP_MOBS =
            new ArrayList<>();
    public static final List<ModConfigSpec.ConfigValue<List<? extends Double>>> SHARD_DROP_CHANCES =
            new ArrayList<>();

    private static final String[][] SHARD_DROP_MOB_DEFAULTS = {
            {"minecraft:ender_dragon"},
            {"minecraft:blaze", "minecraft:magma_cube"},
            {"minecraft:warden"},
            {"minecraft:piglin_brute"},
            {},
            {"minecraft:goat"},
            {"minecraft:witch"},
            {"minecraft:iron_golem"},
            {"minecraft:shulker"},
            {"minecraft:warden"},
            {"minecraft:creeper:powered", "minecraft:pillager:captain"},
            {"minecraft:skeleton", "minecraft:shulker"},
            {}
    };

    private static final double[][] SHARD_DROP_CHANCE_DEFAULTS = {
            {1.0},                // sky：末影龙必掉（唯一来源）
            {0.25, 0.25},         // sun：烈焰人 / 岩浆怪
            {0.5},                // life：监守者 50%
            {0.25},               // strength：猪灵蛮兵
            {},                   // fate：无怪物掉落
            {0.25},               // wind：山羊
            {0.25},               // spirit：女巫
            {0.25},               // barrier：铁傀儡
            {0.25},               // void：潜影贝
            {0.5},                // shadow：监守者 50%
            {0.25, 0.25},         // thunder：带电苦力怕 / 掠夺者队长
            {0.25, 0.25},         // star：骷髅 / 潜影贝
            {}                    // rune：无怪物掉落
    };

    // ==================== 模块三：九阶长剑 ====================

    public static final ModConfigSpec.DoubleValue SWORD_GLOBAL_DAMAGE;
    public static final ModConfigSpec.IntValue SWORD_FORGE_MAX_PROGRESS;
    public static final ModConfigSpec.DoubleValue SWORD_EXP_BONUS;
    public static final ModConfigSpec.BooleanValue SWORD_SPAWN_ENABLE;
    public static final ModConfigSpec.DoubleValue SWORD_UNDEAD_DMG;
    public static final ModConfigSpec.DoubleValue SWORD_NETHER_DMG;
    public static final ModConfigSpec.DoubleValue SWORD_MOB_DMG;
    public static final ModConfigSpec.DoubleValue SWORD_WITHER_DMG;
    public static final ModConfigSpec.DoubleValue SWORD_DARK_DMG;
    public static final ModConfigSpec.DoubleValue SWORD_BOSS_DMG;
    public static final ModConfigSpec.DoubleValue SWORD_T9_BOSS_DMG;
    public static final ModConfigSpec.DoubleValue SWORD_T9_ELYTRA_DMG;
    public static final ModConfigSpec.DoubleValue SWORD_LIFESTEAL_MOD;

    // ==================== 模块四：大胃袋 ====================

    public static final ModConfigSpec.IntValue STOMACH_MAX_HUNGER;
    public static final ModConfigSpec.DoubleValue STOMACH_MAX_SATURATION;
    public static final ModConfigSpec.DoubleValue STOMACH_DEATH_DRAIN_RATIO;
    public static final ModConfigSpec.DoubleValue STOMACH_FULL_DMG_BONUS;
    public static final ModConfigSpec.DoubleValue STOMACH_FULL_MAXHP_BONUS;
    public static final ModConfigSpec.DoubleValue STOMACH_FULL_ARMOR_BONUS;
    public static final ModConfigSpec.DoubleValue STOMACH_FULL_TOUGH_BONUS;
    public static final ModConfigSpec.DoubleValue STOMACH_FULL_KNOCKBACK_RESIST;
    public static final ModConfigSpec.DoubleValue STOMACH_FULL_LUCK_BONUS;
    public static final ModConfigSpec.DoubleValue STOMACH_FULL_ATKSPEED_BONUS;

    // ==================== 模块五：终末龙征 / 鞘翅加成 ====================

    public static final ModConfigSpec.BooleanValue ENABLE_ELYTRA_BONUS;

    public static final ModConfigSpec SPEC;

    private static final int[] DEFAULT_TIER_EXP = {0, 300, 800, 1800, 3500, 6000, 10000, 16000, 25000, 40000};

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("1. 破厄之核_神格核心养成");
        AUTO_GIVE_BROKEN_CORE = b.comment("新玩家上线是否自动发放破厄之核")
                .define("auto_give_broken_core", true);
        CORE_EXP_MULTIPLIER = b.comment("击杀怪物获取破厄经验倍率，0=关闭")
                .defineInRange("core_exp_multiplier", 1.0, 0.0, 100.0);
        for (int i = 0; i < 10; i++) {
            TIER_EXP[i] = b.comment("境界 " + i + " 累计所需经验")
                    .defineInRange("tier_exp_" + i, DEFAULT_TIER_EXP[i], 0, Integer.MAX_VALUE);
        }
        SHIELD_MAX_BASE = b.comment("破厄基础护盾容量").defineInRange("shield_max_base", 3, 0, 100000);
        SHIELD_COOLDOWN_TICK = b.comment("护盾刷新冷却刻数（20刻=1秒）")
                .defineInRange("shield_cooldown_tick", 25, 1, 12000);
        REVERSE_CURSE_TIER = b.comment("多少阶反转诅咒（原版9阶，可改为7/10阶）")
                .defineInRange("reverse_curse_tier", 9, 0, 10);
        AUTO_GOD_UPGRADE_STONE = b.comment("集齐13碎片是否自动发放进化石")
                .define("auto_god_upgrade_stone", true);
        GOD_CORE_SHARD_POWER_MOD = b.comment("神核加持下所有神位碎片效果倍率")
                .defineInRange("god_core_shard_power_mod", 1.5, 0.0, 100.0);
        GOD_CORE_ALL_ATTR_ADD = b.comment("神格额外全属性加成（0.35=35%）")
                .defineInRange("god_core_all_attr_add", 0.35, 0.0, 100.0);
        GOD_CORE_GLOBAL_DAMAGE_UP = b.comment("全局伤害增加比例（0.20=20%）")
                .defineInRange("god_core_global_damage_up", 0.20, 0.0, 100.0);
        GOD_CORE_DAMAGE_REDUCE = b.comment("受到伤害减免比例（0.25=25%）")
                .defineInRange("god_core_damage_reduce", 0.25, 0.0, 1.0);
        GOD_IMMUNE_ALL_ENV_DAMAGE = b.comment("是否开启全套环境伤害免疫（火/摔/岩浆/雷击等）")
                .define("god_immune_all_env_damage", true);
        MAX_SINGLE_DAMAGE_RATIO = b.comment("单次受伤上限占自身最大生命值百分比（0.30=30%）")
                .defineInRange("max_single_damage_ratio", 0.30, 0.0, 1.0);
        INVULNERABLE_TICK = b.comment("挨打无敌持续刻数").defineInRange("invulnerable_tick", 30, 0, 200);
        CREATIVE_FLY_SPEED_MOD = b.comment("创造飞行移速增幅")
                .defineInRange("creative_fly_speed_mod", 0.15, 0.0, 1.0);
        CREATIVE_FLY_DAMAGE_MOD = b.comment("创造飞行状态增伤比例（0.30=30%）")
                .defineInRange("creative_fly_damage_mod", 0.30, 0.0, 100.0);
        ENABLE_ARMOR_PIERCE = b.comment("攻击是否无视药水抗性（破甲效果见 GodCoreEvents 注释）")
                .define("enable_armor_pierce", true);
        BROKEN_CORE_REGEN_AMPLIFIER = b.comment("破厄之核佩戴给予再生buff的等级(放大器，0=再生I，9=再生X)")
                .defineInRange("broken_core_regen_amplifier", 9, 0, 255);
        BROKEN_CORE_REGEN_DURATION_TICKS = b.comment("破厄之核佩戴再生buff持续时间，单位刻(20刻=1秒)")
                .defineInRange("broken_core_regen_duration_ticks", 72000, 0, Integer.MAX_VALUE);
        b.pop();

        b.push("6. 全局功能总开关");
        ENABLE_CURSE_SYSTEM = b.comment("完全关闭诅咒/祝福体系").define("enable_curse_system", true);
        ENABLE_ABYSS_VISION = b.comment("是否开启深渊夜视感知").define("enable_abyss_vision", true);
        ENABLE_BREAKTHROUGH_SOUND = b.comment("境界突破音效开关").define("enable_breakthrough_sound", true);
        ENABLE_BREAKTHROUGH_NOTICE = b.comment("境界突破弹窗提示开关").define("enable_breakthrough_notice", true);
        ENABLE_GOD_CORE_EVOLUTION = b.comment("彻底关闭破厄进化神核功能")
                .define("enable_god_core_evolution", true);
        b.pop();

        b.push("2. 神位碎片_13件God Shard");
        SHARD_GLOBAL_POWER = b.comment("所有神位碎片基础效果统一倍率")
                .defineInRange("shard_global_power", 1.0, 0.0, 100.0);
        for (int i = 0; i < 13; i++) {
            String n = SHARD_NAMES[i];
            SHARD_ENABLE[i] = b.comment("是否启用「" + n + "」碎片功能")
                    .define("shard_" + n + "_enable", true);
            SHARD_MOD[i] = b.comment("「" + n + "」碎片效果倍率")
                    .defineInRange("shard_" + n + "_mod", 1.0, 0.0, 100.0);
        }
        SHARD_LIFE_REVIVE_CD_SECONDS = b.comment("生命碎片复活冷却秒数")
                .defineInRange("shard_life_revive_cd_seconds", 60, 1, 86400);
        SHARD_THUNDER_LIGHTNING_CHANCE = b.comment("御雷碎片落雷概率（0.20=20%）")
                .defineInRange("shard_thunder_lightning_chance", 0.20, 0.0, 1.0);
        SHARD_SHADOW_BACKSTAB_MULT = b.comment("影杀碎片背击伤害倍率（2.0=200%）")
                .defineInRange("shard_shadow_backstab_mult", 2.0, 0.0, 100.0);
        SHARD_RUNE_ENCHANT_LEVEL_BONUS = b.comment("铭纹碎片附魔等级加成数值")
                .defineInRange("shard_rune_enchant_level_bonus", 5, 0, 100);
        SHARD_SUN_DAY_REGEN_SPEED = b.comment("烈阳碎片白天回血速度（每tick治疗量）")
                .defineInRange("shard_sun_day_regen_speed", 0.0, 0.0, 100.0);
        SHARD_DROP_RATE_MOD = b.comment("全部神位碎片掉落总倍率")
                .defineInRange("shard_drop_rate_mod", 1.0, 0.0, 100.0);
        ENDER_DRAGON_SKY_SHARD_GUARANTEED = b.comment("末影龙是否必掉天穹碎片")
                .define("ender_dragon_sky_shard_guaranteed", true);
        SHOW_SHARD_COLLECTION_TIP = b.comment("集齐碎片提示弹窗开关")
                .define("show_shard_collection_tip", true);

        for (int i = 0; i < 13; i++) {
            String n = SHARD_NAMES[i];
            List<String> mobList = new ArrayList<>(Arrays.asList(SHARD_DROP_MOB_DEFAULTS[i]));
            List<Double> chanceList = new ArrayList<>();
            for (double c : SHARD_DROP_CHANCE_DEFAULTS[i]) {
                chanceList.add(c);
            }
            SHARD_DROP_MOBS.add(b.comment("「" + n + "」碎片击杀掉落怪物"
                            + "（注册名，:powered=带电苦力怕，:captain=掠夺者队长）")
                    .defineListAllowEmpty("shard_" + n + "_drop_mobs", mobList,
                            o -> o instanceof String));
            SHARD_DROP_CHANCES.add(b.comment("「" + n + "」碎片掉落概率（与上方怪物一一对应）")
                    .defineListAllowEmpty("shard_" + n + "_drop_chances", chanceList,
                            o -> o instanceof Double));
        }
        b.pop();

        b.push("3. 九阶进阶长剑");
        SWORD_GLOBAL_DAMAGE = b.comment("全部长剑统一伤害缩放倍率")
                .defineInRange("sword_global_damage", 1.0, 0.0, 100.0);
        SWORD_FORGE_MAX_PROGRESS = b.comment("铁砧锻造单次满进度需求")
                .defineInRange("sword_forge_max_progress", 20, 1, 1000);
        SWORD_EXP_BONUS = b.comment("长剑击杀经验加成倍率")
                .defineInRange("sword_exp_bonus", 1.0, 0.0, 100.0);
        SWORD_SPAWN_ENABLE = b.comment("是否允许世界自然生成长剑（关闭只能锻造）")
                .define("sword_spawn_enable", false);
        SWORD_UNDEAD_DMG = b.comment("亡灵生物增伤倍率（1.30=30%）")
                .defineInRange("sword_undead_dmg", 1.30, 1.0, 100.0);
        SWORD_NETHER_DMG = b.comment("下界生物增伤倍率（1.35=35%）")
                .defineInRange("sword_nether_dmg", 1.35, 1.0, 100.0);
        SWORD_MOB_DMG = b.comment("普通怪物增伤倍率（1.25=25%）")
                .defineInRange("sword_mob_dmg", 1.25, 1.0, 100.0);
        SWORD_WITHER_DMG = b.comment("凋零类增伤倍率（1.45=45%）")
                .defineInRange("sword_wither_dmg", 1.45, 1.0, 100.0);
        SWORD_DARK_DMG = b.comment("黑暗环境增伤倍率（1.40=40%）")
                .defineInRange("sword_dark_dmg", 1.40, 1.0, 100.0);
        SWORD_BOSS_DMG = b.comment("Boss 增伤倍率（1.40=40%）")
                .defineInRange("sword_boss_dmg", 1.40, 1.0, 100.0);
        SWORD_T9_BOSS_DMG = b.comment("终末龙征剑 Boss/末地生物增伤倍率（1.60=60%）")
                .defineInRange("sword_t9_boss_dmg", 1.60, 1.0, 100.0);
        SWORD_T9_ELYTRA_DMG = b.comment("鞘翅飞行增伤倍率（1.30=30%）")
                .defineInRange("sword_t9_elytra_dmg", 1.30, 1.0, 100.0);
        SWORD_LIFESTEAL_MOD = b.comment("长剑吸血倍率（1.0=原版）")
                .defineInRange("sword_lifesteal_mod", 1.0, 0.0, 100.0);
        b.pop();

        b.push("4. 大胃袋");
        STOMACH_MAX_HUNGER = b.comment("大胃袋最大可储存饥饿点数")
                .defineInRange("stomach_max_hunger", 200, 0, 10000);
        STOMACH_MAX_SATURATION = b.comment("大胃袋最大可储存饱和度点数")
                .defineInRange("stomach_max_saturation", 100.0, 0.0, 10000.0);
        STOMACH_DEATH_DRAIN_RATIO = b.comment("玩家死亡时，佩戴中大胃袋扣除储存量比例；0.4=扣除40%，保留60%；0=死亡不扣任何存量")
                .defineInRange("stomach_death_drain_ratio", 0.4, 0.0, 1.0);
        STOMACH_FULL_DMG_BONUS = b.comment("储存拉满时 全伤害加成倍率，0.6 = +60%")
                .defineInRange("stomach_full_dmg_bonus", 0.60, 0.0, 5.0);
        STOMACH_FULL_MAXHP_BONUS = b.comment("储存拉满时 生命上限加成倍率，1.0 = +100%")
                .defineInRange("stomach_full_maxhp_bonus", 1.00, 0.0, 5.0);
        STOMACH_FULL_ARMOR_BONUS = b.comment("储存拉满时 护甲百分比加成倍率，1.0 = +100%")
                .defineInRange("stomach_full_armor_bonus", 1.00, 0.0, 5.0);
        STOMACH_FULL_TOUGH_BONUS = b.comment("储存拉满时 护甲韧性百分比加成倍率，1.0 = +100%")
                .defineInRange("stomach_full_tough_bonus", 1.00, 0.0, 5.0);
        STOMACH_FULL_KNOCKBACK_RESIST = b.comment("储存拉满时 击退抗性上限，0.85=85%，最大不要超过1.0")
                .defineInRange("stomach_full_knockback_resist", 0.85, 0.0, 1.0);
        STOMACH_FULL_LUCK_BONUS = b.comment("储存拉满时 幸运百分比加成倍率，2.0 = +200%")
                .defineInRange("stomach_full_luck_bonus", 2.00, 0.0, 5.0);
        STOMACH_FULL_ATKSPEED_BONUS = b.comment("储存拉满时 攻击速度百分比加成倍率，1.5 = +150%")
                .defineInRange("stomach_full_atkspeed_bonus", 1.50, 0.0, 5.0);
        b.pop();

        b.push("5. 终末龙征");
        ENABLE_ELYTRA_BONUS = b.comment("终末龙征剑鞘翅飞行增伤是否启用")
                .define("enable_elytra_bonus", true);
        b.pop();

        SPEC = b.build();
    }

    private GodfallConfig() {
    }

    /** 10 个境界的累计经验阈值（运行时读配置） */
    public static int[] tierExp() {
        int[] arr = new int[10];
        for (int i = 0; i < 10; i++) {
            arr[i] = TIER_EXP[i].get();
        }
        return arr;
    }
}
