package com.lazybones.godfalltrinkets.item.custom;

/**
 * 神位碎片：固定进 {@code god_shard_slot}。
 * <p>
 * 「必须先戴破厄之核」这条规则由基类 {@link GodfallTrinketItem#canEquip} 统一负责（所有饰品都要戴核），
 * 这里只负责绑定槽位 + 描述行。
 */
public class GodShardItem extends GodfallTrinketItem {

    public GodShardItem(Properties properties, String name) {
        super(properties, "god_shard_slot",
                new String[]{"tooltip.godfall_trinkets.desc.shard_" + name + ".line1"}, null);
    }
}
