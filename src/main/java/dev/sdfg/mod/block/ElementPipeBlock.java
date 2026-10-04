package dev.sdfg.mod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Iron-textured pipe. Connects to lids, pipes, valves, containers, and condensation filters. */
public class ElementPipeBlock extends PipeBlock {
    public static final MapCodec<ElementPipeBlock> CODEC = simpleCodec(ElementPipeBlock::new);
    public ElementPipeBlock(BlockBehaviour.Properties properties) {
        super(PipeShapes.PIPE_SIZE, properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(PipeSupport.PROPERTY, PipeSupport.NONE));
    }

    public static BlockBehaviour.Properties pipeProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    @Override
    public MapCodec<ElementPipeBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = context.getClickedFace().getOpposite();
        BlockState state = this.defaultBlockState()
                .setValue(PipeSupport.PROPERTY, PipeSupportLogic.resolve(context.getLevel(), context.getClickedPos(), preferred));
        return withConnections(context.getLevel(), context.getClickedPos(), state);
    }

    public static BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(
                    PROPERTY_BY_DIRECTION.get(direction),
                    PipeNetwork.connects(level.getBlockState(pos.relative(direction)))
            );
        }
        return state;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random
    ) {
        PipeSupport support = PipeSupportLogic.update(level, pos, state.getValue(PipeSupport.PROPERTY));
        return state
                .setValue(PipeSupport.PROPERTY, support)
                .setValue(PROPERTY_BY_DIRECTION.get(direction), PipeNetwork.connects(neighbourState));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PipeShapes.shape(state, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, PipeSupport.PROPERTY);
    }
}
