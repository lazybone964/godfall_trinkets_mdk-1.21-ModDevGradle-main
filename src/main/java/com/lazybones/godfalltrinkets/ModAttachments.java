package com.lazybones.godfalltrinkets;

import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

/**
 * 1.21 里 Forge 的 {@code player.getPersistentData()} 已经没了，玩家的长期状态改走 Attachment。
 * 这里集中注册饰品效果需要的挂载数据。
 */
public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GodfallTrinkets.MOD_ID);

    /** 濒生戒「濒死无敌」冷却，存的是最后一次触发的 gameTime（0 = 从未触发） */
    public static final Supplier<AttachmentType<Long>> DEATH_DEFY_COOLDOWN =
            ATTACHMENT_TYPES.register("death_defy_cooldown",
                    () -> AttachmentType.<Long>builder(() -> 0L).serialize(Codec.LONG).build());

    /** 是否已经给该玩家发过破厄之核（对应旧 NBT godfall_got_broken_core，死亡保留） */
    public static final Supplier<AttachmentType<Boolean>> GOT_BROKEN_CORE =
            ATTACHMENT_TYPES.register("got_broken_core",
                    () -> AttachmentType.<Boolean>builder(() -> false)
                            .serialize(Codec.BOOL)
                            .copyOnDeath()
                            .build());

    /** 是否已经给该玩家发过神格进化石（对应旧 NBT godfall_got_evolution_stone，死亡保留） */
    public static final Supplier<AttachmentType<Boolean>> GOT_EVOLUTION_STONE =
            ATTACHMENT_TYPES.register("got_evolution_stone",
                    () -> AttachmentType.<Boolean>builder(() -> false)
                            .serialize(Codec.BOOL)
                            .copyOnDeath()
                            .build());

    /** 是否已经发放「破厄再生」buff（对应旧 NBT godfall_given_regen） */
    public static final Supplier<AttachmentType<Boolean>> GIVEN_CORE_REGEN =
            ATTACHMENT_TYPES.register("given_core_regen",
                    () -> AttachmentType.<Boolean>builder(() -> false).serialize(Codec.BOOL).build());

    /** 咒厄转移坠已吸收的诅咒集合 */
    public static final Supplier<AttachmentType<List<String>>> ABSORBED_CURSES =
            ATTACHMENT_TYPES.register("absorbed_curses",
                    () -> AttachmentType.<List<String>>builder(() -> List.of())
                            .serialize(Codec.STRING.listOf())
                            .copyOnDeath()
                            .build());

    /** 生命碎片复活的冷却到期时间（gameTime），0 = 可用 */
    public static final Supplier<AttachmentType<Long>> SHARD_LIFE_CD =
            ATTACHMENT_TYPES.register("shard_life_cd",
                    () -> AttachmentType.<Long>builder(() -> 0L).serialize(Codec.LONG).build());

    private ModAttachments() {
    }
}
