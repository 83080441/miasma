package dev.sdfg.mod.client;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.element.ElementDiscovery;
import dev.sdfg.mod.entity.WarpEntity;
import dev.sdfg.mod.entity.WarpSubtype;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Camera-facing warp. Supports static, vortex, binary, and amoeba.
 */
public class WarpRenderer extends EntityRenderer<WarpEntity, WarpRenderState> {
    /** Each binary node is 20% of a block. */
    private static final float BINARY_NODE_SCALE = 0.2F;
    /** Orbit radius so both nodes stay inside the same 1×1. */
    private static final float BINARY_ORBIT_RADIUS = 0.25F;
    /** Outer shell as a fraction of the node's warp scale (local radius is 1). */
    private static final float SHELL_SCALE = 0.42F;
    /** End-portal core edge is this many times smaller than the colored shell. */
    private static final float CORE_SHRINK = 6.0F;
    /** Colored shell is this many times smaller than that same base size. */
    private static final float SHELL_SHRINK = 3.0F;
    private static final float SHELL_ALPHA = 0.42F;
    private static final RenderType SHELL_RENDER_TYPE = RenderTypes.entityTranslucentEmissive(
            Identifier.fromNamespaceAndPath(ExampleMod.MODID, "textures/misc/white.png")
    );

    public WarpRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void submit(WarpRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float anchorX = 0.0F;
        float anchorY = 0.0F;
        float anchorZ = 0.0F;
        if (state.subtype == WarpSubtype.BINARY) {
            float[] nearer = submitBinary(state, poseStack, collector, camera);
            anchorX = nearer[0];
            anchorY = nearer[1];
            anchorZ = nearer[2];
        } else {
            poseStack.pushPose();
            poseStack.mulPose(camera.orientation);
            WarpRenderHelper.submitWarpDisc(poseStack, collector, state);
            poseStack.popPose();
        }
        if (state.showDiscovery) {
            submitDiscoveryNucleus(state, poseStack, collector, anchorX, anchorY, anchorZ);
        }
        super.submit(state, poseStack, collector, camera);
    }

    /**
     * Tiny End-portal cube inside a larger translucent cube tinted with the element color.
     * Each cube tumbles on all three axes at its own rates.
     */
    private static void submitDiscoveryNucleus(
            WarpRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float anchorX,
            float anchorY,
            float anchorZ
    ) {
        float t = state.ageInTicks;
        float base = Math.max(0.10F, SHELL_SCALE * state.warpScale);
        base *= 1.0F + 0.06F * Mth.sin(t * 0.08F);
        float shell = base / SHELL_SHRINK;
        float core = shell / CORE_SHRINK;
        // Wide 3D loop. It is meant to leave the warp disc.
        float orbit = 0.55F;
        float ox = anchorX + orbit * Mth.sin(t * 0.045F);
        float oy = anchorY + orbit * 0.72F * Mth.sin(t * 0.031F + 1.7F);
        float oz = anchorZ + orbit * 0.88F * Mth.cos(t * 0.038F);

        int color = state.discoveryColor;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        poseStack.pushPose();
        poseStack.translate(ox, oy, oz);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotation(-t * 0.083F));
        poseStack.mulPose(Axis.XP.rotation(t * 0.061F));
        poseStack.mulPose(Axis.ZP.rotation(-t * 0.044F));
        poseStack.scale(core, core, core);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        AbstractEndPortalRenderer.submitSpecial(RenderTypes.endPortal(), poseStack, collector);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotation(t * 0.031F));
        poseStack.mulPose(Axis.XP.rotation(-t * 0.047F));
        poseStack.mulPose(Axis.ZP.rotation(t * 0.019F));
        poseStack.scale(shell, shell, shell);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        submitElementShell(poseStack, collector, r, g, b, SHELL_ALPHA);
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void submitElementShell(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float r,
            float g,
            float b,
            float a
    ) {
        collector.submitCustomGeometry(poseStack, SHELL_RENDER_TYPE, (pose, buffer) -> {
            cubeFace(buffer, pose, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, r, g, b, a, 0, 0, 1);
            cubeFace(buffer, pose, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, r, g, b, a, 0, 0, -1);
            cubeFace(buffer, pose, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, r, g, b, a, 1, 0, 0);
            cubeFace(buffer, pose, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, r, g, b, a, -1, 0, 0);
            cubeFace(buffer, pose, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, r, g, b, a, 0, 1, 0);
            cubeFace(buffer, pose, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, r, g, b, a, 0, -1, 0);
        });
    }

    private static void cubeFace(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float r, float g, float b, float a,
            float nx, float ny, float nz
    ) {
        int light = 0xF000F0;
        int overlay = OverlayTexture.NO_OVERLAY;
        buffer.addVertex(pose, x0, y0, z0).setColor(r, g, b, a).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        buffer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        buffer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }

    /** @return local offset of the node closer to the camera, where the nucleus sits. */
    private float[] submitBinary(WarpRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float strength = Mth.clamp(state.distortion, 1, 100) / 100.0F;
        float t = state.ageInTicks;

        // Stable per-entity phase so each binary dances differently, not frame-random.
        float phase = (float) (state.x * 12.9898 + state.y * 78.233 + state.z * 37.719);
        float p1 = phase * 0.17F;
        float p2 = phase * 0.31F;
        float p3 = phase * 0.53F;

        // Tumbling orbital plane (not locked to Y): yaw + pitch + roll wobble.
        float yaw = t * (0.045F + strength * 0.09F) + p1
                + 0.55F * Mth.sin(t * 0.027F + p2)
                + 0.35F * Mth.sin(t * 0.071F + p3);
        float pitch = 0.85F * Mth.sin(t * 0.033F + p1)
                + 0.45F * Mth.sin(t * 0.061F + p2)
                + 0.25F * Mth.cos(t * 0.019F + p3);
        float roll = 0.65F * Mth.sin(t * 0.041F + p2)
                + 0.40F * Mth.cos(t * 0.023F + p1);

        // Separation breathes: nodes approach / pull apart inside the 1×1.
        float separation = BINARY_ORBIT_RADIUS
                * (0.55F + 0.45F * (0.5F + 0.5F * Mth.sin(t * (0.038F + strength * 0.05F) + p3)))
                * (0.85F + 0.15F * Mth.sin(t * 0.091F + p1));
        separation = Mth.clamp(separation, 0.10F, 0.32F);

        // Unit axis after yaw/pitch/roll — a dancing direction in 3D.
        float cy = Mth.cos(yaw);
        float sy = Mth.sin(yaw);
        float cp = Mth.cos(pitch);
        float sp = Mth.sin(pitch);
        float cr = Mth.cos(roll);
        float sr = Mth.sin(roll);

        // Start from +X, apply roll → pitch → yaw (local dance frame).
        float lx = cr;
        float ly = sr;
        float lz = 0.0F;

        float px = lx;
        float py = ly * cp - lz * sp;
        float pz = ly * sp + lz * cp;

        float ax = px * cy - pz * sy;
        float ay = py;
        float az = px * sy + pz * cy;

        // Normalize (should already be ~1, but keep safe).
        float len = Mth.sqrt(ax * ax + ay * ay + az * az);
        if (len > 1.0E-4F) {
            ax /= len;
            ay /= len;
            az /= len;
        }

        float ox = ax * separation;
        float oy = ay * separation;
        float oz = az * separation;

        // Extra micro-jitter so the path isn't a clean ellipse.
        float jx = 0.03F * Mth.sin(t * 0.13F + p2);
        float jy = 0.03F * Mth.cos(t * 0.11F + p3);
        float jz = 0.03F * Mth.sin(t * 0.17F + p1);

        float nodeAx = ox + jx;
        float nodeAy = oy + jy;
        float nodeAz = oz + jz;
        float nodeBx = -ox - jx;
        float nodeBy = -oy - jy;
        float nodeBz = -oz - jz;
        submitBinaryNode(state, poseStack, collector, camera, nodeAx, nodeAy, nodeAz, 1.0F);
        submitBinaryNode(state, poseStack, collector, camera, nodeBx, nodeBy, nodeBz, -1.0F);

        Vec3 cameraPos = camera.pos;
        double distA = cameraPos.distanceToSqr(state.x + nodeAx, state.y + nodeAy, state.z + nodeAz);
        double distB = cameraPos.distanceToSqr(state.x + nodeBx, state.y + nodeBy, state.z + nodeBz);
        return distA <= distB
                ? new float[]{nodeAx, nodeAy, nodeAz}
                : new float[]{nodeBx, nodeBy, nodeBz};
    }

    private void submitBinaryNode(
            WarpRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            float ox,
            float oy,
            float oz,
            float spinSign
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 nodeWorld = new Vec3(state.x + ox, state.y + oy, state.z + oz);
        Vec3 projected = minecraft.gameRenderer.projectPointToScreen(nodeWorld);

        state.nodeOffsetX = ox;
        state.nodeOffsetY = oy;
        state.nodeOffsetZ = oz;
        state.nodeCenterU = (float) projected.x * 0.5F + 0.5F;
        state.nodeCenterV = (float) (-projected.y) * 0.5F + 0.5F;
        state.centerU = state.nodeCenterU;
        state.centerV = state.nodeCenterV;
        state.warpScale = BINARY_NODE_SCALE;
        state.radiusUv = Mth.clamp(state.radiusUv, 0.015F, 0.10F);

        poseStack.pushPose();
        poseStack.translate(ox, oy, oz);
        poseStack.mulPose(camera.orientation);
        WarpRenderHelper.submitWarpDisc(poseStack, collector, state, spinSign);
        poseStack.popPose();
    }

    @Override
    public WarpRenderState createRenderState() {
        return new WarpRenderState();
    }

    @Override
    public void extractRenderState(WarpEntity entity, WarpRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.distortion = entity.getDistortion();
        state.subtype = entity.getSubtype();
        state.lightCoords = 0xF000F0;

        Minecraft minecraft = Minecraft.getInstance();
        Vec3 center = new Vec3(state.x, state.y, state.z);
        Vec3 projected = minecraft.gameRenderer.projectPointToScreen(center);

        state.centerU = (float) projected.x * 0.5F + 0.5F;
        state.centerV = (float) (-projected.y) * 0.5F + 0.5F;
        state.nodeCenterU = state.centerU;
        state.nodeCenterV = state.centerV;

        float distance = Mth.sqrt((float) Math.max(state.distanceToCameraSq, 0.0001));
        float size = state.subtype == WarpSubtype.BINARY ? BINARY_NODE_SCALE : 0.5F;
        float approx = (size * 0.55F) / distance;
        state.radiusUv = Mth.clamp(approx * 0.55F, 0.015F, 0.18F);
        state.warpScale = size;

        state.showDiscovery = false;
        state.discoveryColor = 0;
        Player player = minecraft.player;
        if (player != null && player.isScoping() && ElementDiscovery.lookedWarp(player) == entity) {
            entity.getElement().ifPresent(element -> {
                state.showDiscovery = true;
                state.discoveryColor = 0xFF000000 | element.color();
            });
        }
    }

    @Override
    protected int getBlockLightLevel(WarpEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    protected boolean affectedByCulling(WarpEntity entity) {
        return false;
    }

    @Override
    protected boolean shouldShowName(WarpEntity entity, double distanceToCameraSq) {
        return false;
    }
}
