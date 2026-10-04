package dev.sdfg.mod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A pipe section with an open/closed gate. Closed, nothing flows through it.
 * Right-click toggles it. Starts closed.
 */
public class ValvePipeBlock extends PipeBlock {
    public static final MapCodec<ValvePipeBlock> CODEC = simpleCodec(ValvePipeBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public ValvePipeBlock(BlockBehaviour.Properties properties) {
        super(PipeShapes.PIPE_SIZE, properties);
        BlockState state = this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(OPEN, false)
                .setValue(PipeSupport.PROPERTY, PipeSupport.NONE);
        this.registerDefaultState(state);
    }

    public static BlockBehaviour.Properties valveProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    public static boolean isOpen(BlockState state) {
        return state.getBlock() instanceof ValvePipeBlock && state.getValue(OPEN);
    }

    @Override
    public MapCodec<ValvePipeBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = context.getClickedFace().getOpposite();
        BlockState state = this.defaultBlockState()
                .setValue(PipeSupport.PROPERTY, PipeSupportLogic.resolve(context.getLevel(), context.getClickedPos(), preferred));
        return withConnections(context.getLevel(), context.getClickedPos(), state);
    }

    private static BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state) {
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockState toggled = state.cycle(OPEN);
            level.setBlock(pos, toggled, Block.UPDATE_ALL);
            boolean open = toggled.getValue(OPEN);
            level.playSound(
                    null,
                    pos,
                    open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE,
                    SoundSource.BLOCKS,
                    0.6F,
                    open ? 1.2F : 0.8F
            );
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PipeShapes.shape(state, !state.getValue(OPEN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, OPEN, PipeSupport.PROPERTY);
    }
}
