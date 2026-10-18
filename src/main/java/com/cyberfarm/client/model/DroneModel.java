package com.cyberfarm.client.model;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.renderer.DroneRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Quadcopter drone: flat hull with a glowing core, four diagonal arms with spinning rotors,
 * landing skids and an optional harvester head under the hull. Texture layout: 64x64.
 */
public class DroneModel extends EntityModel<DroneRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(CyberFarm.id("drone"), "main");
    private static final int ARMS = 4;

    private final ModelPart body;
    private final ModelPart harvester;
    private final ModelPart[] rotors = new ModelPart[ARMS];

    public DroneModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.body = root.getChild("body");
        this.harvester = body.getChild("harvester");
        for (int i = 0; i < ARMS; i++) {
            rotors[i] = body.getChild("arm" + i).getChild("motor").getChild("rotor");
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -2.0F, -4.0F, 8.0F, 3.0F, 8.0F)     // hull
                        .texOffs(0, 11).addBox(-2.5F, -4.0F, -2.5F, 5.0F, 2.0F, 5.0F)    // core dome
                        .texOffs(20, 11).addBox(-1.5F, -1.0F, -5.0F, 3.0F, 2.0F, 1.0F),  // front sensor
                PartPose.offset(0.0F, 18.0F, 0.0F));

        for (int i = 0; i < ARMS; i++) {
            float angle = (float) Math.toRadians(45 + 90 * i);
            PartDefinition arm = body.addOrReplaceChild("arm" + i, CubeListBuilder.create()
                            .texOffs(0, 18).addBox(3.0F, -0.5F, -1.0F, 4.0F, 1.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, angle, 0.0F));
            PartDefinition motor = arm.addOrReplaceChild("motor", CubeListBuilder.create()
                            .texOffs(16, 18).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                    PartPose.offset(7.0F, 0.0F, 0.0F));
            motor.addOrReplaceChild("rotor", CubeListBuilder.create()
                            .texOffs(0, 24).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 0.0F, 1.0F)
                            .texOffs(0, 26).addBox(-0.5F, 0.0F, -3.0F, 1.0F, 0.0F, 6.0F),
                    PartPose.offset(0.0F, -2.2F, 0.0F));
        }

        body.addOrReplaceChild("skids", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-3.5F, 1.0F, -3.0F, 1.0F, 2.0F, 6.0F)
                        .texOffs(14, 32).addBox(2.5F, 1.0F, -3.0F, 1.0F, 2.0F, 6.0F),
                PartPose.ZERO);

        body.addOrReplaceChild("harvester", CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-2.0F, 1.0F, -2.0F, 4.0F, 2.0F, 4.0F)
                        .texOffs(32, 6).addBox(-3.0F, 3.0F, -0.5F, 6.0F, 1.0F, 1.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(DroneRenderState state) {
        super.setupAnim(state);
        float spin = state.ageInTicks * 1.6F;
        for (int i = 0; i < ARMS; i++) {
            // Opposite corners spin the same way, like a real quadcopter
            rotors[i].yRot = i % 2 == 0 ? spin : -spin;
        }
        body.y += Mth.sin(state.ageInTicks * 0.15F) * 0.6F;
        body.zRot = Mth.sin(state.ageInTicks * 0.09F) * 0.03F;
        harvester.visible = state.hasHarvester;
    }
}
