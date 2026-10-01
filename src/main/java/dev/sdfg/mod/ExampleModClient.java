package dev.sdfg.mod;

import java.util.List;

import dev.sdfg.mod.client.CauldronBubbleParticle;
import dev.sdfg.mod.client.CauldronWaterTint;
import dev.sdfg.mod.client.ElementContainerTint;
import dev.sdfg.mod.client.ElementMoteParticle;
import dev.sdfg.mod.client.GnomeModel;
import dev.sdfg.mod.client.GnomeRenderer;
import dev.sdfg.mod.client.WarpMoteParticle;
import dev.sdfg.mod.client.WarpSceneCapture;
import dev.sdfg.mod.client.WarpRenderer;
import dev.sdfg.mod.entity.ModEntities;
import dev.sdfg.mod.fluid.ModFluids;
import dev.sdfg.mod.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = ExampleMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ExampleModClient {
    public ExampleModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ExampleMod.LOGGER.info("HELLO FROM CLIENT SETUP");
        ExampleMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        event.enqueueWork(() -> WarpSceneCapture.get().ensureRegistered());
    }

    @SubscribeEvent
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GnomeModel.LAYER_LOCATION, GnomeModel::createBodyLayer);
    }

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ENTITY1.get(), WarpRenderer::new);
        event.registerEntityRenderer(ModEntities.GNOME.get(), GnomeRenderer::new);
    }

    @SubscribeEvent
    static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.WARP_MOTE.get(), WarpMoteParticle.Provider::new);
        event.registerSpriteSet(ModParticles.ELEMENT_MOTE.get(), ElementMoteParticle.Provider::new);
        event.registerSpriteSet(ModParticles.CAULDRON_BUBBLE.get(), CauldronBubbleParticle.Provider::new);
    }

    @SubscribeEvent
    static void registerFluidModels(RegisterFluidModelsEvent event) {
        Material still = new Material(Identifier.withDefaultNamespace("block/water_still"));
        Material flow = new Material(Identifier.withDefaultNamespace("block/water_flow"));
        Material overlay = new Material(Identifier.withDefaultNamespace("block/water_overlay"));
        for (ModFluids.PureLiquid liquid : ModFluids.ALL) {
            FluidModel.Unbaked model = new FluidModel.Unbaked(
                    still,
                    flow,
                    overlay,
                    FluidTintSources.constant(0xFF000000 | liquid.element().color())
            );
            event.register(model, liquid.still(), liquid.flowing());
        }
    }

    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(CauldronWaterTint.INSTANCE), ExampleMod.CAULDRON.get());
        event.register(List.of(ElementContainerTint.INSTANCE), ExampleMod.ELEMENT_CONTAINER.get());
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        IClientFluidTypeExtensions waterOverlay = new IClientFluidTypeExtensions() {
            @Override
            public Identifier getRenderOverlayTexture(Minecraft mc) {
                return Identifier.withDefaultNamespace("textures/misc/underwater.png");
            }
        };
        for (ModFluids.PureLiquid liquid : ModFluids.ALL) {
            event.registerFluidType(waterOverlay, liquid.type().get());
        }
    }

    @SubscribeEvent
    static void onAfterSky(RenderLevelStageEvent.AfterSky event) {
        WarpSceneCapture.beginFrame();
    }

    @SubscribeEvent
    static void onAfterOpaque(RenderLevelStageEvent.AfterOpaqueBlocks event) {
        // Prefer capturing once the terrain is drawn so the warp samples real world pixels.
        WarpSceneCapture.captureThisFrame();
    }
}
