package dev.sdfg.mod.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/** Which face of the cell the pipe sits against. {@link #NONE} keeps it centered. */
public enum PipeSupport implements StringRepresentable {
    NONE,
    DOWN,
    UP,
    NORTH,
    SOUTH,
    WEST,
    EAST;

    public static final EnumProperty<PipeSupport> PROPERTY = EnumProperty.create("support", PipeSupport.class);

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }

    public @Nullable Direction asDirection() {
        return switch (this) {
            case NONE -> null;
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
        };
    }

    public static PipeSupport of(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }
}
