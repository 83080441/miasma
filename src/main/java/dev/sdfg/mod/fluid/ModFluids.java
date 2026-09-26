package dev.sdfg.mod.fluid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.element.Element;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * One finite water-like fluid per {@link Element}, plus its bucket.
 * See {@code plan/LIQUID_README.md}.
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ExampleMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, ExampleMod.MODID);

    private static final List<PureLiquid> ALL_MUTABLE = new ArrayList<>();
    public static final List<PureLiquid> ALL = Collections.unmodifiableList(ALL_MUTABLE);

    static {
        for (Element element : Element.values()) {
            ALL_MUTABLE.add(register(element));
        }
    }

    private ModFluids() {
    }

    private static PureLiquid register(Element element) {
        String id = element.id();
        Pending pending = new Pending();
        pending.type = FLUID_TYPES.register(id, () -> new FluidType(waterLike(id)));
        pending.still = FLUIDS.register(id, () -> new PureElementFluid.Source(element, properties(pending)));
        pending.flowing = FLUIDS.register("flowing_" + id, () -> new PureElementFluid.Flowing(element, properties(pending)));
        pending.block = ExampleMod.BLOCKS.registerBlock(
                id,
                props -> new PureElementLiquidBlock(pending.still.get(), props),
                ModFluids::liquidBlock
        );
        pending.bucket = ExampleMod.ITEMS.registerItem(
                id + "_bucket",
                props -> new BucketItem(pending.still.get(), props.stacksTo(1))
        );
        return new PureLiquid(element, pending.type, pending.still, pending.flowing, pending.block, pending.bucket);
    }

    private static BaseFlowingFluid.Properties properties(Pending pending) {
        return new BaseFlowingFluid.Properties(pending.type, pending.still, pending.flowing)
                .block(() -> pending.block.get())
                .bucket(pending.bucket)
                .explosionResistance(100.0F);
    }

    /** Same body as water, but a source never forms from flowing blocks. */
    private static FluidType.Properties waterLike(String id) {
        return FluidType.Properties.create()
                .descriptionId("block." + ExampleMod.MODID + "." + id)
                .fallDistanceModifier(0.0F)
                .canExtinguish(true)
                .canConvertToSource(false)
                .supportsBoating(true)
                .canHydrate(true)
                .isWaterLike(true)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
    }

    private static BlockBehaviour.Properties liquidBlock() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WATER)
                .replaceable()
                .noCollision()
                .strength(100.0F)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY);
    }

    public record PureLiquid(
            Element element,
            DeferredHolder<FluidType, FluidType> type,
            DeferredHolder<Fluid, PureElementFluid.Source> still,
            DeferredHolder<Fluid, PureElementFluid.Flowing> flowing,
            DeferredBlock<PureElementLiquidBlock> block,
            DeferredItem<BucketItem> bucket
    ) {}

    private static final class Pending {
        private DeferredHolder<FluidType, FluidType> type;
        private DeferredHolder<Fluid, PureElementFluid.Source> still;
        private DeferredHolder<Fluid, PureElementFluid.Flowing> flowing;
        private DeferredBlock<PureElementLiquidBlock> block;
        private DeferredItem<BucketItem> bucket;
    }
}
