package com.lazybones.godfalltrinkets;

import com.lazybones.godfalltrinkets.config.GodfallConfig;
import com.lazybones.godfalltrinkets.item.ModCreativeTabs;
import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.loot.ModLootModifiers;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(GodfallTrinkets.MOD_ID)
public class GodfallTrinkets {

    public static final String MOD_ID = "godfall_trinkets";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GodfallTrinkets(IEventBus modEventBus, ModContainer modContainer) {
        // 物品 / 创造页签注册
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        // 玩家挂载数据（强化戒冷却之类的长期状态，1.21 用 Attachment 取代 getPersistentData）
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        // 物品自定义数据组件（破厄经验 / 护盾 / 神格标记，1.21 用 DataComponent 取代 NBT）
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        // 药效（深渊标记 / 神陨怒层）
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        // 全局战利品修饰器序列化器（碎片宝箱 / 拾珍护符）
        ModLootModifiers.SERIALIZERS.register(modEventBus);
        // 配置（1.21 用 ModConfigSpec 取代 ForgeConfigSpec）
        modContainer.registerConfig(ModConfig.Type.COMMON, GodfallConfig.SPEC);
    }
}
