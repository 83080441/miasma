package dev.sdfg.mod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Spout under a tank or on a pipe line. Pulls one locked element and drips it into
 * the empty block below, where an element crystal grows on the support underneath.
 */
public class CondensationFilterBlock extends Block implements EntityBlock {
    public static final MapCodec<CondensationFilterBlock> CODEC = simpleCodec(CondensationFilterBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            box(2.0, 12.0, 2.0, 14.0, 16.0, 14.0),
            box(5.0, 6.0, 5.0, 11.0, 12.0, 11.0),
            box(6.0, 2.0, 6.0, 10.0, 6.0, 10.0)
    );

    public CondensationFilterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties filterProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.5F)
                .sound(SoundType.COPPER)
                .noOcclusion();
    }

    @Override
    public MapCodec<CondensationFilterBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
        return state;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CondensationFilterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (world, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof CondensationFilterBlockEntity filter) {
                filter.serverTick();
            }
        };
    }
}
