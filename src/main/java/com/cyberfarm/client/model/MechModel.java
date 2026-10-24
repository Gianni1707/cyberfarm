package com.cyberfarm.client.model;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.renderer.MechRenderState;

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
 * The mech: a white and blue humanoid about 3.6 blocks tall, with big shoulder pads, a V-finned
 * head and a dark back unit. Its trunk is a bay the pilot stands in, back against a harness, eyes
 * at the head's visor. The front of the trunk is four petals, two over two, hinged on their outer
 * edges, that open out one after the other; the visor plate on the head lifts with them. Model
 * units are 1/16 block, feet at y = 24. Texture layout: 128x128.
 */
public class MechModel extends EntityModel<MechRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(CyberFarm.id("mech"), "main");
    /** How far the petals swing out, and tilt away from the middle, in radians. */
    private static final float PETAL_SWING = 1.75F;
    private static final float PETAL_TILT = 0.45F;
    /** How far the body sinks when the mech crouches to take its pilot, in model units. */
    private static final float CROUCH_DROP = 14.5F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart visor;
    private final ModelPart bay;
    private final ModelPart[] upperPetals;
    private final ModelPart[] lowerPetals;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftForearm;
    private final ModelPart rightForearm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftShin;
    private final ModelPart rightShin;
    private final ModelPart leftFoot;
    private final ModelPart rightFoot;
    // Module parts: the jet's wings and nozzles, the drill, the blade, the armour plates
    private final ModelPart[] wings;
    private final ModelPart[] wingMids;
    private final ModelPart[] wingTips;
    private final ModelPart[] nozzles;
    private final ModelPart drill;
    private final ModelPart blade;
    private final ModelPart[] plates;

    public MechModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.visor = head.getChild("visor");
        this.bay = body.getChild("bay");
        this.upperPetals = new ModelPart[] {body.getChild("upper_left_petal"), body.getChild("upper_right_petal")};
        this.lowerPetals = new ModelPart[] {body.getChild("lower_left_petal"), body.getChild("lower_right_petal")};
        this.leftArm = body.getChild("left_arm");
        this.rightArm = body.getChild("right_arm");
        this.leftForearm = leftArm.getChild("forearm");
        this.rightForearm = rightArm.getChild("forearm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.leftShin = leftLeg.getChild("shin");
        this.rightShin = rightLeg.getChild("shin");
        this.leftFoot = leftShin.getChild("foot");
        this.rightFoot = rightShin.getChild("foot");
        this.wings = new ModelPart[] {body.getChild("left_wing"), body.getChild("right_wing")};
        this.wingMids = new ModelPart[] {wings[0].getChild("mid"), wings[1].getChild("mid")};
        this.wingTips = new ModelPart[] {wingMids[0].getChild("tip"), wingMids[1].getChild("tip")};
        this.nozzles = new ModelPart[] {body.getChild("left_nozzle"), body.getChild("right_nozzle")};
        this.drill = rightForearm.getChild("drill");
        this.blade = leftForearm.getChild("blade");
        this.plates = new ModelPart[] {leftArm.getChild("plate"), rightArm.getChild("plate"), body.getChild("chest_plate"),
                leftLeg.getChild("plate"), rightLeg.getChild("plate")};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // The trunk, from the hips (y -4) to the shoulders' top (y -26): a shell open in front
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 92).addBox(-8.0F, -22.0F, 3.0F, 16.0F, 22.0F, 3.0F)          // back wall
                        .texOffs(38, 92).addBox(-8.0F, -22.0F, -4.0F, 2.0F, 22.0F, 7.0F)         // left wall
                        .texOffs(38, 92).mirror().addBox(6.0F, -22.0F, -4.0F, 2.0F, 22.0F, 7.0F) // right wall
                        .mirror(false)
                        .texOffs(56, 92).addBox(-6.0F, -2.0F, -4.0F, 12.0F, 2.0F, 7.0F)          // floor
                        .texOffs(18, 74).addBox(-6.0F, -22.0F, -4.0F, 12.0F, 1.0F, 7.0F)         // roof
                        .texOffs(34, 22).addBox(-6.0F, -21.5F, 6.0F, 12.0F, 10.0F, 4.0F),        // back unit
                PartPose.offset(0.0F, -4.0F, 0.0F));
        // The harness the pilot stands against
        body.addOrReplaceChild("bay", CubeListBuilder.create()
                        .texOffs(92, 102).addBox(-1.0F, -16.0F, 2.0F, 2.0F, 14.0F, 1.0F)         // spine
                        .texOffs(98, 102).addBox(-4.0F, -17.0F, 2.0F, 8.0F, 1.0F, 1.0F),         // shoulder bar
                PartPose.ZERO);

        // Petals: upper ones over the chest, lower ones over the belly, hinged on their outer edges
        body.addOrReplaceChild("upper_left_petal", CubeListBuilder.create()
                        .texOffs(56, 102).addBox(0.0F, 0.0F, -1.0F, 8.0F, 11.0F, 1.0F),
                PartPose.offset(-8.0F, -22.0F, -4.0F));
        body.addOrReplaceChild("upper_right_petal", CubeListBuilder.create()
                        .texOffs(56, 102).mirror().addBox(-8.0F, 0.0F, -1.0F, 8.0F, 11.0F, 1.0F),
                PartPose.offset(8.0F, -22.0F, -4.0F));
        body.addOrReplaceChild("lower_left_petal", CubeListBuilder.create()
                        .texOffs(74, 102).addBox(0.0F, -9.0F, -1.0F, 8.0F, 9.0F, 1.0F),
                PartPose.offset(-8.0F, -2.0F, -4.0F));
        body.addOrReplaceChild("lower_right_petal", CubeListBuilder.create()
                        .texOffs(74, 102).mirror().addBox(-8.0F, -9.0F, -1.0F, 8.0F, 9.0F, 1.0F),
                PartPose.offset(8.0F, -2.0F, -4.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(92, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -22.0F, 0.0F));
        // The visor plate, hinged on the head's brow, lifts to let the pilot's eyes through
        head.addOrReplaceChild("visor", CubeListBuilder.create()
                        .texOffs(98, 106).addBox(-4.0F, 0.0F, -1.0F, 8.0F, 5.0F, 1.0F),
                PartPose.offset(0.0F, -7.0F, -4.0F));
        // The V fin on the forehead
        head.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(124, 0).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(-0.8F, -7.0F, -4.0F, 0.0F, 0.0F, -0.45F));
        head.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(124, 0).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(0.8F, -7.0F, -4.0F, 0.0F, 0.0F, 0.45F));

        arm(body, "left_arm", -10.5F, false);
        arm(body, "right_arm", 10.5F, true);
        leg(root, "left_leg", -3.5F, false);
        leg(root, "right_leg", 3.5F, true);
        modules(body);
        return LayerDefinition.create(mesh, 128, 128);
    }

    /**
     * The modules' parts, stowed until they come out: three-part wings and two nozzles on the back
     * unit, the drill under the right hand, the blade along the left forearm, and armour plates over
     * the shoulders, the chest and the thighs.
     */
    private static void modules(PartDefinition body) {
        for (int side = 0; side < 2; side++) {
            float sign = side == 0 ? -1.0F : 1.0F;
            boolean mirror = side == 1;
            float x0 = side == 0 ? -8.0F : 0.0F;
            PartDefinition wing = body.addOrReplaceChild(side == 0 ? "left_wing" : "right_wing", CubeListBuilder.create().mirror(mirror)
                            .texOffs(18, 82).addBox(x0, -0.5F, -2.0F, 8.0F, 1.0F, 4.0F),
                    PartPose.offset(sign * 6.0F, -19.0F, 8.0F));
            PartDefinition mid = wing.addOrReplaceChild("mid", CubeListBuilder.create().mirror(mirror)
                            .texOffs(18, 87).addBox(x0, -0.5F, -1.5F, 8.0F, 1.0F, 3.0F),
                    PartPose.offset(sign * 8.0F, 0.0F, 0.5F));
            mid.addOrReplaceChild("tip", CubeListBuilder.create().mirror(mirror)
                            .texOffs(42, 82).addBox(side == 0 ? -6.0F : 0.0F, -0.5F, -1.0F, 6.0F, 1.0F, 2.0F),
                    PartPose.offset(sign * 8.0F, 0.0F, 0.5F));
            body.addOrReplaceChild(side == 0 ? "left_nozzle" : "right_nozzle", CubeListBuilder.create()
                            .texOffs(0, 74).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                    PartPose.offset(sign * 3.0F, -11.5F, 8.5F));
        }
        PartDefinition rightForearm = body.getChild("right_arm").getChild("forearm");
        rightForearm.addOrReplaceChild("drill", CubeListBuilder.create()
                        .texOffs(0, 62).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 3.0F, 6.0F)            // collar
                        .texOffs(24, 62).addBox(-2.0F, 3.0F, -2.0F, 4.0F, 4.0F, 4.0F)           // bit
                        .texOffs(40, 62).addBox(-1.5F, 7.0F, -1.5F, 3.0F, 3.0F, 3.0F)
                        .texOffs(40, 68).addBox(-1.0F, 10.0F, -1.0F, 2.0F, 3.0F, 2.0F),         // tip
                PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition leftForearm = body.getChild("left_arm").getChild("forearm");
        leftForearm.addOrReplaceChild("blade", CubeListBuilder.create()
                        .texOffs(0, 80).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F)            // hilt
                        .texOffs(112, 67).addBox(-0.5F, 2.0F, -1.5F, 1.0F, 14.0F, 3.0F),        // blade
                PartPose.offset(-2.6F, 8.0F, 0.0F));
        for (String arm : new String[] {"left_arm", "right_arm"}) {
            body.getChild(arm).addOrReplaceChild("plate", CubeListBuilder.create().mirror(arm.equals("right_arm"))
                            .texOffs(56, 57).addBox(-4.5F, -5.0F, -5.5F, 9.0F, 3.0F, 11.0F),
                    PartPose.ZERO);
        }
        body.addOrReplaceChild("chest_plate", CubeListBuilder.create()
                        .texOffs(96, 57).addBox(-7.0F, -21.0F, -6.0F, 14.0F, 9.0F, 1.0F),
                PartPose.ZERO);
        // The thigh plates hang from the legs, which hang from the root: leg() adds them
    }

    /** Shoulder pad, upper arm, then the forearm and hand hanging from the elbow. */
    private static void arm(PartDefinition body, String name, float x, boolean mirror) {
        PartDefinition arm = body.addOrReplaceChild(name, CubeListBuilder.create().mirror(mirror)
                        .texOffs(66, 22).addBox(-3.5F, -4.0F, -4.5F, 7.0F, 7.0F, 9.0F)           // shoulder pad
                        .texOffs(84, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),          // upper arm
                PartPose.offset(x, -20.0F, 0.0F));
        arm.addOrReplaceChild("forearm", CubeListBuilder.create().mirror(mirror)
                        .texOffs(98, 22).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 10.0F, 5.0F)           // forearm
                        .texOffs(100, 40).addBox(-2.0F, 10.0F, -2.0F, 4.0F, 4.0F, 4.0F),         // hand
                PartPose.offset(0.0F, 10.0F, 0.0F));
    }

    /** Thigh, then the shin with its knee pad and the long foot hanging from the knee. */
    private static void leg(PartDefinition root, String name, float x, boolean mirror) {
        PartDefinition leg = root.addOrReplaceChild(name, CubeListBuilder.create().mirror(mirror)
                        .texOffs(60, 40).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 11.0F, 6.0F),          // thigh
                PartPose.offset(x, -4.0F, 0.0F));
        PartDefinition shin = leg.addOrReplaceChild("shin", CubeListBuilder.create().mirror(mirror)
                        .texOffs(0, 40).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 13.0F, 7.0F)            // shin
                        .texOffs(100, 48).addBox(-2.5F, 0.0F, -5.5F, 5.0F, 4.0F, 2.0F),          // knee pad
                PartPose.offset(0.0F, 11.0F, 0.0F));
        shin.addOrReplaceChild("foot", CubeListBuilder.create().mirror(mirror)
                        .texOffs(28, 40).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 4.0F, 10.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        // The armour module's thigh plate
        leg.addOrReplaceChild("plate", CubeListBuilder.create().mirror(mirror)
                        .texOffs(96, 67).addBox(-3.5F, 1.0F, -4.0F, 7.0F, 6.0F, 1.0F),
                PartPose.ZERO);
    }

    private static float range(float value, float from, float to) {
        return Mth.clamp((value - from) / (to - from), 0.0F, 1.0F);
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    @Override
    public void setupAnim(MechRenderState state) {
        super.setupAnim(state);
        // A heavy, slow gait: long steps, knees bending on the way forward, arms swinging against the legs
        float pos = state.walkAnimationPos * 0.45F;
        float amount = Math.min(1.0F, state.walkAnimationSpeed);
        float swing = Mth.cos(pos) * 0.6F * amount;
        leftLeg.xRot = swing;
        rightLeg.xRot = -swing;
        leftShin.xRot = Math.max(0.0F, Mth.sin(pos)) * 0.7F * amount;
        rightShin.xRot = Math.max(0.0F, -Mth.sin(pos)) * 0.7F * amount;
        leftArm.xRot = -swing * 0.7F;
        rightArm.xRot = swing * 0.7F;
        leftForearm.xRot = -0.25F;
        rightForearm.xRot = -0.25F;
        leftArm.zRot = 0.08F;
        rightArm.zRot = -0.08F;
        // Each step lands with a slight dip of the whole body
        body.y += Math.abs(Mth.sin(pos)) * 1.2F * amount;

        head.yRot = Mth.clamp(state.yRot, -60.0F, 60.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(state.xRot, -30.0F, 40.0F) * Mth.DEG_TO_RAD;

        // The pilot's swing becomes a punch thrown forward by the right arm
        if (state.punch > 0.0F) {
            float punch = Mth.sin(state.punch * Mth.PI);
            rightArm.xRot = -1.5F * punch;
            rightForearm.xRot = -0.25F + 0.2F * punch;
        }

        crouch(state.crouch);
        petals(state.open);
        fold(state.fold);
        modules(state);

        if (state.disabled && state.crouch <= 0.0F) {
            // Shut down: it slumps, head and arms hanging
            body.xRot = 0.3F;
            head.xRot = 0.7F;
            head.yRot = 0.0F;
            leftArm.xRot = 0.2F;
            rightArm.xRot = 0.2F;
        }

        // From its own cockpit the pilot's eyes are at the visor: trunk, head and petals are left
        // out, the arms and the legs stay in view
        boolean outside = !state.firstPerson;
        body.skipDraw = !outside;
        head.visible = outside;
        bay.visible = outside;
        for (ModelPart petal : upperPetals) {
            petal.visible = outside;
        }
        for (ModelPart petal : lowerPetals) {
            petal.visible = outside;
        }
    }

    /**
     * The front opens like a flower: the upper petals first, then the lower ones, each swinging out
     * on its outer hinge and tilting away from the middle; the visor plate lifts last.
     */
    private void petals(float open) {
        float upper = smooth(range(open, 0.0F, 0.7F));
        float lower = smooth(range(open, 0.2F, 0.9F));
        float lift = smooth(range(open, 0.35F, 1.0F));
        upperPetals[0].yRot = upper * PETAL_SWING;
        upperPetals[0].zRot = upper * PETAL_TILT;
        upperPetals[1].yRot = -upper * PETAL_SWING;
        upperPetals[1].zRot = -upper * PETAL_TILT;
        lowerPetals[0].yRot = lower * PETAL_SWING;
        lowerPetals[0].zRot = -lower * PETAL_TILT;
        lowerPetals[1].yRot = -lower * PETAL_SWING;
        lowerPetals[1].zRot = lower * PETAL_TILT;
        visor.xRot = -lift * 1.6F;
    }

    /**
     * The modules' parts coming out, each from 0 (stowed, not drawn) to 1 (out). The wings swing up
     * out of the back unit and unfold segment by segment; the drill slides out under the right hand
     * and turns; the blade grows out of the left forearm; the armour plates close in onto the body.
     */
    private void modules(MechRenderState state) {
        float jet = state.jet;
        for (int side = 0; side < 2; side++) {
            float sign = side == 0 ? 1.0F : -1.0F;
            float root = smooth(range(jet, 0.0F, 0.5F));
            float mid = smooth(range(jet, 0.3F, 0.75F));
            float tip = smooth(range(jet, 0.55F, 1.0F));
            wings[side].visible = jet > 0.0F;
            scale(wings[side], 0.3F + 0.7F * root);
            wings[side].zRot = sign * Mth.lerp(root, -1.45F, 0.45F);
            wings[side].yRot = sign * 0.35F * root;
            wingMids[side].zRot = sign * Mth.lerp(mid, 2.9F, 0.12F);
            wingTips[side].zRot = sign * Mth.lerp(tip, 2.9F, 0.15F);
            nozzles[side].visible = jet > 0.0F;
            scale(nozzles[side], smooth(range(jet, 0.0F, 0.4F)));
        }
        float drillOut = smooth(state.drill);
        drill.visible = state.drill > 0.0F;
        drill.y = Mth.lerp(drillOut, 2.0F, 12.0F);
        scale(drill, 0.35F + 0.65F * drillOut);
        drill.yRot = state.drillSpin;
        float bladeOut = smooth(state.blade);
        blade.visible = state.blade > 0.0F;
        blade.xScale = blade.zScale = 0.6F + 0.4F * bladeOut;
        blade.yScale = 0.08F + 0.92F * bladeOut;
        float armour = smooth(state.armor);
        for (ModelPart plate : plates) {
            plate.visible = state.armor > 0.0F;
            scale(plate, 0.7F + 0.3F * armour);
        }
        // Each plate closes in from a little way out
        plates[0].y = -5.0F * (1.0F - armour);
        plates[1].y = -5.0F * (1.0F - armour);
        plates[2].z = -4.0F * (1.0F - armour);
        plates[3].z = -3.0F * (1.0F - armour);
        plates[4].z = -3.0F * (1.0F - armour);
    }

    private static void scale(ModelPart part, float scale) {
        part.xScale = scale;
        part.yScale = scale;
        part.zScale = scale;
    }

    /**
     * Folding up: arms crossed over the trunk, the head sinking into it, the legs drawn up under it.
     * {@code f} goes from 0 (as usual) to 1 (packed; the renderer then shrinks it into its frame).
     */
    private void fold(float f) {
        if (f <= 0.0F) {
            return;
        }
        float arms = smooth(range(f, 0.1F, 0.45F));
        leftArm.xRot = Mth.lerp(arms, leftArm.xRot, -1.35F);
        rightArm.xRot = Mth.lerp(arms, rightArm.xRot, -1.35F);
        leftArm.zRot = Mth.lerp(arms, leftArm.zRot, -0.55F);
        rightArm.zRot = Mth.lerp(arms, rightArm.zRot, 0.55F);
        leftForearm.xRot = Mth.lerp(arms, leftForearm.xRot, -1.2F);
        rightForearm.xRot = Mth.lerp(arms, rightForearm.xRot, -1.2F);
        float head = smooth(range(f, 0.3F, 0.6F));
        this.head.y += 6.0F * head;
        this.head.xRot = Mth.lerp(head, this.head.xRot, 0.0F);
        this.head.yRot = Mth.lerp(head, this.head.yRot, 0.0F);
        float legs = smooth(range(f, 0.3F, 0.65F));
        for (ModelPart[] leg : new ModelPart[][] {{leftLeg, leftShin, leftFoot}, {rightLeg, rightShin, rightFoot}}) {
            leg[0].xRot = Mth.lerp(legs, leg[0].xRot, -1.55F);
            leg[1].xRot = Mth.lerp(legs, leg[1].xRot, 2.9F);
            leg[2].xRot = Mth.lerp(legs, leg[2].xRot, -1.35F);
        }
        body.y += 4.0F * legs;
        leftLeg.y += 4.0F * legs;
        rightLeg.y += 4.0F * legs;
    }

    /**
     * A crouch on both legs, knees forward, so the open bay comes down to a step's height. {@code k}
     * goes from 0 (standing) to 1 (crouched). The trunk stays upright, the pilot's bay level.
     */
    private void crouch(float k) {
        if (k <= 0.0F) {
            return;
        }
        body.y += CROUCH_DROP * k;
        leftLeg.y += CROUCH_DROP * k;
        rightLeg.y += CROUCH_DROP * k;
        for (ModelPart[] leg : new ModelPart[][] {{leftLeg, leftShin, leftFoot}, {rightLeg, rightShin, rightFoot}}) {
            leg[0].xRot = Mth.lerp(k, leg[0].xRot, -1.35F);
            leg[1].xRot = Mth.lerp(k, leg[1].xRot, 2.35F);
            leg[2].xRot = Mth.lerp(k, leg[2].xRot, -1.0F);
        }
        leftArm.xRot = Mth.lerp(k, leftArm.xRot, -0.35F);
        rightArm.xRot = Mth.lerp(k, rightArm.xRot, -0.35F);
        leftArm.zRot = Mth.lerp(k, leftArm.zRot, 0.3F);
        rightArm.zRot = Mth.lerp(k, rightArm.zRot, -0.3F);
    }
}
