package dev.sdfg.mod.client;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.entity.GnomeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Gnome with Blockbench husk-based model + custom 64×64 skin.
 */
public class GnomeRenderer extends MobRenderer<GnomeEntity, LivingEntityRenderState, GnomeModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ExampleMod.MODID, "textures/entity/gnome.png");

    public GnomeRenderer(EntityRendererProvider.Context context) {
        super(context, new GnomeModel(context.bakeLayer(GnomeModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}
