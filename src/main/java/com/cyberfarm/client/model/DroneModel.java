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
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Crop quadcopter: dark chassis with a glowing visor under a low white tank with a mint band, a slim
 * antenna with a glowing tip, four long diagonal arms with copper motors, spray nozzles and open
 * rotors, and landing gear. One tool per installed module: harvester head (centre, below), drill (front),
 * cargo claw (back), seed tank (top), saw (right side), taxi hook (hanging), laser gun (right
 * shoulder) and bomb (centre, below). Parts hanging under the skids tuck up while the drone is
 * parked. Texture layout: 128x64, the tools from texgen's drone_tools() in the right half.
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
    private final ModelPart laser;
    private final ModelPart bomb;
    private final ModelPart harvesterRail;
    private final ModelPart harvesterBlades;
    private final ModelPart jawLeft;
    private final ModelPart jawRight;
    private final ModelPart builderLevel;
    private final ModelPart breederTrough;
    private final ModelPart breederRack;
    private final ModelPart pump;
    private final ModelPart sawBracket;
    private final ModelPart hookSwing;
    private final ModelPart hookLine;
    private final ModelPart hookTip;
    private final ModelPart fisherFloat;
    private final ModelPart bladeLeft;
    private final ModelPart bladeRight;
    private final ModelPart bombVane;
    private final ModelPart[] arms = new ModelPart[ARMS];
    /** The tools under the hull, from front to back when they share it. */
    private final ModelPart[] underHull;
    /** How far each of those reaches forward and back from its middle, in pixels. */
    private static final float[] UNDER_HULL_DEPTH = {1.0F, 2.0F, 3.0F, 2.5F, 0.5F, 1.5F, 1.0F, 1.0F};
    /** How far the hook hangs below its swing pivot when the cable is paid out. */
    private static final float HOOK_REACH = 10.0F;
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
        this.laser = body.getChild("laser");
        this.bomb = body.getChild("bomb");
        this.harvesterRail = harvester.getChild("rail");
        this.harvesterBlades = harvesterRail.getChild("blades");
        this.jawLeft = claw.getChild("jaw_left");
        this.jawRight = claw.getChild("jaw_right");
        this.builderLevel = claw.getChild("level");
        this.breederTrough = body.getChild("trough");
        this.breederRack = breederTrough.getChild("rack");
        this.pump = body.getChild("pump");
        this.sawBracket = body.getChild("saw_bracket");
        this.hookSwing = hook.getChild("swing");
        this.hookLine = hookSwing.getChild("line");
        this.hookTip = hookSwing.getChild("tip");
        this.fisherFloat = hookSwing.getChild("float");
        this.bladeLeft = shears.getChild("blade_left");
        this.bladeRight = shears.getChild("blade_right");
        this.bombVane = bomb.getChild("vane");
        this.underHull = new ModelPart[] {drill, harvester, shears, bomb, pump, breederTrough, claw, hook};
        for (int i = 0; i < ARMS; i++) {
            arms[i] = body.getChild("arm" + i);
            rotors[i] = arms[i].getChild("motor").getChild("rotor");
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Crop drone: dark chassis with a glowing visor, a low white tank with a mint band, a slim mast with a glowing tip
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5F, -1.0F, -3.0F, 5.0F, 2.0F, 6.0F)     // chassis
                        .texOffs(54, 56).addBox(-2.0F, -0.5F, -3.5F, 4.0F, 1.0F, 1.0F)   // visor
                        .texOffs(0, 11).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 2.0F, 3.0F)    // low tank
                        .texOffs(28, 32).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.3F))  // copper band
                        .texOffs(54, 40).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))  // antenna mast
                        .texOffs(44, 46).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.1F)),  // antenna light
                PartPose.offset(0.0F, 18.0F, 0.0F));

        for (int i = 0; i < ARMS; i++) {
            float angle = (float) Math.toRadians(45 + 90 * i);
            PartDefinition arm = body.addOrReplaceChild("arm" + i, CubeListBuilder.create()
                            .texOffs(0, 18).addBox(2.5F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, angle, 0.0F));
            // Copper motor with a spray nozzle underneath, long open propeller on top
            PartDefinition motor = arm.addOrReplaceChild("motor", CubeListBuilder.create()
                            .texOffs(16, 18).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                            .texOffs(44, 56).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(6.5F, 0.0F, 0.0F));
            motor.addOrReplaceChild("rotor", CubeListBuilder.create()
                            .texOffs(0, 24).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 0.0F, 1.0F)
                            .texOffs(0, 26).addBox(-0.5F, 0.0F, -3.0F, 1.0F, 0.0F, 6.0F),
                    PartPose.offset(0.0F, -2.2F, 0.0F));
        }

        // Landing gear: two legs a side down to a long skid
        body.addOrReplaceChild("skids", CubeListBuilder.create()
                        .texOffs(44, 49).addBox(-2.5F, 1.0F, -2.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(44, 49).addBox(-2.5F, 1.0F, 1.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(44, 49).addBox(1.5F, 1.0F, -2.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(44, 49).addBox(1.5F, 1.0F, 1.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(0, 32).addBox(-2.5F, 2.0F, -3.5F, 1.0F, 1.0F, 7.0F)
                        .texOffs(0, 32).addBox(1.5F, 2.0F, -3.5F, 1.0F, 1.0F, 7.0F),
                PartPose.ZERO);

        // Harvester: a striped head between the legs, and a rail with two toothed blades sliding along it, like a mower.
        // The rail is wider than the gap between the skids, so to tuck up it telescopes into the head
        PartDefinition harvesterPart = body.addOrReplaceChild("harvester", CubeListBuilder.create()
                        .texOffs(64, 0).addBox(-1.5F, 1.0F, -2.0F, 3.0F, 2.0F, 4.0F),
                PartPose.ZERO);
        PartDefinition railPart = harvesterPart.addOrReplaceChild("rail", CubeListBuilder.create()
                        .texOffs(80, 0).addBox(-3.0F, 3.0F, -0.5F, 6.0F, 1.0F, 1.0F),
                PartPose.ZERO);
        railPart.addOrReplaceChild("blades", CubeListBuilder.create()
                        .texOffs(94, 0).addBox(-2.5F, 3.0F, -1.5F, 2.0F, 1.0F, 1.0F)
                        .texOffs(94, 0).addBox(0.5F, 3.0F, -1.5F, 2.0F, 1.0F, 1.0F),
                PartPose.ZERO);

        // Under the middle of the hull, spinning on its own axis
        body.addOrReplaceChild("drill", CubeListBuilder.create()
                        .texOffs(48, 0).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 1.0F, 2.0F)
                        .texOffs(56, 0).addBox(-0.5F, 2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.ZERO);

        // Transport: a housing between the skids and two jaws below them, free to open, with toes turned inward;
        // the Builder adds a level on the front
        PartDefinition clawPart = body.addOrReplaceChild("claw", CubeListBuilder.create()
                        .texOffs(100, 0).addBox(-1.5F, 1.0F, -1.0F, 3.0F, 2.0F, 2.0F),
                PartPose.ZERO);
        clawPart.addOrReplaceChild("jaw_left", CubeListBuilder.create()
                        .texOffs(112, 0).addBox(-1.5F, 3.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .texOffs(116, 0).addBox(-0.5F, 4.0F, -0.5F, 0.5F, 1.0F, 1.0F),
                PartPose.ZERO);
        clawPart.addOrReplaceChild("jaw_right", CubeListBuilder.create()
                        .texOffs(112, 0).addBox(0.5F, 3.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .texOffs(116, 0).addBox(0.0F, 4.0F, -0.5F, 0.5F, 1.0F, 1.0F),
                PartPose.ZERO);
        clawPart.addOrReplaceChild("level", CubeListBuilder.create()
                        .texOffs(98, 17).addBox(-1.5F, 1.0F, -2.0F, 3.0F, 1.0F, 1.0F),
                PartPose.ZERO);

        // Planter: green seed tank on top, behind the core and clear of its mint band
        body.addOrReplaceChild("seed_tank", CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-1.5F, -3.0F, 1.85F, 3.0F, 2.0F, 1.0F),
                PartPose.ZERO);
        // Planter and Fertilizer: a pump under the hull, a stem and a spray bar with nozzles, between the skids
        body.addOrReplaceChild("pump", CubeListBuilder.create()
                        .texOffs(106, 17).addBox(-0.5F, 1.0F, -0.5F, 1.0F, 1.0F, 1.0F)
                        .texOffs(84, 24).addBox(-1.5F, 2.0F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.ZERO);
        // Breeder: a slatted hay rack hung below the skids on two struts
        PartDefinition troughPart = body.addOrReplaceChild("trough", CubeListBuilder.create()
                        .texOffs(76, 24).addBox(-1.3F, 1.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .texOffs(76, 24).addBox(0.3F, 1.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.ZERO);
        troughPart.addOrReplaceChild("rack", CubeListBuilder.create()
                        .texOffs(64, 28).addBox(-2.5F, 3.0F, -1.5F, 5.0F, 2.0F, 3.0F),
                PartPose.ZERO);

        // Logger: a toothed disc on the right side, spinning on its hub, on a bracket from the hull
        body.addOrReplaceChild("saw", CubeListBuilder.create()
                        .texOffs(64, 6).addBox(0.0F, -2.5F, -2.5F, 0.0F, 5.0F, 5.0F)
                        .texOffs(74, 6).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(4.1F, 0.5F, 0.0F));
        body.addOrReplaceChild("saw_bracket", CubeListBuilder.create()
                        .texOffs(78, 6).addBox(2.5F, 0.0F, -0.5F, 2.0F, 1.0F, 1.0F),
                PartPose.ZERO);

        // Taxi: a winch, and a braided cable swinging with the hook the passenger holds on to; the Fisher adds a float
        PartDefinition hookPart = body.addOrReplaceChild("hook", CubeListBuilder.create()
                        .texOffs(84, 6).addBox(-1.5F, 1.0F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.ZERO);
        PartDefinition swing = hookPart.addOrReplaceChild("swing", CubeListBuilder.create(), PartPose.offset(0.0F, 2.0F, 0.0F));
        swing.addOrReplaceChild("line", CubeListBuilder.create()
                        .texOffs(94, 6).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.ZERO);
        swing.addOrReplaceChild("tip", CubeListBuilder.create()
                        .texOffs(98, 6).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 1.0F, 1.0F)
                        .texOffs(106, 6).addBox(0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, HOOK_REACH, 0.0F));
        swing.addOrReplaceChild("float", CubeListBuilder.create()
                        .texOffs(112, 17).addBox(-1.0F, 3.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.ZERO);

        // Rancher: shears under the hull, on a bolt behind the blades, opening in a V below the skids
        PartDefinition shearsPart = body.addOrReplaceChild("shears", CubeListBuilder.create()
                        .texOffs(124, 6).addBox(-0.5F, 1.0F, 1.0F, 1.0F, 3.0F, 1.0F),
                PartPose.ZERO);
        shearsPart.addOrReplaceChild("blade_left", CubeListBuilder.create()
                        .texOffs(110, 6).addBox(-1.0F, 0.0F, -4.5F, 1.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 3.0F, 1.5F));
        shearsPart.addOrReplaceChild("blade_right", CubeListBuilder.create()
                        .texOffs(110, 6).addBox(0.0F, 0.0F, -4.5F, 1.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 3.0F, 1.5F));

        // Fertilizer: a white canister on the left shoulder, clear of the core's mint band
        body.addOrReplaceChild("sprayer", CubeListBuilder.create()
                        .texOffs(44, 40).addBox(-3.85F, -4.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.ZERO);

        // Guard: a laser gun on the right shoulder, its barrel pointing ahead, a red lens at the tip
        body.addOrReplaceChild("laser", CubeListBuilder.create()
                        .texOffs(0, 52).addBox(1.5F, -3.0F, -1.5F, 2.0F, 2.0F, 2.0F)
                        .texOffs(8, 52).addBox(2.0F, -2.5F, -5.5F, 1.0F, 1.0F, 4.0F)
                        .texOffs(20, 52).addBox(2.0F, -2.5F, -6.5F, 1.0F, 1.0F, 1.0F),
                PartPose.ZERO);

        // Bomb: a banded charge slung under the hull, a red detonator at the bottom, an arming vane on its nose
        PartDefinition bombPart = body.addOrReplaceChild("bomb", CubeListBuilder.create()
                        .texOffs(68, 17).addBox(-1.5F, 1.0F, -1.5F, 3.0F, 3.0F, 3.0F)
                        .texOffs(80, 17).addBox(-0.5F, 4.0F, -0.5F, 1.0F, 1.0F, 1.0F)
                        .texOffs(84, 17).addBox(-0.5F, 2.0F, -2.5F, 1.0F, 1.0F, 1.0F),
                PartPose.ZERO);
        bombPart.addOrReplaceChild("vane", CubeListBuilder.create()
                        .texOffs(88, 17).addBox(-1.5F, -0.5F, -0.01F, 3.0F, 1.0F, 0.0F)
                        .texOffs(96, 17).addBox(-0.5F, -1.5F, 0.0F, 1.0F, 3.0F, 0.0F),
                PartPose.offset(0.0F, 2.5F, -2.5F));

        return LayerDefinition.create(mesh, 128, 64);
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
        // Parked, what hangs under the skids tucks up, so it stays out of the pad, and stops moving
        float out = 1.0F - state.stow;
        harvester.visible = state.has(DroneTask.HARVEST);
        harvesterRail.xScale = 1.0F - 0.5F * state.stow;
        harvesterRail.y -= 1.05F * state.stow;
        harvesterBlades.x = Mth.sin(state.ageInTicks * 0.7F) * 1.5F * out;
        drill.visible = state.has(DroneTask.QUARRY);
        drill.yRot = state.ageInTicks * 0.8F;
        // The Builder carries its blocks in the same claw
        claw.visible = state.has(DroneTask.TRANSPORT) || state.has(DroneTask.BUILD);
        claw.y -= 2.0F * state.stow;
        builderLevel.visible = state.has(DroneTask.BUILD);
        // The jaws shut on a load, and work slowly while they wait for one
        float gap = state.carried.isEmpty() ? (0.75F + 0.75F * Mth.sin(state.ageInTicks * 0.16F)) * out : 0.0F;
        jawLeft.x = -gap;
        jawRight.x = gap;
        // The Breeder keeps its food in the Planter's tank, the Fisher casts with the Taxi's hook
        seedTank.visible = state.has(DroneTask.PLANT) || state.has(DroneTask.BREED);
        breederTrough.visible = state.has(DroneTask.BREED);
        // Parked, the rack narrows and rises to fit between the skids
        breederRack.xScale = 1.0F - 0.4F * state.stow;
        breederRack.y -= 2.0F * state.stow;
        pump.visible = state.has(DroneTask.PLANT) || state.has(DroneTask.FERTILIZE);
        saw.visible = state.has(DroneTask.LOG);
        saw.xRot = state.ageInTicks * 1.2F;
        sawBracket.visible = saw.visible;
        hook.visible = state.has(DroneTask.TAXI) || state.has(DroneTask.FISH);
        hookSwing.zRot = Mth.sin(state.ageInTicks * 0.13F) * 0.22F * out;
        // The winch reels the cable in until the hook sits right under it, still in sight between the skids
        float reach = HOOK_REACH * out;
        hookTip.y = reach;
        hookLine.visible = reach > 0.0F;
        hookLine.yScale = Math.max(reach, 0.01F) / HOOK_REACH;
        fisherFloat.visible = state.has(DroneTask.FISH) && state.stow < 0.5F;
        shears.visible = state.has(DroneTask.RANCH);
        shears.y -= state.stow;
        // The blades open and shut in a V, and close to fit between the skids when parked
        float snip = (0.22F + 0.22F * Mth.sin(state.ageInTicks * 0.35F)) * out;
        bladeLeft.yRot = snip;
        bladeRight.yRot = -snip;
        sprayer.visible = state.has(DroneTask.FERTILIZE);
        laser.visible = state.has(DroneTask.GUARD);
        bomb.visible = state.has(DroneTask.BOMB);
        bomb.y -= 2.0F * state.stow;
        bombVane.zRot = out > 0.0F ? state.ageInTicks * 0.6F : 0.0F;
        shareUnderHull();
    }

    /**
     * Alone, a tool hangs in the middle of the hull. With several, they share its length from front to
     * back, and those too deep for their share shorten to fit.
     */
    private void shareUnderHull() {
        int count = 0;
        for (ModelPart tool : underHull) {
            if (tool.visible) {
                count++;
            }
        }
        if (count < 2) {
            return;
        }
        float pitch = count == 2 ? 3.0F : 6.6F / (count - 1);
        float z = -pitch * (count - 1) / 2.0F;
        for (int i = 0; i < underHull.length; i++) {
            if (underHull[i].visible) {
                underHull[i].z += z;
                underHull[i].zScale = Math.min(1.0F, pitch / 2.0F / UNDER_HULL_DEPTH[i]);
                z += pitch;
            }
        }
    }
}
