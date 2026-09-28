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
    /** Discovery square, in blocks. */
    private static final float DISCOVERY_SIZE = 0.20F;
    private static final RenderType DISCOVERY_RENDER_TYPE = RenderTypes.entityTranslucentEmissive(
            Identifier.fromNamespaceAndPath(ExampleMod.MODID, "textures/misc/white.png")
    );

    public WarpRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void submit(WarpRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.subtype == WarpSubtype.BINARY) {
            submitBinary(state, poseStack, collector, camera);
        } else {
            poseStack.pushPose();
            poseStack.mulPose(camera.orientation);
            WarpRenderHelper.submitWarpDisc(poseStack, collector, state);
            poseStack.popPose();
        }
        if (state.showDiscovery) {
            submitDiscoverySquare(state, poseStack, collector);
        }
        super.submit(state, poseStack, collector, camera);
    }

    /** Colored square in the nucleus: it spins and drifts while the spyglass is on this warp. */
    private static void submitDiscoverySquare(WarpRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        float t = state.ageInTicks;
        float half = DISCOVERY_SIZE * 0.5F;
        float ox = 0.10F * Mth.sin(t * 0.05F);
        float oy = 0.07F * Mth.sin(t * 0.07F + 1.3F);
        float oz = 0.10F * Mth.cos(t * 0.04F);
        int color = state.discoveryColor;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        poseStack.pushPose();
        poseStack.translate(ox, oy, oz);
        poseStack.mulPose(Axis.YP.rotation(t * 0.04F));
        poseStack.mulPose(Axis.XP.rotation(t * 0.03F));
        collector.submitCustomGeometry(poseStack, DISCOVERY_RENDER_TYPE, (pose, buffer) -> {
            quad(buffer, pose, -half, -half, half, half, 0.0F, r, g, b, 1.0F);
            quad(buffer, pose, half, -half, -half, half, -0.001F, r, g, b, -1.0F);
        });
        poseStack.popPose();
    }

    private static void quad(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x0,
            float y0,
            float x1,
            float y1,
            float z,
            float r,
            float g,
            float b,
            float normalZ
    ) {
        buffer.addVertex(pose, x0, y0, z).setColor(r, g, b, 1.0F).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0.0F, 0.0F, normalZ);
        buffer.addVertex(pose, x1, y0, z).setColor(r, g, b, 1.0F).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0.0F, 0.0F, normalZ);
        buffer.addVertex(pose, x1, y1, z).setColor(r, g, b, 1.0F).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0.0F, 0.0F, normalZ);
        buffer.addVertex(pose, x0, y1, z).setColor(r, g, b, 1.0F).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0.0F, 0.0F, normalZ);
    }

    private void submitBinary(WarpRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
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

        submitBinaryNode(state, poseStack, collector, camera, ox + jx, oy + jy, oz + jz, 1.0F);
        submitBinaryNode(state, poseStack, collector, camera, -ox - jx, -oy - jy, -oz - jz, -1.0F);
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
