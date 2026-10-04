package dev.sdfg.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.sdfg.mod.block.CrystallizationPedestalBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws the quartz gem sitting on the crystallization pedestal. */
public class CrystallizationPedestalRenderer
        implements BlockEntityRenderer<CrystallizationPedestalBlockEntity, CrystallizationPedestalRenderState> {
    private final ItemModelResolver itemModelResolver;

    public CrystallizationPedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public CrystallizationPedestalRenderState createRenderState() {
        return new CrystallizationPedestalRenderState();
    }

    @Override
    public void extractRenderState(
            CrystallizationPedestalBlockEntity blockEntity,
            CrystallizationPedestalRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        int seed = (int) blockEntity.getBlockPos().asLong();
        this.itemModelResolver.updateForTopItem(
                state.quartz,
                blockEntity.quartz(),
                ItemDisplayContext.FIXED,
                blockEntity.getLevel(),
                null,
                seed
        );
    }

    @Override
    public void submit(
            CrystallizationPedestalRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        if (state.quartz.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        // Flat on the top plate, like a dropped item.
        poseStack.translate(0.5F, 0.90F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        state.quartz.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
