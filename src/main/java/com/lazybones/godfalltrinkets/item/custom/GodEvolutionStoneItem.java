package com.lazybones.godfalltrinkets.item.custom;

import com.lazybones.godfalltrinkets.config.GodfallConfig;
import com.lazybones.godfalltrinkets.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 神格进化石——集齐 13 碎片后发放，右键破厄之核把它进化成神格。
 *
 * 1.21 差异：{@code getCuriosInventory} 直接返回 {@code Optional}（没有 {@code resolve()}）；
 * 旧版复制 NBT 改为复制整套 DataComponent；{@code SlotResult} 是 record。
 */
public class GodEvolutionStoneItem extends Item {

    public GodEvolutionStoneItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stone = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stone);
        }

        if (!GodfallConfig.ENABLE_GOD_CORE_EVOLUTION.get()) {
            player.sendSystemMessage(Component.literal("神格进化功能已被关闭")
                    .withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stone);
        }

        var result = CuriosApi.getCuriosInventory(player)
                .flatMap(h -> h.findFirstCurio(s -> s.is(ModItems.BROKEN_CORE.get())));
        if (result.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.godfall_trinkets.no_core_found")
                    .withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stone);
        }

        ItemStack core = result.get().stack();
        if (GodCoreItem.isGodCore(core)) {
            player.sendSystemMessage(Component.translatable("message.godfall_trinkets.already_god")
                    .withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stone);
        }

        // 复制全部数据组件（经验/护盾状态等）到神格
        ItemStack god = new ItemStack(ModItems.GOD_CORE.get());
        god.applyComponents(core.getComponents());
        GodCoreItem.markAsGodCore(god);

        // 就地替换槽位里的物品
        String slotId = result.get().slotContext().identifier();
        int slotIndex = result.get().slotContext().index();
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.getStacksHandler(slotId).ifPresent(stacks ->
                        stacks.getStacks().setStackInSlot(slotIndex, god)));

        stone.shrink(1);
        player.sendSystemMessage(Component.translatable("message.godfall_trinkets.evolve_success")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        return InteractionResultHolder.consume(stone);
    }
}
