package dev.sdfg.mod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import dev.sdfg.mod.block.CauldronLidBlock;
import dev.sdfg.mod.block.ElementContainerBlock;
import dev.sdfg.mod.block.ElementPipeBlock;
import dev.sdfg.mod.block.HeatedCauldronBlock;
import dev.sdfg.mod.block.ValvePipeBlock;
import dev.sdfg.mod.block.ModBlockEntities;
import dev.sdfg.mod.element.ElementDiscovery;
import dev.sdfg.mod.inventory.VialMenu;
import dev.sdfg.mod.item.ModDataComponents;
import dev.sdfg.mod.item.VialItem;
import dev.sdfg.mod.entity.ModEntities;
import dev.sdfg.mod.fluid.ModFluids;
import dev.sdfg.mod.particle.ModParticles;
import dev.sdfg.mod.worldgen.ModWorldGen;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ExampleMod.MODID)
public class ExampleMod {
    public static final String MODID = "sdfg";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<VialMenu>> VIAL_MENU = MENUS.register(
            "element_container",
            () -> IMenuTypeExtension.create(VialMenu::fromNetwork)
    );

    public static final DeferredBlock<HeatedCauldronBlock> CAULDRON = BLOCKS.registerBlock(
            "cauldron",
            HeatedCauldronBlock::new,
            HeatedCauldronBlock::cauldronProperties
    );
    public static final DeferredItem<BlockItem> CAULDRON_ITEM = ITEMS.registerSimpleBlockItem("cauldron", CAULDRON);

    public static final DeferredBlock<CauldronLidBlock> CAULDRON_LID = BLOCKS.registerBlock(
            "cauldron_lid",
            CauldronLidBlock::new,
            CauldronLidBlock::lidProperties
    );
    public static final DeferredItem<BlockItem> CAULDRON_LID_ITEM = ITEMS.registerSimpleBlockItem("cauldron_lid", CAULDRON_LID);

    public static final DeferredBlock<ElementPipeBlock> ELEMENT_PIPE = BLOCKS.registerBlock(
            "element_pipe",
            ElementPipeBlock::new,
            ElementPipeBlock::pipeProperties
    );
    public static final DeferredItem<BlockItem> ELEMENT_PIPE_ITEM = ITEMS.registerSimpleBlockItem("element_pipe", ELEMENT_PIPE);

    public static final DeferredBlock<ValvePipeBlock> VALVE_PIPE = BLOCKS.registerBlock(
            "element_valve",
            ValvePipeBlock::new,
            ValvePipeBlock::valveProperties
    );
    public static final DeferredItem<BlockItem> VALVE_PIPE_ITEM = ITEMS.registerSimpleBlockItem("element_valve", VALVE_PIPE);

    public static final DeferredBlock<ElementContainerBlock> ELEMENT_CONTAINER = BLOCKS.registerBlock(
            "element_container",
            ElementContainerBlock::new,
            ElementContainerBlock::containerProperties
    );
    public static final DeferredItem<BlockItem> ELEMENT_CONTAINER_ITEM = ITEMS.registerSimpleBlockItem("element_container", ELEMENT_CONTAINER);

    /** Corked vial. Holds up to 100 of a container's mix. No recipe yet. */
    public static final DeferredItem<VialItem> VIAL = ITEMS.registerItem("vial", VialItem::new);

    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", p -> p.mapColor(MapColor.STONE));
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    /** Iron-look helmet that reveals Warp stats when looking at a node. */
    public static final DeferredItem<Item> REVEALING_HELMET = ITEMS.registerItem(
            "revealing_helmet",
            props -> new Item(props.humanoidArmor(ArmorMaterials.IRON, ArmorType.HELMET))
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sdfg")) 
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get());
                output.accept(REVEALING_HELMET.get());
                output.accept(CAULDRON_ITEM.get());
                output.accept(CAULDRON_LID_ITEM.get());
                output.accept(ELEMENT_PIPE_ITEM.get());
                output.accept(VALVE_PIPE_ITEM.get());
                output.accept(ELEMENT_CONTAINER_ITEM.get());
                output.accept(VIAL.get());
                for (ModFluids.PureLiquid liquid : ModFluids.ALL) {
                    output.accept(liquid.bucket().get());
                }
            }).build());

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModWorldGen.FEATURES.register(modEventBus);
        ElementDiscovery.ATTACHMENT_TYPES.register(modEventBus);

        modEventBus.addListener(ModEntities::registerAttributes);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }
}
