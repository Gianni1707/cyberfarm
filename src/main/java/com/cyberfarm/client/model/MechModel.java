package com.cyberfarm.client.model;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.renderer.MechRenderState;

import com.cyberfarm.port.client.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import com.cyberfarm.port.client.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The mech: a super-robot humanoid about 3.6 blocks tall in the mod's teal, mint and steel. A
 * broad chest whose two plates meet in a prow, big angular shoulder pads with upswept flanges, a
 * steel V crest on the visor, two thrusters on its back, flared shins and big boots. Its trunk is
 * a bay the pilot stands in, back against a harness, eyes at the head's visor. The chest plates
 * open like a book, the two abdomen doors swing aside after them, and the visor lifts with the
 * crest on it. Model units are 1/16 block, feet at y = 24. Texture layout: 128x128, the boxes'
 * places listed in MECH_BOXES in tools/texgen.py.
 */
public class MechModel extends EntityModel<MechRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(CyberFarm.id("mech"), "main");
    /** The chest plates' angle when shut, which makes the prow, and how far they swing out, in radians. */
    private static final float CHEST_REST = 0.22F;
    private static final float CHEST_SWING = 1.45F;
    /** How far the body sinks when the mech crouches to take its pilot, and how far each hip slides out to let them through, in model units. */
    private static final float CROUCH_DROP = 14.5F;
    private static final float CROUCH_SPREAD = 2.6F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart visor;
    private final ModelPart bay;
    private final ModelPart[] chestPlates;
    private final ModelPart[] abdomenDoors;
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
    private final ModelPart[] thrusters;
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
        this.chestPlates = new ModelPart[] {body.getChild("upper_left_petal"), body.getChild("upper_right_petal")};
        this.abdomenDoors = new ModelPart[] {body.getChild("lower_left_petal"), body.getChild("lower_right_petal")};
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
        this.thrusters = new ModelPart[] {body.getChild("left_thruster"), body.getChild("right_thruster")};
        this.wings = new ModelPart[] {body.getChild("left_wing"), body.getChild("right_wing")};
        this.wingMids = new ModelPart[] {wings[0].getChild("mid"), wings[1].getChild("mid")};
        this.wingTips = new ModelPart[] {wingMids[0].getChild("tip"), wingMids[1].getChild("tip")};
        this.nozzles = new ModelPart[] {thrusters[0].getChild("nozzle"), thrusters[1].getChild("nozzle")};
        this.drill = rightForearm.getChild("drill");
        this.blade = leftForearm.getChild("blade");
        // Shoulders first, then the chest, then the thighs: modules() moves them by that order
        this.plates = new ModelPart[] {leftArm.getChild("plate"), rightArm.getChild("plate"),
                chestPlates[0].getChild("chest_plate"), chestPlates[1].getChild("chest_plate"),
                leftLeg.getChild("plate"), rightLeg.getChild("plate")};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // The trunk, from the hips (y -4) to the shoulders' top (y -26): a shell open in front
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(16, 0).addBox(-7.0F, -22.0F, 3.0F, 14.0F, 22.0F, 3.0F)          // back wall
                        .texOffs(0, 0).addBox(-7.0F, -22.0F, -4.0F, 1.0F, 22.0F, 7.0F)           // left wall
                        .texOffs(0, 0).mirror().addBox(6.0F, -22.0F, -4.0F, 1.0F, 22.0F, 7.0F)   // right wall
                        .mirror(false)
                        .texOffs(82, 58).addBox(-6.0F, -2.0F, -4.0F, 12.0F, 2.0F, 7.0F)          // floor
                        .texOffs(38, 70).addBox(-6.0F, -22.0F, -4.0F, 12.0F, 1.0F, 7.0F)         // roof
                        .texOffs(0, 45).addBox(-6.0F, -21.0F, 6.0F, 12.0F, 10.0F, 3.0F)          // back unit
                        // The shoulder yokes the pads sit against: they also hide the pilot's arms, wider than the bay
                        .texOffs(0, 96).addBox(-9.0F, -24.0F, -4.0F, 2.0F, 12.0F, 6.0F)
                        .texOffs(0, 96).mirror().addBox(7.0F, -24.0F, -4.0F, 2.0F, 12.0F, 6.0F),
                PartPose.offset(0.0F, -4.0F, 0.0F));
        // The harness the pilot stands against
        body.addOrReplaceChild("bay", CubeListBuilder.create()
                        .texOffs(78, 29).addBox(-1.0F, -16.0F, 2.0F, 2.0F, 14.0F, 1.0F)          // spine
                        .texOffs(28, 92).addBox(-4.0F, -17.0F, 2.0F, 8.0F, 1.0F, 1.0F),          // shoulder bar
                PartPose.ZERO);

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(30, 45).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 7.0F, 6.0F)           // helm
                        .texOffs(40, 79).addBox(-3.0F, -5.0F, 2.0F, 6.0F, 5.0F, 2.0F)            // nape
                        .texOffs(26, 70).addBox(-5.0F, -6.0F, -2.5F, 1.0F, 4.0F, 5.0F)           // ears
                        .texOffs(26, 70).mirror().addBox(4.0F, -6.0F, -2.5F, 1.0F, 4.0F, 5.0F),
                PartPose.offset(0.0F, -22.0F, 0.0F));
        // The visor plate, hinged on the head's brow, lifts to let the pilot's eyes through; it carries the gem and the V crest
        PartDefinition visor = head.addOrReplaceChild("visor", CubeListBuilder.create()
                        .texOffs(42, 87).addBox(-4.0F, 0.0F, -1.0F, 8.0F, 4.0F, 1.0F)
                        .texOffs(22, 92).addBox(-1.0F, -0.6F, -1.8F, 2.0F, 2.0F, 1.0F),          // gem
                PartPose.offset(0.0F, -7.0F, -4.0F));

        // Everything that comes in pairs is written for the left side (x negative) and flipped for the right
        for (boolean right : new boolean[] {false, true}) {
            String side = right ? "right" : "left";
            float sign = right ? -1.0F : 1.0F;
            visor.addOrReplaceChild(side + "_fin", CubeListBuilder.create().mirror(right)
                            .texOffs(0, 92).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F)
                            .texOffs(102, 87).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                    PartPose.offsetAndRotation(sign * -0.6F, -1.0F, -1.3F, -0.2F, 0.0F, sign * -1.0F));

            // Chest plates, hinged on the trunk's outer edges and angled to meet in a prow; abdomen doors under them
            PartDefinition chest = body.addOrReplaceChild("upper_" + side + "_petal", CubeListBuilder.create().mirror(right)
                            .texOffs(110, 45).addBox(x(right, 0.0F, 7.0F), 0.0F, -2.0F, 7.0F, 10.0F, 2.0F)
                            .texOffs(104, 96).addBox(x(right, 0.0F, 7.0F), 1.0F, -3.0F, 7.0F, 6.0F, 1.0F),   // pectoral
                    PartPose.offsetAndRotation(sign * -7.0F, -20.0F, -4.0F, 0.0F, sign * CHEST_REST, 0.0F));
            body.addOrReplaceChild("lower_" + side + "_petal", CubeListBuilder.create().mirror(right)
                            .texOffs(88, 96).addBox(x(right, -0.3F, 7.0F), 0.0F, -1.0F, 7.0F, 8.0F, 1.0F)
                            .texOffs(14, 114).addBox(x(right, 1.2F, 5.0F), 1.0F, -2.0F, 5.0F, 4.0F, 1.0F),   // ribs
                    PartPose.offset(sign * -7.0F, -9.65F, -4.0F));

            // Shoulder pad, flat on top for the hangar's clamp, with its skirt and upswept flange; upper arm; forearm, cuff and fist
            PartDefinition arm = body.addOrReplaceChild(side + "_arm", CubeListBuilder.create().mirror(right)
                            .texOffs(16, 96).addBox(x(right, -3.5F, 6.0F), -5.5F, -5.5F, 6.0F, 5.0F, 11.0F)
                            .texOffs(0, 29).addBox(x(right, -4.5F, 1.0F), -3.5F, -5.0F, 1.0F, 6.0F, 10.0F)
                            .texOffs(84, 87).addBox(x(right, -2.5F, 4.0F), -5.0F, -6.5F, 4.0F, 4.0F, 1.0F)
                            .texOffs(50, 96).addBox(-2.0F, -0.5F, -2.0F, 4.0F, 10.0F, 4.0F),
                    PartPose.offset(sign * -11.0F, -20.0F, 0.0F));
            arm.addOrReplaceChild("flange", CubeListBuilder.create().mirror(right)
                            .texOffs(66, 96).addBox(x(right, -2.0F, 2.0F), -4.0F, -4.5F, 2.0F, 4.0F, 9.0F)
                            .texOffs(0, 114).addBox(x(right, -2.0F, 2.0F), -6.0F, -2.5F, 2.0F, 2.0F, 5.0F),
                    PartPose.offsetAndRotation(sign * -4.5F, -3.0F, 0.0F, 0.0F, 0.0F, sign * -0.25F));
            PartDefinition forearm = arm.addOrReplaceChild("forearm", CubeListBuilder.create().mirror(right)
                            .texOffs(78, 45).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 8.0F, 5.0F)
                            .texOffs(58, 58).addBox(-3.0F, 4.0F, -3.0F, 6.0F, 4.0F, 6.0F)
                            .texOffs(112, 70).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                    PartPose.offset(0.0F, 9.5F, 0.0F));

            // Thigh with hip armour, then the knee and the flared shin, the knee pad and the boot with its toe cap
            PartDefinition leg = root.addOrReplaceChild(side + "_leg", CubeListBuilder.create().mirror(right)
                            .texOffs(58, 29).addBox(-2.5F, 0.5F, -2.5F, 5.0F, 10.0F, 5.0F)
                            .texOffs(0, 58).addBox(x(right, -3.8F, 1.0F), 0.0F, -3.0F, 1.0F, 6.0F, 6.0F),
                    PartPose.offset(sign * -4.0F, -4.0F, 0.0F));
            PartDefinition shin = leg.addOrReplaceChild("shin", CubeListBuilder.create().mirror(right)
                            .texOffs(28, 58).addBox(-2.5F, 0.0F, -3.0F, 5.0F, 5.0F, 6.0F)
                            .texOffs(90, 0).addBox(-3.5F, 5.0F, -4.0F, 7.0F, 8.0F, 8.0F),
                    PartPose.offset(0.0F, 11.0F, 0.0F));
            shin.addOrReplaceChild("knee", CubeListBuilder.create().mirror(right)
                            .texOffs(68, 79).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 5.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, 1.0F, -3.0F, 0.3F, 0.0F, 0.0F));
            shin.addOrReplaceChild("foot", CubeListBuilder.create().mirror(right)
                            .texOffs(22, 29).addBox(-3.5F, 0.0F, -6.0F, 7.0F, 4.0F, 11.0F)
                            .texOffs(60, 87).addBox(-2.5F, 1.0F, -8.0F, 5.0F, 3.0F, 2.0F),
                    PartPose.offset(0.0F, 13.0F, 0.0F));

            // The dorsal thrusters, always there: a tube and a bell
            PartDefinition thruster = body.addOrReplaceChild(side + "_thruster", CubeListBuilder.create().mirror(right)
                            .texOffs(12, 79).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F)
                            .texOffs(24, 79).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 3.0F, 4.0F),
                    PartPose.offset(sign * -3.0F, -19.0F, 10.7F));

            // The modules' parts, stowed until they come out. Jet: a three-part wing behind the back unit, a nozzle under the thruster
            PartDefinition wing = body.addOrReplaceChild(side + "_wing", CubeListBuilder.create().mirror(right)
                            .texOffs(0, 87).addBox(x(right, -8.0F, 8.0F), -0.5F, -2.0F, 8.0F, 1.0F, 4.0F),
                    PartPose.offset(sign * -6.5F, -21.5F, 11.5F));
            PartDefinition mid = wing.addOrReplaceChild("mid", CubeListBuilder.create().mirror(right)
                            .texOffs(106, 87).addBox(x(right, -8.0F, 8.0F), -0.5F, -1.5F, 8.0F, 1.0F, 3.0F),
                    PartPose.offset(sign * -8.0F, 0.0F, 0.5F));
            mid.addOrReplaceChild("tip", CubeListBuilder.create().mirror(right)
                            .texOffs(6, 92).addBox(x(right, -6.0F, 6.0F), -0.5F, -1.0F, 6.0F, 1.0F, 2.0F),
                    PartPose.offset(sign * -8.0F, 0.0F, 0.5F));
            thruster.addOrReplaceChild("nozzle", CubeListBuilder.create().mirror(right)
                            .texOffs(104, 79).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                    PartPose.offset(0.0F, 7.0F, 0.3F));
            // Armour: a plate on the shoulder pad, one on the chest plate, one outside the hip
            arm.addOrReplaceChild("plate", CubeListBuilder.create().mirror(right)
                            .texOffs(84, 29).addBox(-2.5F, -0.5F, -6.0F, 5.0F, 1.0F, 12.0F),
                    PartPose.offset(sign * -0.5F, -6.0F, 0.0F));
            chest.addOrReplaceChild("chest_plate", CubeListBuilder.create().mirror(right)
                            .texOffs(0, 79).addBox(-2.5F, -3.5F, -0.5F, 5.0F, 7.0F, 1.0F),
                    PartPose.offset(sign * 4.3F, 4.0F, -3.1F));
            leg.addOrReplaceChild("plate", CubeListBuilder.create().mirror(right)
                            .texOffs(14, 58).addBox(-0.5F, -3.0F, -3.0F, 1.0F, 6.0F, 6.0F),
                    PartPose.offset(sign * -4.35F, 3.5F, 0.0F));
            if (right) {
                // The drill, under the right fist
                forearm.addOrReplaceChild("drill", CubeListBuilder.create()
                                .texOffs(76, 70).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 3.0F, 5.0F)        // collar
                                .texOffs(96, 70).addBox(-2.0F, 3.0F, -2.0F, 4.0F, 4.0F, 4.0F)        // bit
                                .texOffs(92, 79).addBox(-1.5F, 7.0F, -1.5F, 3.0F, 3.0F, 3.0F)
                                .texOffs(94, 87).addBox(-1.0F, 10.0F, -1.0F, 2.0F, 3.0F, 2.0F),      // tip
                        PartPose.offset(0.0F, 12.0F, 0.0F));
            } else {
                // The blade, down the outside of the left cuff
                forearm.addOrReplaceChild("blade", CubeListBuilder.create()
                                .texOffs(74, 87).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 2.0F, 3.0F)        // hilt
                                .texOffs(50, 0).addBox(-0.5F, 2.0F, -1.5F, 1.0F, 14.0F, 3.0F),       // blade
                        PartPose.offset(-3.6F, 5.0F, 0.0F));
            }
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Where a box written for the left side starts on the right side: flipped across the part's middle. */
    private static float x(boolean right, float x, float width) {
        return right ? -x - width : x;
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
        // Each step lands with a slight dip of the whole body, and the thrusters sway with the stride
        body.y += Math.abs(Mth.sin(pos)) * 1.2F * amount;
        thrusters[0].zRot = thrusters[1].zRot = Mth.sin(pos) * 0.45F * amount;
        // Standing still and powered, it breathes; not once it crouches, where the bobbing trunk would bring the abdomen doors onto the thighs
        if (amount < 0.05F && !state.disabled) {
            body.y += Mth.sin(state.ageInTicks * 0.08F) * 0.6F * (1.0F - amount * 20.0F) * (1.0F - Math.min(1.0F, state.crouch * 4.0F));
        }

        // The head turns with the pilot's, and nods half as far as the entity does: the crest stays clear of the shoulders
        head.yRot = Mth.clamp(state.yRot, -60.0F, 60.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(state.xRot, -30.0F, 40.0F) * Mth.DEG_TO_RAD * 0.5F;

        // The pilot's swing becomes a punch thrown forward by the right arm
        if (state.punch > 0.0F) {
            float punch = Mth.sin(state.punch * Mth.PI);
            rightArm.xRot = -1.5F * punch;
            rightForearm.xRot = -0.25F + 0.2F * punch;
        }

        crouch(state.crouch);
        open(state.open);
        fold(state.fold);
        modules(state);

        if (state.disabled && state.crouch <= 0.0F) {
            // Shut down: it slumps, head and arms hanging
            body.xRot = 0.3F;
            head.xRot = 0.3F;
            head.yRot = 0.0F;
            leftArm.xRot = 0.2F;
            rightArm.xRot = 0.2F;
        }

        // From its own cockpit the pilot's eyes are at the visor: trunk, head and the opening plates
        // are left out, the arms and the legs stay in view
        boolean outside = !state.firstPerson;
        body.skipDraw = !outside;
        head.visible = outside;
        bay.visible = outside;
        for (int side = 0; side < 2; side++) {
            chestPlates[side].visible = outside;
            abdomenDoors[side].visible = outside;
        }
    }

    /**
     * The front opens: the chest plates swing out like a book on their outer hinges, then the
     * abdomen doors swing aside the same way, and the visor lifts last.
     */
    private void open(float open) {
        float chest = smooth(range(open, 0.0F, 0.6F));
        float doors = smooth(range(open, 0.3F, 0.85F));
        float lift = smooth(range(open, 0.4F, 1.0F));
        chestPlates[0].yRot = CHEST_REST + chest * CHEST_SWING;
        chestPlates[1].yRot = -chestPlates[0].yRot;
        abdomenDoors[0].yRot = doors * 1.75F;
        abdomenDoors[1].yRot = -doors * 1.75F;
        visor.xRot = -lift * 1.6F;
        // With the visor up the head faces ahead: turned, the lifted crest would sweep into the shoulders
        head.xRot *= 1.0F - lift;
        head.yRot *= 1.0F - lift;
    }

    /**
     * The modules' parts coming out, each from 0 (stowed, not drawn) to 1 (out). The wings swing up
     * behind the back unit and unfold segment by segment; the drill slides out under the right fist
     * and turns; the blade grows down the left cuff; the armour plates slide in from the front.
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
        drill.y = Mth.lerp(drillOut, 6.0F, 12.0F);
        scale(drill, 0.35F + 0.65F * drillOut);
        drill.yRot = state.drillSpin;
        float bladeOut = smooth(state.blade);
        blade.visible = state.blade > 0.0F;
        blade.xScale = blade.zScale = 0.6F + 0.4F * bladeOut;
        blade.yScale = 0.08F + 0.92F * bladeOut;
        float armour = smooth(state.armor);
        for (int i = 0; i < plates.length; i++) {
            plates[i].visible = state.armor > 0.0F;
            scale(plates[i], 0.7F + 0.3F * armour);
            // Each plate slides in from in front of its place, the shoulder ones from a little further
            plates[i].z -= (i < 2 ? 5.0F : 4.0F) * (1.0F - armour);
        }
    }

    private static void scale(ModelPart part, float scale) {
        part.xScale = scale;
        part.yScale = scale;
        part.zScale = scale;
    }

    /**
     * Folding up: forearms level before the belly, the head bowed, the legs drawn up into a kneel.
     * {@code f} goes from 0 (as usual) to 1 (packed; the renderer then shrinks it into its frame).
     */
    private void fold(float f) {
        if (f <= 0.0F) {
            return;
        }
        float arms = smooth(range(f, 0.1F, 0.45F));
        leftArm.xRot = Mth.lerp(arms, leftArm.xRot, -0.55F);
        rightArm.xRot = Mth.lerp(arms, rightArm.xRot, -0.55F);
        leftArm.zRot = Mth.lerp(arms, leftArm.zRot, 0.18F);
        rightArm.zRot = Mth.lerp(arms, rightArm.zRot, -0.18F);
        leftForearm.xRot = Mth.lerp(arms, leftForearm.xRot, -1.3F);
        rightForearm.xRot = Mth.lerp(arms, rightForearm.xRot, -1.3F);
        float head = smooth(range(f, 0.3F, 0.6F));
        this.head.xRot = Mth.lerp(head, this.head.xRot, 0.35F);
        this.head.yRot = Mth.lerp(head, this.head.yRot, 0.0F);
        float legs = smooth(range(f, 0.3F, 0.65F));
        for (ModelPart[] leg : new ModelPart[][] {{leftLeg, leftShin, leftFoot}, {rightLeg, rightShin, rightFoot}}) {
            leg[0].xRot = Mth.lerp(legs, leg[0].xRot, -1.25F);
            leg[1].xRot = Mth.lerp(legs, leg[1].xRot, 2.9F);
            leg[2].xRot = Mth.lerp(legs, leg[2].xRot, -1.35F);
        }
        body.y += 4.0F * legs;
        leftLeg.y += 4.0F * legs;
        rightLeg.y += 4.0F * legs;
    }

    /**
     * A crouch on both legs, knees forward and hips slid apart, so the open bay comes down to a
     * step's height and the pilot climbs in between the knees. {@code k} goes from 0 (standing) to
     * 1 (crouched). The trunk stays upright, the pilot's bay level, the head facing ahead.
     */
    private void crouch(float k) {
        if (k <= 0.0F) {
            return;
        }
        body.y += CROUCH_DROP * k;
        leftLeg.y += CROUCH_DROP * k;
        rightLeg.y += CROUCH_DROP * k;
        leftLeg.x -= CROUCH_SPREAD * k;
        rightLeg.x += CROUCH_SPREAD * k;
        for (ModelPart[] leg : new ModelPart[][] {{leftLeg, leftShin, leftFoot}, {rightLeg, rightShin, rightFoot}}) {
            leg[0].xRot = Mth.lerp(k, leg[0].xRot, -1.0F);
            leg[1].xRot = Mth.lerp(k, leg[1].xRot, 2.29F);
            leg[2].xRot = Mth.lerp(k, leg[2].xRot, -1.29F);
        }
        leftArm.xRot = Mth.lerp(k, leftArm.xRot, -0.35F);
        rightArm.xRot = Mth.lerp(k, rightArm.xRot, -0.35F);
        leftArm.zRot = Mth.lerp(k, leftArm.zRot, 0.4F);
        rightArm.zRot = Mth.lerp(k, rightArm.zRot, -0.4F);
        // Turned, the visor and the crest would sweep into the shoulders as they tip inwards
        head.xRot *= 1.0F - k;
        head.yRot *= 1.0F - k;
    }
}
