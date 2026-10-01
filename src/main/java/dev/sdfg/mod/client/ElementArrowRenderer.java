package dev.sdfg.mod.client;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.entity.ElementArrow;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Earth is a solid red spike, water a small tinted square. Fire is only the flame particles. */
public class ElementArrowRenderer extends EntityRenderer<ElementArrow, ElementArrowRenderer.State> {
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "textures/misc/white.png");
    private static final RenderType SOLID = RenderTypes.entitySolid(WHITE);
    private static final RenderType GLASS = RenderTypes.entityTranslucentEmissive(WHITE);
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float SQUARE = 0.11F;

    public ElementArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ElementArrow entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.elementNumber = entity.element().number();
        state.xRot = entity.getXRot(partialTick);
        state.yRot = entity.getYRot(partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.elementNumber == Element.FIRE.number()) {
            super.submit(state, poseStack, collector, camera);
            return;
        }
        poseStack.pushPose();
        if (state.elementNumber == Element.WATER.number()) {
            poseStack.mulPose(camera.orientation);
            int color = Element.WATER.color();
            collector.submitCustomGeometry(poseStack, GLASS, (pose, buffer) -> square(
                    buffer, pose,
                    ((color >> 16) & 255) / 255.0F,
                    ((color >> 8) & 255) / 255.0F,
                    (color & 255) / 255.0F,
                    0.55F
            ));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot));
            collector.submitCustomGeometry(poseStack, SOLID, (pose, buffer) -> pick(buffer, pose, 0.92F, 0.12F, 0.14F));
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    /** Opaque spike along +X: three side faces and a base, about the size of an arrow. */
    private static void pick(VertexConsumer buffer, PoseStack.Pose pose, float r, float g, float b) {
        float tipX = 0.42F;
        float tail = -0.18F;
        float up = 0.14F;
        float side = 0.12F;
        face(buffer, pose, tipX, 0.0F, 0.0F, tail, up, 0.0F, tail, -up * 0.5F, side, r, g, b);
        face(buffer, pose, tipX, 0.0F, 0.0F, tail, -up * 0.5F, -side, tail, up, 0.0F, r, g, b);
        face(buffer, pose, tipX, 0.0F, 0.0F, tail, -up * 0.5F, side, tail, -up * 0.5F, -side, r, g, b);
        face(buffer, pose, tail, up, 0.0F, tail, -up * 0.5F, -side, tail, -up * 0.5F, side, r, g, b);
    }

    /** One solid face. The normal comes from the three corners, outward when the order is counter-clockwise from outside. */
    private static void face(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float r, float g, float b
    ) {
        float ux = x2 - x1;
        float uy = y2 - y1;
        float uz = z2 - z1;
        float vx = x3 - x1;
        float vy = y3 - y1;
        float vz = z3 - z1;
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 1.0E-5F) {
            nx /= length;
            ny /= length;
            nz /= length;
        }
        quad(buffer, pose, x1, y1, z1, x2, y2, z2, x3, y3, z3, x3, y3, z3, r, g, b, 1.0F, nx, ny, nz);
    }

    private static void square(VertexConsumer buffer, PoseStack.Pose pose, float r, float g, float b, float a) {
        quad(buffer, pose,
                -SQUARE, -SQUARE, 0.0F,
                SQUARE, -SQUARE, 0.0F,
                SQUARE, SQUARE, 0.0F,
                -SQUARE, SQUARE, 0.0F,
                r, g, b, a, 0.0F, 0.0F, 1.0F);
        quad(buffer, pose,
                -SQUARE, SQUARE, 0.0F,
                SQUARE, SQUARE, 0.0F,
                SQUARE, -SQUARE, 0.0F,
                -SQUARE, -SQUARE, 0.0F,
                r, g, b, a, 0.0F, 0.0F, -1.0F);
    }

    private static void quad(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float r, float g, float b, float a,
            float nx, float ny, float nz
    ) {
        int overlay = OverlayTexture.NO_OVERLAY;
        buffer.addVertex(pose, x0, y0, z0).setColor(r, g, b, a).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
        buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
        buffer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
        buffer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
    }

    public static final class State extends EntityRenderState {
        public int elementNumber = Element.EARTH.number();
        public float xRot;
        public float yRot;
    }
}
