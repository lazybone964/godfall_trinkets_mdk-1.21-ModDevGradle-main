package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.config.GodfallConfig;
import com.lazybones.godfalltrinkets.item.custom.StomachPouchItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** 大胃袋的属性加成与全伤害加成 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public class StomachPouchBonusEvent {

    private static final ResourceLocation MOD_DAMAGE =
            ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, "stomach_pouch_damage");
    private static final ResourceLocation MOD_MAX_HEALTH =
            ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, "stomach_pouch_max_health");
    private static final ResourceLocation MOD_ARMOR =
            ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, "stomach_pouch_armor");
    private static final ResourceLocation MOD_TOUGHNESS =
            ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, "stomach_pouch_toughness");
    private static final ResourceLocation MOD_KNOCKBACK =
            ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, "stomach_pouch_knockback");
    private static final ResourceLocation MOD_LUCK =
            ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, "stomach_pouch_luck");
    private static final ResourceLocation MOD_ATTACK_SPEED =
            ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, "stomach_pouch_attack_speed");

    /** 佩戴中的大胃袋，没有就 EMPTY */
    public static ItemStack getEquippedPouch(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(s -> s.getItem() instanceof StomachPouchItem))
                .map(result -> result.stack())
                .orElse(ItemStack.EMPTY);
    }

    private static void removeAllPouchModifiers(Player player) {
        removeModifier(player, Attributes.ATTACK_DAMAGE, MOD_DAMAGE);
        removeModifier(player, Attributes.MAX_HEALTH, MOD_MAX_HEALTH);
        removeModifier(player, Attributes.ARMOR, MOD_ARMOR);
        removeModifier(player, Attributes.ARMOR_TOUGHNESS, MOD_TOUGHNESS);
        removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, MOD_KNOCKBACK);
        removeModifier(player, Attributes.LUCK, MOD_LUCK);
        removeModifier(player, Attributes.ATTACK_SPEED, MOD_ATTACK_SPEED);
    }

    private static void removeModifier(Player player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    private static void addModifier(Player player, Holder<Attribute> attribute, ResourceLocation id,
                                    double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    /** 全伤害加成：近战 / 弓 / 弩 / 三叉戟都吃这一手 */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        Entity source = event.getSource().getEntity();
        if (!(source instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        ItemStack pouch = getEquippedPouch(player);
        if (pouch.isEmpty()) {
            return;
        }
        float coeffHunger = StomachPouchItem.getHungerCoeff(pouch);
        double multiplier = 1.0D + coeffHunger * GodfallConfig.STOMACH_FULL_DMG_BONUS.get();
        event.setAmount((float) (event.getAmount() * multiplier));
    }

    /** 服务端每 tick 重建百分比属性修饰符（戴上重算 / 摘下清空） */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack pouch = getEquippedPouch(player);
        removeAllPouchModifiers(player);
        if (pouch.isEmpty()) {
            return;
        }
        float coeffHunger = StomachPouchItem.getHungerCoeff(pouch);
        float coeffSat = StomachPouchItem.getSatCoeff(pouch);

        addModifier(player, Attributes.MAX_HEALTH, MOD_MAX_HEALTH,
                coeffHunger * GodfallConfig.STOMACH_FULL_MAXHP_BONUS.get(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addModifier(player, Attributes.ARMOR, MOD_ARMOR,
                coeffSat * GodfallConfig.STOMACH_FULL_ARMOR_BONUS.get(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addModifier(player, Attributes.ARMOR_TOUGHNESS, MOD_TOUGHNESS,
                coeffSat * GodfallConfig.STOMACH_FULL_TOUGH_BONUS.get(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        // 击退抗性是绝对值，不是百分比
        addModifier(player, Attributes.KNOCKBACK_RESISTANCE, MOD_KNOCKBACK,
                coeffSat * GodfallConfig.STOMACH_FULL_KNOCKBACK_RESIST.get(),
                AttributeModifier.Operation.ADD_VALUE);
        addModifier(player, Attributes.LUCK, MOD_LUCK,
                coeffSat * GodfallConfig.STOMACH_FULL_LUCK_BONUS.get(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addModifier(player, Attributes.ATTACK_SPEED, MOD_ATTACK_SPEED,
                coeffSat * GodfallConfig.STOMACH_FULL_ATKSPEED_BONUS.get(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }
}
