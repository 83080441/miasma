package dev.sdfg.mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Picks a sturdy face to hug, or {@link PipeSupport#NONE} when there is no wall/floor. */
public final class PipeSupportLogic {
    private static final Direction[] LOOKUP_ORDER = {
            Direction.DOWN,
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };

    private PipeSupportLogic() {
    }

    public static boolean hasSupport(BlockGetter level, BlockPos pipePos, Direction towardSupport) {
        BlockPos supportPos = pipePos.relative(towardSupport);
        BlockState supportState = level.getBlockState(supportPos);
        return supportState.isFaceSturdy(level, supportPos, towardSupport.getOpposite());
    }

    /**
     * Prefer the clicked face's opposite (the wall you placed against).
     * Otherwise floor, then ceiling, then sides. No sturdy face → center.
     */
    public static PipeSupport resolve(BlockGetter level, BlockPos pos, @Nullable Direction preferred) {
        if (preferred != null && hasSupport(level, pos, preferred)) {
            return PipeSupport.of(preferred);
        }
        for (Direction direction : LOOKUP_ORDER) {
            if (hasSupport(level, pos, direction)) {
                return PipeSupport.of(direction);
            }
        }
        return PipeSupport.NONE;
    }

    /** Keep the current support while it stays sturdy; otherwise re-resolve. */
    public static PipeSupport update(BlockGetter level, BlockPos pos, PipeSupport current) {
        Direction toward = current.asDirection();
        if (toward != null && hasSupport(level, pos, toward)) {
            return current;
        }
        return resolve(level, pos, null);
    }
}
