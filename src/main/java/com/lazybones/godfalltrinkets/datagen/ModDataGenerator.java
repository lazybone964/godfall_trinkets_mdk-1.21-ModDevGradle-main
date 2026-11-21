package com.lazybones.godfalltrinkets.datagen;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider.TagLookup;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * 数据生成入口。runData 时由 NeoForge 触发，产出到 src/generated/resources（见 build.gradle 的 data run 配置）。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModDataGenerator {

    private ModDataGenerator() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(),
                new ModCuriosProvider(output, fileHelper, event.getLookupProvider()));

        // Curios 槽位物品标签（curios:ring / curios:charm / ...），缺了它饰品拖不进槽
        generator.addProvider(event.includeServer(),
                new ModItemTagsProvider(output, event.getLookupProvider(),
                        CompletableFuture.completedFuture(TagLookup.empty()), fileHelper));
    }
}
