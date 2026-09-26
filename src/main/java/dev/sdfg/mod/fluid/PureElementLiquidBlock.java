package dev.sdfg.mod.fluid;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

/** Liquid block whose item form is the element bucket, so the reveal helmet can read it. */
public class PureElementLiquidBlock extends LiquidBlock {
    public PureElementLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    public Item asItem() {
        return this.fluid.getBucket();
    }
}
