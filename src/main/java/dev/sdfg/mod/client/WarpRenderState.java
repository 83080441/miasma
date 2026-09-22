package dev.sdfg.mod.client;

import dev.sdfg.mod.entity.WarpSubtype;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class WarpRenderState extends EntityRenderState {
    public float centerU = 0.5F;
    public float centerV = 0.5F;
    public float radiusUv = 0.12F;
    public float warpScale = 0.5F;
    /** Distortion strength 1–100. */
    public int distortion = 50;
    public WarpSubtype subtype = WarpSubtype.STATIC;

    /** World-space offset from entity center for the active node (binary). */
    public float nodeOffsetX;
    public float nodeOffsetY;
    public float nodeOffsetZ;
    /** Screen UV of the active node center when drawing. */
    public float nodeCenterU = 0.5F;
    public float nodeCenterV = 0.5F;
}
