package dev.sdfg.mod.client;

import dev.sdfg.mod.entity.WarpSubtype;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * Client-only space distortion (no PNG).
 * Subtypes: static, vortex, binary, amoeba, virus.
 */
public final class WarpRenderHelper {
    public static final RenderType WARP_RENDER_TYPE = RenderTypes.entityTranslucent(WarpSceneCapture.TEXTURE_ID);

    private static final int SEGMENTS = 72;
    private static final int AMOEBA_SEGMENTS = 96;
    private static final int RINGS = 16;
    private static final float OUTER_RADIUS = 1.0F;
    private static final int FULL_BRIGHT = 0xF000F0;

    private WarpRenderHelper() {
    }

    public static void submitWarpDisc(PoseStack poseStack, SubmitNodeCollector collector, WarpRenderState state) {
        submitWarpDisc(poseStack, collector, state, 1.0F);
    }

    public static void submitWarpDisc(PoseStack poseStack, SubmitNodeCollector collector, WarpRenderState state, float spinSign) {
        WarpSceneCapture.captureThisFrame();
        if (!WarpSceneCapture.get().hasCapture()) {
            return;
        }

        poseStack.pushPose();
        poseStack.scale(state.warpScale, state.warpScale, state.warpScale);

        float strength = Mth.clamp(state.distortion, 1, 100) / 100.0F;
        float chroma = strength * 0.016F;
        final float sign = spinSign;

        collector.submitCustomGeometry(poseStack, WARP_RENDER_TYPE,
                (pose, buffer) -> buildWarpDisc(state, pose, buffer, 1.0F, 0.82F, 0.82F, -chroma, strength, sign));
        collector.submitCustomGeometry(poseStack, WARP_RENDER_TYPE,
                (pose, buffer) -> buildWarpDisc(state, pose, buffer, 0.85F, 1.0F, 0.90F, 0.0F, strength, sign));
        collector.submitCustomGeometry(poseStack, WARP_RENDER_TYPE,
                (pose, buffer) -> buildWarpDisc(state, pose, buffer, 0.82F, 0.88F, 1.0F, chroma, strength, sign));

        poseStack.popPose();
    }

    private static void buildWarpDisc(
            WarpRenderState state,
            PoseStack.Pose pose,
            VertexConsumer buffer,
            float r,
            float g,
            float b,
            float uvBias,
            float strength,
            float spinSign
    ) {
        boolean organic = state.subtype == WarpSubtype.AMOEBA || state.subtype == WarpSubtype.VIRUS;
        int segments = organic ? AMOEBA_SEGMENTS : SEGMENTS;

        for (int ring = 0; ring < RINGS; ring++) {
            float ring0 = ring / (float) RINGS;
            float ring1 = (ring + 1) / (float) RINGS;

            for (int i = 0; i < segments; i++) {
                float ang0 = i / (float) segments * Mth.TWO_PI;
                float ang1 = (i + 1) / (float) segments * Mth.TWO_PI;

                float edge0 = organicEdge(ang0, state);
                float edge1 = organicEdge(ang1, state);

                float r00 = ring0 * edge0;
                float r01 = ring1 * edge0;
                float r10 = ring0 * edge1;
                float r11 = ring1 * edge1;

                float c0 = Mth.cos(ang0);
                float s0 = Mth.sin(ang0);
                float c1 = Mth.cos(ang1);
                float s1 = Mth.sin(ang1);

                float a00 = softEdgeAlpha(ring0);
                float a01 = softEdgeAlpha(ring1);
                float a10 = softEdgeAlpha(ring0);
                float a11 = softEdgeAlpha(ring1);

                warpVertex(buffer, pose, state, c0 * r00, s0 * r00, r, g, b, a00, uvBias, strength, spinSign, edge0);
                warpVertex(buffer, pose, state, c0 * r01, s0 * r01, r, g, b, a01, uvBias, strength, spinSign, edge0);
                warpVertex(buffer, pose, state, c1 * r11, s1 * r11, r, g, b, a11, uvBias, strength, spinSign, edge1);
                warpVertex(buffer, pose, state, c1 * r10, s1 * r10, r, g, b, a10, uvBias, strength, spinSign, edge1);
            }
        }
    }

    private static float organicEdge(float angle, WarpRenderState state) {
        if (state.subtype == WarpSubtype.VIRUS) {
            return virusRadius(angle, state);
        }
        if (state.subtype == WarpSubtype.AMOEBA) {
            return amoebaRadius(angle, state);
        }
        return OUTER_RADIUS;
    }

    /**
     * Living bacterial silhouette: irregular polar radius that mutates over time,
     * clamped so the blob stays inside the entity's 1×1 footprint.
     */
    private static float amoebaRadius(float angle, WarpRenderState state) {
        float t = state.ageInTicks;
        float phase = (float) (state.x * 7.13 + state.y * 19.7 + state.z * 3.91);

        // Slow body reshape + faster membrane ripple.
        float body = 0.22F * Mth.sin(2.0F * angle + t * 0.035F + phase)
                + 0.16F * Mth.sin(3.0F * angle - t * 0.048F + phase * 1.3F)
                + 0.11F * Mth.sin(5.0F * angle + t * 0.062F + phase * 0.7F)
                + 0.08F * Mth.sin(7.0F * angle - t * 0.091F + phase * 2.1F);

        // Occasional lobe push — feels like the cell "breathing" a protrusion.
        float lobe = 0.14F * Mth.sin(angle * 1.0F + t * 0.021F + phase)
                * Mth.sin(t * 0.017F + phase * 0.5F);

        // Micro membrane jitter.
        float jitter = 0.05F * Mth.sin(11.0F * angle + t * 0.19F + phase);

        float radius = 0.62F + body + lobe + jitter;
        // Keep inside the block after warpScale 0.5 (local radius ≤ ~0.95).
        return Mth.clamp(radius, 0.38F, 0.95F);
    }

    /**
     * Virus silhouette: compact core + sharp spikes ("cachitos") that extend and retract.
     */
    private static float virusRadius(float angle, WarpRenderState state) {
        float t = state.ageInTicks;
        float phase = (float) (state.x * 4.7 + state.y * 11.3 + state.z * 2.9);
        float strength = Mth.clamp(state.distortion, 1, 100) / 100.0F;

        // Core body — slightly bumpy, mostly round.
        float core = 0.48F
                + 0.04F * Mth.sin(4.0F * angle + t * 0.03F + phase)
                + 0.03F * Mth.sin(6.0F * angle - t * 0.05F + phase * 1.4F);

        // ~10 spikes around the rim; each pumps in/out on its own phase.
        final int spikes = 10;
        float spikeSum = 0.0F;
        for (int s = 0; s < spikes; s++) {
            float spikeAngle = (s / (float) spikes) * Mth.TWO_PI + phase * 0.15F;
            float delta = Mth.wrapDegrees((angle - spikeAngle) * (180.0F / Mth.PI)) * (Mth.PI / 180.0F);
            // Pointed lobe: high power of cos near the spike axis.
            float width = 0.55F;
            float falloff = Mth.clamp(1.0F - Math.abs(delta) / width, 0.0F, 1.0F);
            float pointed = falloff * falloff * falloff;

            // Each cachito extends/retracts independently.
            float pump = 0.5F + 0.5F * Mth.sin(t * (0.055F + s * 0.011F) + phase + s * 1.7F);
            // Staggered double-beat so they don't all move together.
            float pump2 = 0.5F + 0.5F * Mth.sin(t * (0.09F + s * 0.007F) - phase * 0.8F + s * 0.9F);
            float extension = 0.35F + 0.65F * pump * pump2;
            extension *= 0.75F + 0.25F * strength;

            spikeSum += pointed * extension * 0.42F;
        }

        float radius = core + spikeSum;
        return Mth.clamp(radius, 0.40F, 0.95F);
    }

    private static float softEdgeAlpha(float ringT) {
        return Mth.clamp(1.0F - Mth.square(Math.max(0.0F, ringT - 0.55F) / 0.45F), 0.0F, 1.0F);
    }

    private static void warpVertex(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            WarpRenderState state,
            float lx,
            float ly,
            float r,
            float g,
            float b,
            float a,
            float uvBias,
            float strength,
            float spinSign,
            float localEdge
    ) {
        float radius = Mth.sqrt(lx * lx + ly * ly);
        float nx = radius > 1.0E-4F ? lx / radius : 0.0F;
        float ny = radius > 1.0E-4F ? ly / radius : 0.0F;
        float edge = Math.max(localEdge, 1.0E-3F);
        float normalized = Mth.clamp(radius / edge, 0.0F, 1.0F);

        float sampleNx = nx;
        float sampleNy = ny;
        float sampleRadius;

        if (state.subtype == WarpSubtype.AMOEBA || state.subtype == WarpSubtype.VIRUS) {
            float t = state.ageInTicks;
            float phase = (float) (state.x * 5.1 + state.z * 9.3);
            float drift = (state.subtype == WarpSubtype.VIRUS ? 0.06F : 0.12F) * strength;
            float driftAngle = t * 0.04F + phase;
            float cu = state.centerU + Mth.cos(driftAngle) * drift * state.radiusUv * 0.35F;
            float cv = state.centerV + Mth.sin(driftAngle * 1.3F) * drift * state.radiusUv * 0.35F;

            float spin;
            float twist;
            if (state.subtype == WarpSubtype.VIRUS) {
                // Stronger radial pull along spikes; slow body rotation.
                spin = t * (0.02F + strength * 0.05F) * spinSign;
                twist = strength * 3.6F * (1.0F - normalized)
                        * (0.5F + 0.5F * Mth.sin(angleOf(nx, ny) * 10.0F + t * 0.08F + phase));
            } else {
                spin = t * (0.03F + strength * 0.08F) * spinSign
                        + 0.8F * Mth.sin(normalized * 3.0F + t * 0.05F);
                twist = strength * 2.8F * (1.0F - normalized)
                        * (0.6F + 0.4F * Mth.sin(angleOf(nx, ny) * 3.0F + t * 0.07F));
            }
            float angle = angleOf(nx, ny) + spin + twist;

            float pull = strength * (state.subtype == WarpSubtype.VIRUS ? 0.9F : 0.75F);
            float warped = normalized * (1.0F - pull * (1.0F - normalized * normalized));
            warped *= 1.0F - strength * 0.2F * (1.0F - normalized);

            sampleNx = Mth.cos(angle);
            sampleNy = Mth.sin(angle);
            sampleRadius = warped * state.radiusUv;

            float u = cu + sampleNx * sampleRadius + uvBias;
            float v = cv + sampleNy * sampleRadius + uvBias * 0.4F;
            emit(buffer, pose, lx, ly, u, v, r, g, b, a);
            return;
        }

        boolean swirling = state.subtype == WarpSubtype.VORTEX || state.subtype == WarpSubtype.BINARY;
        if (swirling) {
            float spin = state.ageInTicks * (0.08F + strength * 0.22F) * spinSign;
            float twist = strength * 4.5F * (1.0F - normalized) * spinSign;
            float angle = angleOf(nx, ny) + spin + twist;

            float pull = strength * (state.subtype == WarpSubtype.BINARY ? 0.95F : 0.88F);
            float warped = normalized * (1.0F - pull * (1.0F - normalized * normalized));
            warped *= 1.0F - strength * 0.35F * (1.0F - normalized);

            sampleNx = Mth.cos(angle);
            sampleNy = Mth.sin(angle);
            sampleRadius = warped * state.radiusUv;
        } else {
            float k = strength * 0.95F;
            float warped = normalized * (1.0F - k * (1.0F - normalized * normalized));
            sampleRadius = warped * state.radiusUv;
        }

        float u = state.centerU + sampleNx * sampleRadius + uvBias;
        float v = state.centerV + sampleNy * sampleRadius + uvBias * 0.4F;
        emit(buffer, pose, lx, ly, u, v, r, g, b, a);
    }

    private static float angleOf(float nx, float ny) {
        return (float) Mth.atan2(ny, nx);
    }

    private static void emit(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float lx,
            float ly,
            float u,
            float v,
            float r,
            float g,
            float b,
            float a
    ) {
        buffer.addVertex(pose, lx, ly, 0.0F)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }
}
