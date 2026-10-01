package dev.sdfg.mod.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.block.ElementContainerBlock;
import dev.sdfg.mod.block.ElementContainerBlockEntity;
import dev.sdfg.mod.block.HeatedCauldronBlock;
import dev.sdfg.mod.block.HeatedCauldronBlockEntity;
import dev.sdfg.mod.element.BlockResidue;
import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.element.ElementAmounts;
import dev.sdfg.mod.entity.ElementArrow;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ColorParticleOption;
import dev.sdfg.mod.particle.ModParticles;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * One right-click channel. The player stays at the spot where it started and may only look.
 * Once each second it takes 10 of each scroll element, in a random order, from random blocks
 * in the cube, so a block only loses a little and the rest stay. After the three seconds
 * one arrow leaves per scroll element, half a second apart, and nothing is absorbed after that.
 * Letting go keeps what was already taken and fires nothing.
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class WandChannel {
    /** How much of one element is taken from the cube each second. */
    private static final int BITE = 10;
    /** Ticks between those bites. One second. */
    private static final int BITE_INTERVAL = 20;
    /** Half a second, in ticks, between one arrow and the next. */
    private static final int ARROW_INTERVAL = 10;
    /** How far from the body the arrows appear, so they ring the player. */
    private static final double ARROW_RING = 1.15;
    private static final int BLACK = 0x1A1A1A;

    private static final Map<UUID, Cast> CASTS = new HashMap<>();
    private static final Map<UUID, Volley> VOLLEYS = new HashMap<>();
    private static final Set<UUID> WAIT_FOR_RELEASE = new HashSet<>();
    private static final Set<UUID> SAW_RELEASE = new HashSet<>();

    private WandChannel() {
    }

    /** First scroll in hotbar slots 1, 2, then 3. Null when those three are empty of scrolls. */
    public static ScrollItem scrollInHotbar(Player player) {
        for (int slot = 0; slot < 3; slot++) {
            if (player.getInventory().getItem(slot).getItem() instanceof ScrollItem scroll
                    && scroll.kind().elements().length > 0) {
                return scroll;
            }
        }
        return null;
    }

    /** False while a channel or its arrows are still going, or the click never came up. */
    public static boolean mayBegin(Player player) {
        UUID id = player.getUUID();
        return !CASTS.containsKey(id) && !VOLLEYS.containsKey(id) && !WAIT_FOR_RELEASE.contains(id);
    }

    public static void begin(ServerPlayer player, ScrollItem scroll) {
        if (!mayBegin(player)) {
            return;
        }
        SAW_RELEASE.remove(player.getUUID());
        Element[] elements = scroll.kind().elements().clone();
        CASTS.put(player.getUUID(), new Cast(elements, order(player), player.position()));
    }

    /** The use button is up. A finished channel may start again only after this. */
    public static void noteReleased(Player player) {
        UUID id = player.getUUID();
        SAW_RELEASE.add(id);
        if (!VOLLEYS.containsKey(id)) {
            WAIT_FOR_RELEASE.remove(id);
        }
    }

    /** Release, a swapped item, or a dropped wand. Already-taken energy stays gone. */
    public static void cancel(Player player) {
        CASTS.remove(player.getUUID());
    }

    public static void tickDrain(ServerPlayer player, int ticksRemaining) {
        Cast cast = CASTS.get(player.getUUID());
        if (cast == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        int used = WandItem.CHANNEL_TICKS - ticksRemaining;
        while (cast.nextBiteAt <= used && cast.nextBiteAt < WandItem.CHANNEL_TICKS) {
            cast.nextBiteAt += BITE_INTERVAL;
            absorb(level, player, cast);
        }
    }

    /** The three seconds finished. One last bite, then the arrows start. */
    public static void complete(ServerPlayer player) {
        Cast cast = CASTS.remove(player.getUUID());
        if (cast == null || !player.isAlive()) {
            return;
        }
        if (cast.nextBiteAt == WandItem.CHANNEL_TICKS && player.level() instanceof ServerLevel level) {
            absorb(level, player, cast);
        }
        WAIT_FOR_RELEASE.add(player.getUUID());
        SAW_RELEASE.remove(player.getUUID());
        VOLLEYS.put(player.getUUID(), new Volley(cast.elements, player.getLookAngle()));
    }

    @SubscribeEvent
    static void afterTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        hold(player);
        if (player instanceof ServerPlayer server) {
            volley(server);
        }
    }

    private static void hold(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        Cast cast = CASTS.get(player.getUUID());
        if (cast == null) {
            return;
        }
        if (!player.isUsingItem() || !player.getUseItem().is(ExampleMod.WAND.get())) {
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.setPos(cast.anchor.x, cast.anchor.y, cast.anchor.z);
        player.resetFallDistance();
        player.setSprinting(false);
    }

    /**
     * Ten of each scroll element, elements in a random order, each ten taken from
     * random blocks that still have it. A block gives up at most ten, then the next one.
     */
    private static void absorb(ServerLevel level, ServerPlayer player, Cast cast) {
        List<Element> elements = new ArrayList<>(List.of(cast.elements));
        List<BlockPos> cells = new ArrayList<>(List.of(cast.order));
        RandomSource random = level.getRandom();
        shuffle(elements, random);
        for (Element element : elements) {
            shuffle(cells, random);
            int left = BITE;
            for (BlockPos pos : cells) {
                if (left <= 0) {
                    break;
                }
                left -= drain(level, player, pos, element, left);
            }
        }
    }

    private static <T> void shuffle(List<T> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int swapWith = random.nextInt(i + 1);
            T swap = list.get(i);
            list.set(i, list.get(swapWith));
            list.set(swapWith, swap);
        }
    }

    private static void volley(ServerPlayer player) {
        Volley volley = VOLLEYS.get(player.getUUID());
        if (volley == null) {
            return;
        }
        if (!player.isAlive()) {
            VOLLEYS.remove(player.getUUID());
            WAIT_FOR_RELEASE.remove(player.getUUID());
            return;
        }
        if (volley.wait > 0) {
            volley.wait--;
            return;
        }
        fire(player, volley, volley.next);
        volley.next++;
        if (volley.next >= volley.elements.length) {
            VOLLEYS.remove(player.getUUID());
            if (SAW_RELEASE.contains(player.getUUID())) {
                WAIT_FOR_RELEASE.remove(player.getUUID());
            }
        } else {
            volley.wait = ARROW_INTERVAL;
        }
    }

    private static void fire(ServerPlayer player, Volley volley, int shot) {
        Element[] elements = volley.elements;
        if (!(player.level() instanceof ServerLevel level) || elements.length == 0) {
            return;
        }
        Element element = elements[shot];
        double angle = (Math.PI * 2.0 * shot) / elements.length;
        double x = player.getX() + Math.cos(angle) * ARROW_RING;
        double y = player.getEyeY() - 0.2;
        double z = player.getZ() + Math.sin(angle) * ARROW_RING;
        ElementArrow arrow = new ElementArrow(level, player, element);
        arrow.setPos(x, y, z);
        arrow.shoot(volley.look.x, volley.look.y, volley.look.z, 2.2F, 0.0F);
        arrow.pickup = ElementArrow.Pickup.DISALLOWED;
        level.addFreshEntity(arrow);
    }

    /**
     * Far, upper, and behind first. Lower and in front last.
     * The third axis picks one side so the two ends are opposite corners.
     */
    private static BlockPos[] order(Player player) {
        Vec3 look = player.getLookAngle();
        double fx = look.x;
        double fz = look.z;
        double flat = Math.hypot(fx, fz);
        if (flat < 1.0E-6) {
            fx = 0.0;
            fz = 1.0;
        } else {
            fx /= flat;
            fz /= flat;
        }
        double rightX = -fz;
        double rightZ = fx;
        BlockPos center = player.getOnPos();
        List<BlockPos> cells = new ArrayList<>(27);
        for (int x = -WandItem.RADIUS; x <= WandItem.RADIUS; x++) {
            for (int y = -WandItem.RADIUS; y <= WandItem.RADIUS; y++) {
                for (int z = -WandItem.RADIUS; z <= WandItem.RADIUS; z++) {
                    cells.add(center.offset(x, y, z));
                }
            }
        }
        final double lookX = fx;
        final double lookZ = fz;
        cells.sort(Comparator
                .comparingInt((BlockPos pos) -> center.getY() - pos.getY())
                .thenComparing(Comparator.comparingDouble((BlockPos pos) -> behind(pos, center, lookX, lookZ)).reversed())
                .thenComparing(Comparator.comparingDouble((BlockPos pos) -> side(pos, center, rightX, rightZ)).reversed()));
        return cells.toArray(BlockPos[]::new);
    }

    /** Positive when the block sits opposite the look, on the horizontal plane. */
    private static double behind(BlockPos pos, BlockPos center, double lookX, double lookZ) {
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        return -(dx * lookX + dz * lookZ);
    }

    private static double side(BlockPos pos, BlockPos center, double rightX, double rightZ) {
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        return dx * rightX + dz * rightZ;
    }

    private static int drain(ServerLevel level, ServerPlayer player, BlockPos pos, Element element, int budget) {
        if (budget <= 0 || !level.isLoaded(pos)) {
            return 0;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return 0;
        }
        int[] before = BlockResidue.asArray(BlockResidue.amounts(level, pos));
        int index = element.number() - 1;
        if (before[index] <= 0) {
            return 0;
        }
        int[] original = BlockResidue.snapshot(level, pos);
        int take = Math.min(before[index], budget);
        int[] after = before.clone();
        boolean emptied;
        if (state.getBlock() instanceof HeatedCauldronBlock
                && level.getBlockEntity(pos) instanceof HeatedCauldronBlockEntity cauldron) {
            take = cauldron.drain(element, take);
            emptied = cauldron.contents().isEmpty();
            if (take > 0) {
                after = BlockResidue.asArray(cauldron.contents());
            }
        } else if (state.getBlock() instanceof ElementContainerBlock
                && level.getBlockEntity(pos) instanceof ElementContainerBlockEntity container) {
            ElementAmounts drawn = ElementAmounts.of(element, take);
            container.extract(drawn);
            after = BlockResidue.asArray(container.contents());
            take = before[index] - after[index];
            emptied = container.contents().isEmpty();
        } else {
            after[index] = before[index] - take;
            emptied = BlockResidue.sum(after) <= 0;
        }
        if (take <= 0) {
            return 0;
        }
        Vec3 from = Vec3.atCenterOf(pos);
        Vec3 to = player.getEyePosition();
        motes(level, from, to, element.color(), 4);
        int had = original[index] <= 0 ? before[index] : original[index];
        float gone = had <= 0 ? 1.0F : 1.0F - after[index] / (float) had;
        if (gone < 0.0F) {
            gone = 0.0F;
        }
        int black = Math.round(gone * 8.0F);
        if (black > 0) {
            motes(level, from, to, BLACK, black);
        }
        if (emptied) {
            BlockResidue.forget(level, pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            motes(level, from, to, element.color(), 12);
        } else if (!(state.getBlock() instanceof HeatedCauldronBlock)
                && !(state.getBlock() instanceof ElementContainerBlock)) {
            BlockResidue.remember(level, pos, after, original);
        } else {
            BlockResidue.remember(level, pos, after, original);
        }
        return take;
    }

    private static void motes(ServerLevel level, Vec3 from, Vec3 to, int rgb, int count) {
        Vec3 delta = to.subtract(from);
        double distance = delta.length();
        if (distance < 1.0E-4) {
            return;
        }
        Vec3 velocity = delta.scale((distance / 12.0) / distance);
        ColorParticleOption particle = ColorParticleOption.create(ModParticles.FLIGHT_MOTE.get(), 0xFF000000 | (rgb & 0xFFFFFF));
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            double x = from.x + (random.nextDouble() - 0.5) * 0.5;
            double y = from.y + (random.nextDouble() - 0.5) * 0.5;
            double z = from.z + (random.nextDouble() - 0.5) * 0.5;
            level.sendParticles(particle, x, y, z, 0, velocity.x, velocity.y, velocity.z, 1.0);
        }
    }

    private static final class Cast {
        final Element[] elements;
        final BlockPos[] order;
        final Vec3 anchor;
        /** Ticks of channeling when the next bite lands. The first one is at one second. */
        int nextBiteAt = BITE_INTERVAL;

        Cast(Element[] elements, BlockPos[] order, Vec3 anchor) {
            this.elements = elements;
            this.order = order;
            this.anchor = anchor;
        }
    }

    private static final class Volley {
        final Element[] elements;
        final Vec3 look;
        int next;
        int wait;

        Volley(Element[] elements, Vec3 look) {
            this.elements = elements;
            this.look = look;
        }
    }
}
