package com.cyberfarm.client.model;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.renderer.DroneRenderState;
import com.cyberfarm.drone.DroneTask;
import com.cyberfarm.entity.DroneEntity;
import com.mojang.blaze3d.vertex.PoseStack;

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
 * Quadcopter drone: flat hull with a glowing core, four diagonal arms with spinning rotors and
 * landing skids. One tool per installed module: harvester head (centre, below), drill (front),
 * cargo claw (back), seed tank (top), saw (right side) and taxi hook (hanging). Texture layout: 64x64.
 */
public class DroneModel extends EntityModel<DroneRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(CyberFarm.id("drone"), "main");
    private static final int ARMS = 4;

    private final ModelPart body;
    private final ModelPart harvester;
    private final ModelPart drill;
    private final ModelPart claw;
    private final ModelPart seedTank;
    private final ModelPart saw;
    private final ModelPart hook;
    private final ModelPart shears;
    private final ModelPart sprayer;
    private final ModelPart[] arms = new ModelPart[ARMS];
    private final ModelPart[] rotors = new ModelPart[ARMS];
    /** Folded for the deploy, each arm swings in by this much, so the four lie along the hull. */
    private static final float FOLD = (float) Math.toRadians(40.0);
    /** Rotor turn per tick at full speed. */
    private static final float ROTOR_SPEED = 1.6F;

    public DroneModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.body = root.getChild("body");
        this.harvester = body.getChild("harvester");
        this.drill = body.getChild("drill");
        this.claw = body.getChild("claw");
        this.seedTank = body.getChild("seed_tank");
        this.saw = body.getChild("saw");
        this.hook = body.getChild("hook");
        this.shears = body.getChild("shears");
        this.sprayer = body.getChild("sprayer");
        for (int i = 0; i < ARMS; i++) {
            arms[i] = body.getChild("arm" + i);
            rotors[i] = arms[i].getChild("motor").getChild("rotor");
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

        // Pivot on its own axis so it can spin
        body.addOrReplaceChild("drill", CubeListBuilder.create()
                        .texOffs(48, 0).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 1.0F, 2.0F)
                        .texOffs(56, 0).addBox(-0.5F, 2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, -3.0F));

        body.addOrReplaceChild("claw", CubeListBuilder.create()
                        .texOffs(32, 10).addBox(-2.0F, 1.0F, -1.0F, 4.0F, 1.0F, 2.0F)
                        .texOffs(44, 10).addBox(-2.0F, 2.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .texOffs(44, 10).addBox(1.0F, 2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 3.0F));

        // Planter: green seed tank on top, behind the core
        body.addOrReplaceChild("seed_tank", CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-1.5F, -4.0F, 2.5F, 3.0F, 2.0F, 1.0F),
                PartPose.ZERO);

        // Logger: circular saw on the right side, spinning around its own axle
        body.addOrReplaceChild("saw", CubeListBuilder.create()
                        .texOffs(48, 10).addBox(0.0F, -2.5F, -2.5F, 0.0F, 5.0F, 5.0F),
                PartPose.offset(4.6F, 0.5F, 0.0F));

        // Taxi: a cable with a hook the passenger holds on to
        body.addOrReplaceChild("hook", CubeListBuilder.create()
                        .texOffs(8, 40).addBox(-0.5F, 1.0F, -0.5F, 1.0F, 11.0F, 1.0F)
                        .texOffs(12, 40).addBox(-1.5F, 12.0F, -0.5F, 3.0F, 1.0F, 1.0F)
                        .texOffs(20, 40).addBox(0.5F, 11.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.ZERO);

        // Rancher: a pair of shear blades on the left side, opposite the saw
        body.addOrReplaceChild("shears", CubeListBuilder.create()
                        .texOffs(32, 40).addBox(-0.5F, -1.5F, -2.0F, 1.0F, 1.0F, 4.0F)
                        .texOffs(32, 40).addBox(-0.5F, 0.5F, -2.0F, 1.0F, 1.0F, 4.0F),
                PartPose.offset(-4.6F, 0.5F, 0.0F));

        // Fertilizer: a white canister on the left shoulder
        body.addOrReplaceChild("sprayer", CubeListBuilder.create()
                        .texOffs(44, 40).addBox(-4.5F, -5.0F, 0.0F, 2.0F, 3.0F, 2.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Arms opening during the deploy and closing when picked up, eased in and out. */
    private static float unfold(DroneRenderState state) {
        float t;
        if (state.shutdown >= 0) {
            t = 1.0F - Mth.clamp((state.shutdown - DroneEntity.FOLD_START) / DroneEntity.FOLD_TICKS, 0.0F, 1.0F);
        } else if (state.startup >= 0) {
            t = Mth.clamp(state.startup / DroneEntity.UNFOLD_TICKS, 0.0F, 1.0F);
        } else {
            return 1.0F;
        }
        return t * t * (3.0F - 2.0F * t);
    }

    /**
     * The rotors' angle: still before the deploy spin-up, then turning faster and faster at a steady
     * acceleration (the angle grows with the square of the time, so it never jumps) up to full speed.
     */
    private static float rotorAngle(DroneRenderState state) {
        if (state.shutdown >= 0) {
            // The mirror image: from full speed down to a stop at a steady deceleration
            float stopping = Math.min(state.shutdown, DroneEntity.SPIN_DOWN_TICKS);
            float touchdown = state.ageInTicks - state.shutdown;
            return ROTOR_SPEED * (touchdown + stopping - stopping * stopping / (2 * DroneEntity.SPIN_DOWN_TICKS));
        }
        if (state.startup < 0) {
            return state.ageInTicks * ROTOR_SPEED;
        }
        float spinning = Math.max(0.0F, state.startup - DroneEntity.SPIN_START);
        return spinning < DroneEntity.SPIN_TICKS
                ? ROTOR_SPEED * spinning * spinning / (2 * DroneEntity.SPIN_TICKS)
                : ROTOR_SPEED * (spinning - DroneEntity.SPIN_TICKS / 2.0F);
    }

    /** Moves the pose to the Transport claw, following the body's hover and sway. */
    public void translateToClaw(PoseStack pose) {
        body.translateAndRotate(pose);
        claw.translateAndRotate(pose);
    }

    @Override
    public void setupAnim(DroneRenderState state) {
        super.setupAnim(state);
        float spin = rotorAngle(state);
        float folded = 1.0F - unfold(state);
        for (int i = 0; i < ARMS; i++) {
            // Opposite corners spin the same way, like a real quadcopter
            rotors[i].yRot = i % 2 == 0 ? spin : -spin;
            arms[i].yRot += (i % 2 == 0 ? FOLD : -FOLD) * folded;
        }
        // Resting on the pad or the ground it does not bob: only once it lifts off, and until it lands
        float lift = state.shutdown >= 0 ? 0.0F
                : state.startup < 0 ? 1.0F : Mth.clamp((state.startup - DroneEntity.LIFT_START) / 10.0F, 0.0F, 1.0F);
        body.y += Mth.sin(state.ageInTicks * 0.15F) * 0.6F * lift;
        body.zRot = Mth.sin(state.ageInTicks * 0.09F) * 0.03F;
        harvester.visible = state.has(DroneTask.HARVEST);
        drill.visible = state.has(DroneTask.QUARRY);
        drill.yRot = state.ageInTicks * 0.8F;
        // The Builder carries its blocks in the same claw
        claw.visible = state.has(DroneTask.TRANSPORT) || state.has(DroneTask.BUILD);
        // The Breeder keeps its food in the Planter's tank, the Fisher casts with the Taxi's hook
        seedTank.visible = state.has(DroneTask.PLANT) || state.has(DroneTask.BREED);
        saw.visible = state.has(DroneTask.LOG);
        saw.xRot = state.ageInTicks * 1.2F;
        hook.visible = state.has(DroneTask.TAXI) || state.has(DroneTask.FISH);
        shears.visible = state.has(DroneTask.RANCH);
        // The blades snip open and shut
        shears.yRot = Mth.sin(state.ageInTicks * 0.5F) * 0.15F;
        sprayer.visible = state.has(DroneTask.FERTILIZE);
    }
}
