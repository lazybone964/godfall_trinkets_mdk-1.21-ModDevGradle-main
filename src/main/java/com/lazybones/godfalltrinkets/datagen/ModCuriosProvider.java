package com.lazybones.godfalltrinkets.datagen;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import top.theillusivec4.curios.api.CuriosDataProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Curios 槽位数据生成器：产出 data/godfall_trinkets/curios/slots/*.json 与 curios/entities/*.json。
 * 槽位图标位于 assets/godfall_trinkets/textures/slot/*.png。
 */
public class ModCuriosProvider extends CuriosDataProvider {

    public ModCuriosProvider(PackOutput output, ExistingFileHelper fileHelper,
                             CompletableFuture<HolderLookup.Provider> registries) {
        super(GodfallTrinkets.MOD_ID, output, fileHelper, registries);
    }

    @Override
    public void generate(HolderLookup.Provider registries, ExistingFileHelper fileHelper) {
        // 破厄之核专属槽位
        createSlot("broken_core_slot")
                .size(1)
                .icon(icon("broken_core"))
                .order(0)
                .renderToggle(true);

        // 神位碎片槽位（裸配 0 格，戴破厄之核 / 神格后 13 格；
        // 格数由 TrinketSlotSyncEvents 写进槽位的 baseSize，不靠槽位修饰符）
        createSlot("god_shard_slot")
                .size(0)
                .icon(icon("god_shard"))
                .order(1)
                .renderToggle(true);

        // 护符槽（基础 2 格，戴破厄之核后 5 格：2 + 核解锁 3）
        createSlot("charm")
                .size(2)
                .icon(icon("charm"))
                .order(4)
                .renderToggle(true);

        createSlot("ring")
                .size(2)
                .icon(icon("ring"))
                .order(2)
                .renderToggle(true);

        createSlot("bracelet")
                .size(2)
                .icon(icon("bracelet"))
                .order(3)
                .renderToggle(true);

        createSlot("curse_transfer")
                .size(0)
                .icon(icon("curse_transfer"))
                .order(5)
                .renderToggle(true);

        createSlot("head")
                .size(1)
                .icon(icon("head"))
                .order(6)
                .renderToggle(true);

        createSlot("back")
                .size(1)
                .icon(icon("back"))
                .order(7)
                .renderToggle(true);

        createSlot("body")
                .size(2)
                .icon(icon("body"))
                .order(8)
                .renderToggle(true);

        createSlot("necklace")
                .size(2)
                .icon(icon("necklace"))
                .order(9)
                .renderToggle(true);

        createSlot("badge")
                .size(1)
                .icon(icon("badge"))
                .order(10)
                .renderToggle(true);

        // 实体-槽位绑定（仅玩家）
        bindPlayerSlot("broken_core_slot");
        bindPlayerSlot("god_shard_slot");
        bindPlayerSlot("charm");
        bindPlayerSlot("ring");
        bindPlayerSlot("bracelet");
        bindPlayerSlot("curse_transfer");
        bindPlayerSlot("head");
        bindPlayerSlot("back");
        bindPlayerSlot("body");
        bindPlayerSlot("necklace");
        bindPlayerSlot("badge");
    }

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, "slot/" + name);
    }

    private void bindPlayerSlot(String slotId) {
        createEntities(slotId + "_entities")
                .addEntities(EntityType.PLAYER)
                .addSlots(slotId);
    }
}
