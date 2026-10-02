package com.nutriamod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nutriamod.NutriaMod;
import com.nutriamod.entity.OtterEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class OtterModel<T extends OtterEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(NutriaMod.MODID, "otter"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;

    public OtterModel(ModelPart root) {
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.tail = this.body.getChild("tail");
        this.rightFrontLeg = this.body.getChild("right_front_leg");
        this.leftFrontLeg = this.body.getChild("left_front_leg");
        this.rightHindLeg = this.body.getChild("right_hind_leg");
        this.leftHindLeg = this.body.getChild("left_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -4.0F, -5.0F, 5.0F, 4.0F, 10.0F),
                PartPose.offset(0.0F, 21.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-2.5F, -2.0F, -4.0F, 5.0F, 4.0F, 4.0F)
                        .texOffs(50, 0).addBox(-2.5F, -3.0F, -2.0F, 2.0F, 2.0F, 1.0F)
                        .texOffs(50, 0).addBox(0.5F, -3.0F, -2.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, -2.5F, -5.0F));

        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(32, 10).addBox(-1.5F, -0.5F, -2.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -4.0F));

        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(10, 16).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 9.0F),
                PartPose.offset(0.0F, -2.0F, 5.0F));

        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F);
        body.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-1.75F, 0.0F, -3.5F));
        body.addOrReplaceChild("left_front_leg", leg, PartPose.offset(1.75F, 0.0F, -3.5F));
        body.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-1.75F, 0.0F, 3.5F));
        body.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(1.75F, 0.0F, 3.5F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float yaw = netHeadYaw * ((float) Math.PI / 180.0F);
        float pitch = headPitch * ((float) Math.PI / 180.0F);
        boolean swimming = entity.isInWater();
        boolean sitting = entity.isInSittingPose() && !swimming;

        // Reiniciar pose
        body.xRot = 0.0F;
        body.yRot = 0.0F;
        body.zRot = 0.0F;
        body.y = 21.0F;
        head.xRot = pitch;
        head.yRot = yaw;
        tail.xRot = -0.15F;
        tail.yRot = 0.0F;

        if (swimming) {
            body.xRot = pitch;
            head.xRot = 0.0F;
            float paddle = Mth.cos(ageInTicks * 0.45F) * 0.35F;
            rightFrontLeg.xRot = 0.9F + paddle;
            leftFrontLeg.xRot = 0.9F - paddle;
            rightHindLeg.xRot = 0.7F - paddle;
            leftHindLeg.xRot = 0.7F + paddle;
            tail.xRot = 0.0F;
            tail.yRot = Mth.cos(ageInTicks * 0.35F) * 0.5F;
            if (entity.isPlayingInWater()) {
                body.zRot = ageInTicks * 0.3F; // vueltas jugando
            }
        } else if (sitting) {
            body.xRot = -0.8F;
            body.y = 20.0F;
            head.xRot = pitch + 0.8F;
            rightFrontLeg.xRot = 0.8F;
            leftFrontLeg.xRot = 0.8F;
            rightHindLeg.xRot = -1.2F;
            leftHindLeg.xRot = -1.2F;
            tail.xRot = 0.65F;
            tail.yRot = Mth.cos(ageInTicks * 0.15F) * 0.2F;
        } else {
            float swing = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
            rightFrontLeg.xRot = swing;
            leftFrontLeg.xRot = -swing;
            rightHindLeg.xRot = -swing;
            leftHindLeg.xRot = swing;
            tail.yRot = Mth.cos(ageInTicks * 0.15F) * 0.15F + Mth.cos(limbSwing * 0.6662F) * 0.3F * limbSwingAmount;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        body.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
