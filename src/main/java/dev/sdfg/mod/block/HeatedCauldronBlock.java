package dev.sdfg.mod.block;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Vanilla-looking cauldron. Heat is whatever block sits underneath.
 * Empty and hot: gray vapor. Water and hot: bubbles. Cold: quiet.
 */
public class HeatedCauldronBlock extends Block implements EntityBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 3);
    public static final int MAX_LEVEL = 3;
    /** Gray steam {@code #9A9A9A}. */
    private static final int STEAM_COLOR = 0xFF9A9A9A;

    public static final TagKey<Block> HEAT_SOURCES = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(ExampleMod.MODID, "cauldron_heat")
    );

    private static final VoxelShape SHAPE = Shapes.join(
            Shapes.block(),
            Shapes.or(
                    box(0.0, 0.0, 4.0, 16.0, 3.0, 12.0),
                    box(4.0, 0.0, 0.0, 12.0, 3.0, 16.0),
                    box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0),
                    box(2.0, 4.0, 2.0, 14.0, 16.0, 14.0)
            ),
            BooleanOp.ONLY_FIRST
    );

    public HeatedCauldronBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
    }

    public static BlockBehaviour.Properties cauldronProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0F)
                .noOcclusion()
                .sound(SoundType.METAL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
        int fill = state.getValue(LEVEL);
        if (stack.is(Items.WATER_BUCKET) && fill < MAX_LEVEL) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(LEVEL, MAX_LEVEL), Block.UPDATE_ALL);
                HeatedCauldronBlockEntity cauldron = entityAt(level, pos);
                if (cauldron != null) {
                    cauldron.fillWater();
                }
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.BUCKET) && fill > 0) {
            HeatedCauldronBlockEntity cauldron = entityAt(level, pos);
            if (cauldron != null && cauldron.isSealed()) {
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(LEVEL, 0), Block.UPDATE_ALL);
                if (cauldron != null) {
                    cauldron.clearBrew();
                }
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET)));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatedCauldronBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (world, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof HeatedCauldronBlockEntity cauldron) {
                cauldron.serverTick();
            }
        };
    }

    private static HeatedCauldronBlockEntity entityAt(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof HeatedCauldronBlockEntity cauldron ? cauldron : null;
    }

    /** Lit campfire, or a block in {@code #sdfg:cauldron_heat}. */
    public static boolean isHeated(LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return CampfireBlock.isLitCampfire(below) || below.is(HEAT_SOURCES);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!isHeated(level, pos)) {
            return;
        }
        int fill = state.getValue(LEVEL);
        if (fill <= 0) {
            double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
            level.addParticle(
                    ColorParticleOption.create(ModParticles.ELEMENT_MOTE.get(), STEAM_COLOR),
                    x, y, z,
                    0.0, 0.0, 0.0
            );
            return;
        }
        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
        double y = pos.getY() + 0.25 + fill * 0.18;
        double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
        level.addParticle(
                ColorParticleOption.create(ModParticles.CAULDRON_BUBBLE.get(), bubbleColor(level, pos, random)),
                x, y, z,
                0.0, 0.02, 0.0
        );
    }

    /** Water stays blue. After an item is absorbed, each bubble picks a color in proportion to the mix. */
    private static int bubbleColor(Level level, BlockPos pos, RandomSource random) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof HeatedCauldronBlockEntity cauldron) {
            return cauldron.contents().pickWeighted(random)
                    .map(element -> 0xFF000000 | element.color())
                    .orElse(0xFF000000 | Element.WATER.color());
        }
        return 0xFF000000 | Element.WATER.color();
    }
}
