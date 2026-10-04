package dev.sdfg.mod.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Collision / outline shapes that follow {@link PipeSupport}. */
public final class PipeShapes {
    /** Pipe cross-section (core and arms), in pixels. */
    public static final float PIPE_SIZE = 5.0F;
    /** Closed valve core. */
    public static final float CLOSED_CORE_SIZE = 9.0F;

    private PipeShapes() {
    }

    public static VoxelShape shape(BlockState state, boolean closedCore) {
        PipeSupport support = state.getValue(PipeSupport.PROPERTY);
        double arm = PIPE_SIZE;
        double core = closedCore ? CLOSED_CORE_SIZE : PIPE_SIZE;
        double cx = 8.0;
        double cy = 8.0;
        double cz = 8.0;
        Direction toward = support.asDirection();
        if (toward != null) {
            double inset = (16.0 - core) / 2.0;
            cx = 8.0 + toward.getStepX() * inset;
            cy = 8.0 + toward.getStepY() * inset;
            cz = 8.0 + toward.getStepZ() * inset;
        }

        VoxelShape shape = cubeAt(cx, cy, cz, core);
        double armHalf = arm / 2.0;
        for (Direction direction : Direction.values()) {
            if (!state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                continue;
            }
            if (toward == direction) {
                continue;
            }
            shape = Shapes.or(shape, armToward(cx, cy, cz, armHalf, direction));
        }
        return shape;
    }

    private static VoxelShape cubeAt(double cx, double cy, double cz, double size) {
        double h = size / 2.0;
        return Block.box(cx - h, cy - h, cz - h, cx + h, cy + h, cz + h);
    }

    private static VoxelShape armToward(double cx, double cy, double cz, double half, Direction direction) {
        return switch (direction) {
            case NORTH -> Block.box(cx - half, cy - half, 0.0, cx + half, cy + half, cz - half);
            case SOUTH -> Block.box(cx - half, cy - half, cz + half, cx + half, cy + half, 16.0);
            case WEST -> Block.box(0.0, cy - half, cz - half, cx - half, cy + half, cz + half);
            case EAST -> Block.box(cx + half, cy - half, cz - half, 16.0, cy + half, cz + half);
            case DOWN -> Block.box(cx - half, 0.0, cz - half, cx + half, cy - half, cz + half);
            case UP -> Block.box(cx - half, cy + half, cz - half, cx + half, 16.0, cz + half);
        };
    }
}
