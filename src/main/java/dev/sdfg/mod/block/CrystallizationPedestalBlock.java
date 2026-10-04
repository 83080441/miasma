package dev.sdfg.mod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Holds a single quartz gem. Condensation drips grow an element crystal in the
 * air block above when that quartz is present.
 */
public class CrystallizationPedestalBlock extends Block implements EntityBlock {
    public static final MapCodec<CrystallizationPedestalBlock> CODEC = simpleCodec(CrystallizationPedestalBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            box(2.0, 0.0, 2.0, 14.0, 2.0, 14.0),
            box(5.0, 2.0, 5.0, 11.0, 12.0, 11.0),
            box(2.0, 12.0, 2.0, 14.0, 14.0, 14.0)
    );

    public CrystallizationPedestalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties pedestalProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(1.5F)
                .sound(SoundType.STONE)
                .noOcclusion();
    }

    @Override
    public MapCodec<CrystallizationPedestalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrystallizationPedestalBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof CrystallizationPedestalBlockEntity pedestal)) {
            return InteractionResult.PASS;
        }
        if (pedestal.hasQuartz()) {
            return InteractionResult.PASS;
        }
        if (!stack.is(Items.QUARTZ)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            pedestal.setQuartz(stack.split(1));
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CrystallizationPedestalBlockEntity pedestal) || !pedestal.hasQuartz()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack taken = pedestal.takeQuartz();
            if (!taken.isEmpty() && !player.addItem(taken)) {
                player.drop(taken, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }
}
