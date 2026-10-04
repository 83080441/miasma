package dev.sdfg.mod.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.sdfg.mod.block.ElementContainerBlock;
import dev.sdfg.mod.block.ElementContainerBlockEntity;
import dev.sdfg.mod.block.ElementCrystalBlock;
import dev.sdfg.mod.block.HeatedCauldronBlock;
import dev.sdfg.mod.block.HeatedCauldronBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.AttachmentType;

/**
 * What a drained block still holds. Plain blocks keep the leftover here, because the
 * block state itself does not change. Cauldrons and glass already store their mix.
 * The original snapshot is what the block had the first time the wand touched it,
 * so the gray wash can show how much of the total is gone. It stays on the chunk.
 */
public final class BlockResidue {
    private static final int SLOTS = Element.values().length;

    public static final Supplier<AttachmentType<BlockResidue>> TYPE = ElementDiscovery.ATTACHMENT_TYPES.register(
            "block_residue",
            () -> AttachmentType.builder(() -> new BlockResidue())
                    .serialize(BlockResidue.CODEC, residue -> !residue.entries.isEmpty())
                    .sync(BlockResidue.STREAM)
                    .build()
    );

    private final Map<Long, Stored> entries = new HashMap<>();

    public BlockResidue() {
    }

    private BlockResidue(List<Stored> stored) {
        for (Stored entry : stored) {
            this.entries.put(entry.pos.asLong(), entry);
        }
    }

    /** Loads this class so {@link #TYPE} is registered before the bus is frozen. */
    public static void init() {
    }

    /**
     * What the wand and the helmet row should count. A stored leftover wins over the
     * catalog for an ordinary block. Cauldron and glass use the mix they hold now.
     */
    public static ElementAmounts amounts(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof HeatedCauldronBlock
                && level.getBlockEntity(pos) instanceof HeatedCauldronBlockEntity cauldron) {
            return cauldron.contents();
        }
        if (state.getBlock() instanceof ElementContainerBlock
                && level.getBlockEntity(pos) instanceof ElementContainerBlockEntity container) {
            return container.contents();
        }
        if (state.getBlock() instanceof ElementCrystalBlock) {
            int amount = ElementCrystalBlock.isSolid(state)
                    ? 100
                    : state.getValue(ElementCrystalBlock.AGE) * 10;
            return ElementAmounts.of(ElementCrystalBlock.elementOf(state), amount);
        }
        Stored stored = find(level, pos);
        if (stored != null && stored.blockId == blockId(state)) {
            return fromArray(unpack(stored.remaining));
        }
        return ElementLookup.of(state);
    }

    /**
     * Fraction of the block's original energy that is already gone, from 0 to 1.
     * A block that was never drained, or whose id changed, is 0.
     */
    public static float loss(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Stored stored = find(level, pos);
        if (stored == null || stored.blockId != blockId(state)) {
            return 0.0F;
        }
        int original = sum(unpack(stored.original));
        if (original <= 0) {
            return 0.0F;
        }
        int left = sum(asArray(amounts(level, pos)));
        float gone = 1.0F - left / (float) original;
        if (gone < 0.0F) {
            return 0.0F;
        }
        if (gone > 1.0F) {
            return 1.0F;
        }
        return gone;
    }

    /** Visits every stored block in the chunks around {@code origin}. */
    public static void visitNearby(Level level, BlockPos origin, int chunkRadius, Consumer<BlockPos> consumer) {
        int centerX = origin.getX() >> 4;
        int centerZ = origin.getZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                if (!level.hasChunk(centerX + dx, centerZ + dz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(centerX + dx, centerZ + dz);
                if (!chunk.hasData(TYPE)) {
                    continue;
                }
                for (Stored stored : chunk.getData(TYPE).entries.values()) {
                    consumer.accept(stored.pos);
                }
            }
        }
    }

    public static int[] snapshot(Level level, BlockPos pos) {
        Stored stored = find(level, pos);
        BlockState state = level.getBlockState(pos);
        if (stored != null && stored.blockId == blockId(state)) {
            return unpack(stored.original);
        }
        return asArray(live(level, pos));
    }

    public static void remember(Level level, BlockPos pos, int[] remaining, int[] original) {
        if (level.isClientSide() || !(level.getChunkAt(pos) instanceof LevelChunk chunk)) {
            return;
        }
        BlockResidue data = chunk.getData(TYPE);
        BlockState state = level.getBlockState(pos);
        data.entries.put(pos.asLong(), new Stored(pos.immutable(), blockId(state), pack(remaining), pack(original)));
        chunk.setData(TYPE, data);
        chunk.markUnsaved();
        chunk.syncData(TYPE);
    }

    public static void forget(Level level, BlockPos pos) {
        if (level.isClientSide() || !(level.getChunkAt(pos) instanceof LevelChunk chunk) || !chunk.hasData(TYPE)) {
            return;
        }
        BlockResidue data = chunk.getData(TYPE);
        data.entries.remove(pos.asLong());
        if (data.entries.isEmpty()) {
            chunk.removeData(TYPE);
        } else {
            chunk.setData(TYPE, data);
            chunk.syncData(TYPE);
        }
        chunk.markUnsaved();
    }

    private static ElementAmounts live(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof HeatedCauldronBlock
                && level.getBlockEntity(pos) instanceof HeatedCauldronBlockEntity cauldron) {
            return cauldron.contents();
        }
        if (state.getBlock() instanceof ElementContainerBlock
                && level.getBlockEntity(pos) instanceof ElementContainerBlockEntity container) {
            return container.contents();
        }
        return ElementLookup.of(state);
    }

    private static Stored find(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(TYPE)) {
            return null;
        }
        return chunk.getData(TYPE).entries.get(pos.asLong());
    }

    static int blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getId(state.getBlock());
    }

    public static int[] asArray(ElementAmounts amounts) {
        int[] out = new int[SLOTS];
        if (amounts == null) {
            return out;
        }
        for (Element element : Element.values()) {
            out[element.number() - 1] = amounts.get(element);
        }
        return out;
    }

    static ElementAmounts fromArray(int[] amounts) {
        ElementAmounts result = ElementAmounts.empty();
        for (Element element : Element.values()) {
            int value = amounts[element.number() - 1];
            if (value > 0) {
                result.set(element, value);
            }
        }
        return result;
    }

    public static int sum(int[] amounts) {
        int total = 0;
        for (int amount : amounts) {
            total += amount;
        }
        return total;
    }

    private static long pack(int[] amounts) {
        long packed = 0L;
        for (int i = 0; i < SLOTS; i++) {
            int value = amounts[i];
            if (value < 0) {
                value = 0;
            }
            if (value > 255) {
                value = 255;
            }
            packed |= (long) value << (i * 8);
        }
        return packed;
    }

    private static int[] unpack(long packed) {
        int[] amounts = new int[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            amounts[i] = (int) ((packed >> (i * 8)) & 0xFFL);
        }
        return amounts;
    }

    private List<Stored> stored() {
        return new ArrayList<>(this.entries.values());
    }

    private record Stored(BlockPos pos, int blockId, long remaining, long original) {
        private static final Codec<Stored> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Stored::pos),
                Codec.INT.fieldOf("block").forGetter(Stored::blockId),
                Codec.LONG.fieldOf("left").forGetter(Stored::remaining),
                Codec.LONG.fieldOf("was").forGetter(Stored::original)
        ).apply(instance, Stored::new));

        private static final StreamCodec<ByteBuf, Stored> STREAM = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                Stored::pos,
                ByteBufCodecs.VAR_INT,
                Stored::blockId,
                ByteBufCodecs.VAR_LONG,
                Stored::remaining,
                ByteBufCodecs.VAR_LONG,
                Stored::original,
                Stored::new
        );
    }

    private static final MapCodec<BlockResidue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Stored.CODEC.listOf().fieldOf("blocks").forGetter(BlockResidue::stored)
    ).apply(instance, BlockResidue::new));

    private static final StreamCodec<ByteBuf, BlockResidue> STREAM =
            Stored.STREAM.apply(ByteBufCodecs.list()).map(BlockResidue::new, BlockResidue::stored);
}
