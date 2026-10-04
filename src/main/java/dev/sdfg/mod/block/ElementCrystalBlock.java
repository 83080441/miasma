package dev.sdfg.mod.block;

import com.mojang.serialization.MapCodec;
import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.element.Element;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Condensed element crystal under a condensation filter.
 * Age 1–10; visual buds map to four stages, and age 10 is the solid cluster.
 */
public class ElementCrystalBlock extends Block {
    public static final MapCodec<ElementCrystalBlock> CODEC = simpleCodec(ElementCrystalBlock::new);
    public static final IntegerProperty AGE = IntegerProperty.create("age", 1, 10);
    public static final IntegerProperty ELEMENT = IntegerProperty.create("element", 1, 8);
    public static final int SOLID_AGE = 10;

    private static final VoxelShape SMALL = box(3.0, 0.0, 3.0, 13.0, 4.0, 13.0);
    private static final VoxelShape MEDIUM = box(3.0, 0.0, 3.0, 13.0, 8.0, 13.0);
    private static final VoxelShape LARGE = box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);
    private static final VoxelShape CLUSTER = box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public ElementCrystalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AGE, SOLID_AGE)
                .setValue(ELEMENT, Element.AETHER.number()));
    }

    public static BlockBehaviour.Properties crystalProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(1.2F)
                .sound(SoundType.AMETHYST_CLUSTER)
                .noOcclusion()
                .forceSolidOn()
                .pushReaction(PushReaction.DESTROY);
    }

    public static BlockState fresh(Element element) {
        return ExampleMod.ELEMENT_CRYSTAL.get().defaultBlockState()
                .setValue(AGE, 1)
                .setValue(ELEMENT, element.number());
    }

    public static BlockState grow(BlockState state) {
        int age = Math.min(SOLID_AGE, state.getValue(AGE) + 1);
        return state.setValue(AGE, age);
    }

    public static Element elementOf(BlockState state) {
        Element element = Element.byNumber(state.getValue(ELEMENT));
        return element == null ? Element.AETHER : element;
    }

    public static boolean isSolid(BlockState state) {
        return state.getValue(AGE) >= SOLID_AGE;
    }

    /** Visual stage 0–3 for models (small / medium / large / cluster). */
    public static int visualStage(int age) {
        if (age >= SOLID_AGE) {
            return 3;
        }
        if (age >= 7) {
            return 2;
        }
        if (age >= 4) {
            return 1;
        }
        return 0;
    }

    @Override
    public MapCodec<ElementCrystalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, ELEMENT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Quartz is only the seed; once the bud exists it stays on the pedestal alone.
        return level.getBlockState(pos.below()).getBlock() instanceof CrystallizationPedestalBlock;
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
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (visualStage(state.getValue(AGE))) {
            case 0 -> SMALL;
            case 1 -> MEDIUM;
            case 2 -> LARGE;
            default -> CLUSTER;
        };
    }
}
