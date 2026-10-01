package dev.sdfg.mod.block;

import com.mojang.serialization.MapCodec;
import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.element.ElementAmounts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

/** Glass tank that stores whatever a pipe network pulls out of a cauldron. */
public class ElementContainerBlock extends TransparentBlock implements EntityBlock {
    public static final MapCodec<ElementContainerBlock> CODEC = simpleCodec(ElementContainerBlock::new);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);
    /** One full element (100) fills the glass. Extra elements keep it full and change the mix color. */
    public static final int FULL_AMOUNT = ElementAmounts.MAX_AMOUNT;

    public ElementContainerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
    }

    public static BlockBehaviour.Properties containerProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .strength(0.3F)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .isValidSpawn((state, level, pos, entity) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false);
    }

    /** 0 empty, 1–3 partial, 4 full. */
    public static int fillLevel(ElementAmounts contents) {
        int total = 0;
        for (Element element : Element.values()) {
            total += contents.get(element);
        }
        if (total <= 0) {
            return 0;
        }
        if (total >= 76) {
            return 4;
        }
        if (total >= 51) {
            return 3;
        }
        if (total >= 26) {
            return 2;
        }
        return 1;
    }

    /** Updates the visible fill. The block entity is kept. */
    public static void applyFill(Level level, BlockPos pos, ElementAmounts contents) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(LEVEL)) {
            return;
        }
        int fill = fillLevel(contents);
        if (state.getValue(LEVEL) != fill) {
            level.setBlock(pos, state.setValue(LEVEL, fill), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public MapCodec<ElementContainerBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElementContainerBlockEntity(pos, state);
    }
}
