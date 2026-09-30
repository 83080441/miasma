package dev.sdfg.mod.element;

import java.util.Optional;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.entity.WarpEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Elements a player has learned by scoping a Warp.
 * A new player knows none. Each look unlocks only that node's primary.
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class ElementDiscovery {
    /** Same reach as the revealing helmet. */
    public static final double LOOK_RANGE = 48.0;

    private static final Identifier APPROACH_WARP = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "progress/root");

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ExampleMod.MODID);

    public static final Supplier<AttachmentType<DiscoveredSet>> DISCOVERED = ATTACHMENT_TYPES.register(
            "discovered_elements",
            () -> AttachmentType.builder(DiscoveredSet::empty)
                    .serialize(DiscoveredSet.CODEC, set -> set.mask() != 0)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, DiscoveredSet.STREAM_CODEC)
                    .build()
    );

    private ElementDiscovery() {
    }

    public static DiscoveredSet of(Player player) {
        return player.getData(DISCOVERED);
    }

    public static boolean knows(Player player, Element element) {
        return of(player).knows(element);
    }

    /** The Warp under the spyglass, or null. */
    public static WarpEntity lookedWarp(Player player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(LOOK_RANGE));
        AABB box = player.getBoundingBox().expandTowards(look.scale(LOOK_RANGE)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                start,
                end,
                box,
                entity -> entity instanceof WarpEntity && entity.isAlive(),
                0.0F
        );
        if (hit == null) {
            return null;
        }
        Entity entity = hit.getEntity();
        return entity instanceof WarpEntity warp ? warp : null;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (serverPlayer.level() instanceof ServerLevel level) {
            tryApproach(serverPlayer, level);
        }
        if (!serverPlayer.isScoping()) {
            return;
        }
        WarpEntity warp = lookedWarp(serverPlayer);
        if (warp == null) {
            return;
        }
        Optional<Element> primary = warp.getElement();
        if (primary.isEmpty()) {
            return;
        }
        Element element = primary.get();
        award(serverPlayer, Identifier.fromNamespaceAndPath(ExampleMod.MODID, "progress/" + element.id()), "discover");
        DiscoveredSet current = of(serverPlayer);
        if (current.knows(element)) {
            return;
        }
        serverPlayer.setData(DISCOVERED, current.with(element));
        serverPlayer.syncData(DISCOVERED);
        serverPlayer.sendOverlayMessage(
                Component.translatable("sdfg.discovery.found", Component.translatable("element.sdfg." + element.id()))
        );
    }

    /** Approaching any living Warp within {@link WarpEntity#PULL_RANGE} unlocks the progress tab. */
    private static void tryApproach(ServerPlayer player, ServerLevel level) {
        if (alreadyDone(player, APPROACH_WARP)) {
            return;
        }
        AABB search = player.getBoundingBox().inflate(WarpEntity.PULL_RANGE);
        for (WarpEntity warp : level.getEntitiesOfClass(WarpEntity.class, search)) {
            if (warp.isAlive() && warp.distanceTo(player) <= WarpEntity.PULL_RANGE) {
                award(player, APPROACH_WARP, "approach");
                return;
            }
        }
    }

    private static boolean alreadyDone(ServerPlayer player, Identifier id) {
        AdvancementHolder holder = holder(player, id);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static void award(ServerPlayer player, Identifier id, String criterion) {
        AdvancementHolder holder = holder(player, id);
        if (holder != null) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    private static AdvancementHolder holder(ServerPlayer player, Identifier id) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getServer().getAdvancements().get(id);
    }

    /** Bitmask of known elements. Bit {@code number - 1}. Empty is a new player. */
    public record DiscoveredSet(int mask) {
        public static final MapCodec<DiscoveredSet> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.optionalFieldOf("mask", 0).forGetter(DiscoveredSet::mask)
        ).apply(instance, DiscoveredSet::new));

        public static final StreamCodec<ByteBuf, DiscoveredSet> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(DiscoveredSet::new, DiscoveredSet::mask);

        public static DiscoveredSet empty() {
            return new DiscoveredSet(0);
        }

        public boolean knows(Element element) {
            if (element == null) {
                return false;
            }
            return (this.mask & bit(element)) != 0;
        }

        public DiscoveredSet with(Element element) {
            return new DiscoveredSet(this.mask | bit(element));
        }

        private static int bit(Element element) {
            return 1 << (element.number() - 1);
        }
    }
}
