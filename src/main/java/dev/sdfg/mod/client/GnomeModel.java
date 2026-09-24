package dev.sdfg.mod.client;

import dev.sdfg.mod.ExampleMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Blockbench husk-based gnome mesh (64×64). See {@code plan/GNOME_README.md}.
 */
public class GnomeModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(ExampleMod.MODID, "gnome"), "main");

    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public GnomeModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition root = meshdefinition.getRoot();

        root.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(0, 1).addBox(-4.0F, 9.0F, -4.0F, 8.0F, 7.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 16.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, 14.0F, -2.0F, 2.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offset(5.0F, 2.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(42, 16).addBox(-1.0F, 14.0F, -2.0F, 2.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.9F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offset(1.9F, 12.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.1F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F)
        );

        // Pointed hood / hat (bb_main in Blockbench export).
        root.addOrReplaceChild(
                "bb_main",
                CubeListBuilder.create()
                        .texOffs(-9, -6).addBox(-4.0F, -17.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(-7, -5).addBox(-3.0F, -19.0F, -3.0F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F))
                        .texOffs(-3, -2).addBox(-2.0F, -21.0F, -1.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(-1, -1).addBox(-1.0F, -23.0F, -1.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(-3, -2).addBox(-2.0F, -21.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);

        this.head.yRot = state.yRot * ((float) Math.PI / 180.0F);
        this.head.xRot = state.xRot * ((float) Math.PI / 180.0F);

        float pos = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        this.rightLeg.xRot = Mth.cos(pos * 0.6662F) * 1.4F * speed;
        this.leftLeg.xRot = Mth.cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
        this.rightArm.xRot = Mth.cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
        this.leftArm.xRot = Mth.cos(pos * 0.6662F) * 1.4F * speed;
    }
}
