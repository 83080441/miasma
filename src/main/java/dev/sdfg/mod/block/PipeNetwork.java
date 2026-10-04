package dev.sdfg.mod.block;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Walks pipes from a cauldron lid to the nearest element container. */
public final class PipeNetwork {
    public static final int MAX_BLOCKS = 64;

    private PipeNetwork() {
    }

    public static boolean connects(BlockState state) {
        return state.getBlock() instanceof ElementPipeBlock
                || state.getBlock() instanceof ValvePipeBlock
                || state.getBlock() instanceof CauldronLidBlock
                || state.getBlock() instanceof ElementContainerBlock
                || state.getBlock() instanceof CondensationFilterBlock;
    }

    /** A section the element can travel through. A closed valve blocks its neighbours. */
    public static boolean passes(BlockState state) {
        if (state.getBlock() instanceof ValvePipeBlock) {
            return ValvePipeBlock.isOpen(state);
        }
        return state.getBlock() instanceof ElementPipeBlock;
    }

    /** Closest container reachable through pipes. The lid itself is the origin and is not a container. */
    public static BlockPos findContainer(Level level, BlockPos origin) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && seen.size() <= MAX_BLOCKS) {
            BlockPos pos = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!seen.add(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof ElementContainerBlock) {
                    return next;
                }
                if (passes(state)) {
                    queue.addLast(next);
                }
            }
        }
        return null;
    }
}
