package dev.sdfg.mod;

import dev.sdfg.mod.client.WarpMoteParticle;
import dev.sdfg.mod.client.WarpSceneCapture;
import dev.sdfg.mod.client.WarpRenderer;
import dev.sdfg.mod.entity.ModEntities;
import dev.sdfg.mod.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
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
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ENTITY1.get(), WarpRenderer::new);
    }

    @SubscribeEvent
    static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.WARP_MOTE.get(), WarpMoteParticle.Provider::new);
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
