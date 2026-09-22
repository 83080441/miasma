package dev.sdfg.mod.client;

import dev.sdfg.mod.ExampleMod;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

/**
 * Client-only full-screen color capture for space-distortion sampling.
 * Keeps the last successful frame if a copy fails mid-pass.
 */
public final class WarpSceneCapture extends AbstractTexture {
    public static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "textures/misc/warp_capture");

    private static final WarpSceneCapture INSTANCE = new WarpSceneCapture();
    private static boolean capturedThisFrame;

    private int captureWidth;
    private int captureHeight;
    private boolean registered;
    private boolean hasValidCapture;

    private WarpSceneCapture() {
    }

    public static WarpSceneCapture get() {
        return INSTANCE;
    }

    public static void beginFrame() {
        capturedThisFrame = false;
    }

    public void ensureRegistered() {
        if (!this.registered) {
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, this);
            this.registered = true;
        }
    }

    public static void captureThisFrame() {
        if (capturedThisFrame) {
            return;
        }
        capturedThisFrame = true;

        WarpSceneCapture capture = get();
        capture.ensureRegistered();
        RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if (main.getColorTexture() == null) {
            return;
        }

        try {
            capture.ensureSize(main.width, main.height);
            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .copyTextureToTexture(main.getColorTexture(), capture.getTexture(), 0, 0, 0, 0, 0, main.width, main.height);
            capture.hasValidCapture = true;
        } catch (Exception ignored) {
            // Keep previous frame if we already had a successful capture.
        }
    }

    public boolean hasCapture() {
        return this.hasValidCapture && this.texture != null && this.captureWidth > 0 && this.captureHeight > 0;
    }

    private void ensureSize(int width, int height) {
        if (this.texture != null && this.captureWidth == width && this.captureHeight == height) {
            return;
        }

        this.releaseTextures();
        this.hasValidCapture = false;
        int usage = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_SRC;
        this.texture = RenderSystem.getDevice().createTexture(() -> "sdfg warp capture", usage, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
        this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
        this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        this.captureWidth = width;
        this.captureHeight = height;
    }
}
