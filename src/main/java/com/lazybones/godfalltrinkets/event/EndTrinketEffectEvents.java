package com.lazybones.godfalltrinkets.event;

import com.lazybones.godfalltrinkets.GodfallTrinkets;
import com.lazybones.godfalltrinkets.item.ModItems;
import com.lazybones.godfalltrinkets.util.CurioHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Random;

/**
 * 末地系饰品（阶段二 + 3-4d）：龙核之心 / 残晶冠冕 / 终末之眼 / 寂灭龙脊。
 * <p>
 * 肩甲「护甲 ×3、韧性 ×2」不再像 1.20.1 那样每个 tick 去改 AttributeInstance 的 baseValue
 * （那样会互相覆盖、重登后残留），而是在 {@code ModItems.CRYSTAL_CROWN} 上挂
 * {@code ADD_MULTIPLIED_BASE} 属性修饰符，由 Curios 装备时自动生效/移除；
 * 这里只负责「生命值锁 1」的 tick 逻辑。
 * <p>
 * 寂灭龙脊的移速/攻速也不再改 baseValue（旧实现会把所有玩家的 baseValue 钉死在 0.1，
 * 与其它移速加成冲突），改为按当前血量实时刷新「临时属性修饰符」。
 */
@EventBusSubscriber(modid = GodfallTrinkets.MOD_ID)
public final class EndTrinketEffectEvents {

    private static final Random RANDOM = new Random();

    private static final String EYE_COOLDOWN_TAG = "eye_end_cooldown";
    private static final long EYE_COOLDOWN_TICKS = 1200L;

    private static final ResourceLocation SPINE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, "spine_end_speed");
    private static final ResourceLocation SPINE_ATTACK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(GodfallTrinkets.MOD_ID, "spine_end_attack_speed");

    private EndTrinketEffectEvents() {
    }

    // ======================== Tick ========================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        // 残晶冠冕：强制把当前生命值锁为 1（生命上限不受影响）
        if (CurioHelper.hasTrinket(player, ModItems.CRYSTAL_CROWN.get())
                && player.getHealth() > 1.0f) {
            player.setHealth(1.0f);
        }

        // 寂灭龙脊：血量越低，移速与攻速越高
        boolean hasSpine = CurioHelper.hasTrinket(player, ModItems.DRAGON_SPINE_END.get());
        float missing = hasSpine
                ? 1.0f - player.getHealth() / player.getMaxHealth()
                : 0.0f;
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPINE_SPEED_ID,
                missing * 3.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyModifier(player.getAttribute(Attributes.ATTACK_SPEED), SPINE_ATTACK_SPEED_ID,
                missing * 0.25, AttributeModifier.Operation.ADD_VALUE);
    }

    /** 刷新临时修饰符：值为 0 时直接移除 */
    private static void applyModifier(AttributeInstance attribute, ResourceLocation id,
                                      double amount, AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }
        if (attribute.getModifier(id) != null) {
            attribute.removeModifier(id);
        }
        if (amount > 0.0001) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    // ======================== 伤害 ========================

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        float damage = event.getAmount();

        // ---- 玩家作为攻击者：寂灭龙脊残血增伤 ----
        if (event.getSource().getEntity() instanceof Player attacker
                && attacker != event.getEntity()
                && CurioHelper.hasTrinket(attacker, ModItems.DRAGON_SPINE_END.get())) {
            float ratio = attacker.getHealth() / attacker.getMaxHealth();
            damage *= 1.0f + (1.0f - ratio);
        }

        if (!(event.getEntity() instanceof Player player)) {
            event.setAmount(damage);
            return;
        }

        boolean fatal = player.getHealth() - damage <= 0;

        // ---- 终末之眼：致命伤消耗末影珍珠，回 30% 生命并瞬移 5 格 ----
        if (fatal && CurioHelper.hasTrinket(player, ModItems.EYE_OF_THE_END.get())) {
            long now = player.level().getGameTime();
            long cooldown = player.getPersistentData().getLong(EYE_COOLDOWN_TAG);
            if (now >= cooldown) {
                int pearlSlot = findEnderPearl(player);
                if (pearlSlot != -1) {
                    player.getInventory().removeItem(pearlSlot, 1);
                    player.heal(player.getMaxHealth() * 0.3f);
                    BlockPos safe = findSafePos(player.blockPosition(), player.level(), 5);
                    player.teleportTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
                    player.getPersistentData().putLong(EYE_COOLDOWN_TAG, now + EYE_COOLDOWN_TICKS);
                    damage = 0;
                }
            }
        }

        // ---- 终末之眼：身处末地，承伤 -15% ----
        if (CurioHelper.hasTrinket(player, ModItems.EYE_OF_THE_END.get())
                && player.level().dimension() == Level.END) {
            damage *= 0.85f;
        }

        // ---- 寂灭龙脊：25% 概率反弹 30% 伤害 ----
        if (damage > 0
                && CurioHelper.hasTrinket(player, ModItems.DRAGON_SPINE_END.get())
                && event.getSource().getEntity() instanceof LivingEntity attacker
                && attacker != player
                && RANDOM.nextFloat() <= 0.25f) {
            attacker.hurt(player.damageSources().thorns(player), damage * 0.3f);
        }

        event.setAmount(Math.max(0, damage));
    }

    private static int findEnderPearl(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.ENDER_PEARL) && stack.getCount() > 0) {
                return i;
            }
        }
        return -1;
    }

    /** 5 格内找安全落点：下方实心、上方两格空气 */
    private static BlockPos findSafePos(BlockPos origin, Level level, int range) {
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = -3; y <= 3; y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (level.getBlockState(pos.below()).isSolid()
                            && level.getBlockState(pos).isAir()
                            && level.getBlockState(pos.above()).isAir()) {
                        return pos;
                    }
                }
            }
        }
        return origin;
    }
}
