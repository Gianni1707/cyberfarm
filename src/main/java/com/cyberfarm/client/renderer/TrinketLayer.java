package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.trinket.EnergySphereItem;
import com.cyberfarm.trinket.GravityBootsItem;
import com.cyberfarm.trinket.NeuralChipItem;
import com.cyberfarm.trinket.PocketDroneItem;
import com.cyberfarm.trinket.TrinketType;
import com.cyberfarm.trinket.Trinkets;
import com.cyberfarm.trinket.UtilityBeltItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.joml.Quaternionf;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The trinkets drawn on the player: the Gravity Boots on the legs (lit, with the field under the feet, while they
 * work), the Neural Chip at the back of the neck, and the Utility Belt with what it holds (the Pocket Drone,
 * once out, is PocketDroneView's). Plain tinted boxes in model pixels, on the model's own parts so they follow its pose.
 */
public class TrinketLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    /** What the layer needs of a player's trinkets, read once per frame from the player. */
    public record Look(double hover, boolean bootsLit, boolean chip, boolean chipLit, boolean belt, boolean sphere, float charge,
            boolean projector, boolean surveyTool, boolean drone, boolean droneOut, int sphereTier, boolean cableSwapper) {
        static final Look NONE = new Look(-1, false, false, false, false, false, 0, false, false, false, false, 0, false);

        boolean any() {
            return hover >= 0 || chip || belt;
        }
    }

    public static final ContextKey<Look> LOOK = new ContextKey<>(CyberFarm.id("trinkets"));

    private static final int LIGHT_FULL = 0xF000F0;
    private static final int WHITE = 0xFFE4EAEA, CERAMIC = 0xFFBDC6C5, STEEL = 0xFF96A3A2, DARK_STEEL = 0xFF465353;
    private static final int MINT = 0xFF5EC2B8, PALE_MINT = 0xFFA5EAD4, GLOW = 0xFFD1FEE7, OFF = 0xFF055B57, TEAL = 0xFF2F6468;
    /** The black of the Cable Swapper's die slot. */
    private static final int SLOT_DARK = 0xFF132423;
    private static final int LEATHER = 0xFF7A5230, LEATHER_HI = 0xFF9C6B42, LEATHER_LO = 0xFF5A3A20;

    public TrinketLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    /** Reads a player's trinkets for the layer. */
    public static Look look(Player player) {
        ItemStack boots = Trinkets.worn(player, TrinketType.FEET, GravityBootsItem.class);
        ItemStack chip = NeuralChipItem.worn(player);
        ItemStack belt = UtilityBeltItem.worn(player);
        ItemStack sphere = UtilityBeltItem.content(belt, UtilityBeltItem.SPHERE);
        ItemStack drone = UtilityBeltItem.content(belt, UtilityBeltItem.DRONE);
        boolean powered = EnergySphereItem.energy(sphere) > 0;
        Look look = new Look(boots.isEmpty() ? -1 : GravityBootsItem.height(boots), powered && GravityBootsItem.height(boots) > 0 && !player.onGround(),
                !chip.isEmpty(), powered && !NeuralChipItem.programs(chip).isEmpty(),
                !belt.isEmpty(), !sphere.isEmpty(), EnergySphereItem.energy(sphere) / (float) Math.max(1, EnergySphereItem.capacity(sphere)),
                !UtilityBeltItem.content(belt, UtilityBeltItem.PROJECTOR).isEmpty(), !UtilityBeltItem.content(belt, UtilityBeltItem.SURVEY_TOOL).isEmpty(),
                !drone.isEmpty(), PocketDroneItem.isActive(drone) || com.cyberfarm.client.PocketDroneView.landing(player),
                EnergySphereItem.tier(sphere), !UtilityBeltItem.content(belt, UtilityBeltItem.CABLE_SWAPPER).isEmpty());
        return look.any() ? look : Look.NONE;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        Look look = state.getRenderData(LOOK);
        if (look == null || look == Look.NONE || state.isInvisible) {
            return;
        }
        float time = state.ageInTicks;
        PlayerModel model = getParentModel();
        if (look.hover() >= 0) {
            boot(pose, collector, light, look, time, model.rightLeg, -1);
            boot(pose, collector, light, look, time, model.leftLeg, 1);
            if (look.bootsLit()) {
                field(pose, collector, look, time);
            }
        }
        if (look.chip()) {
            pose.pushPose();
            model.body.translateAndRotate(pose);
            pose.scale(1 / 16.0F, 1 / 16.0F, 1 / 16.0F);
            chip(pose, collector, light, look.chipLit(), time);
            pose.popPose();
        }
        if (look.belt()) {
            pose.pushPose();
            model.body.translateAndRotate(pose);
            pose.scale(1 / 16.0F, 1 / 16.0F, 1 / 16.0F);
            belt(pose, collector, light, look, time);
            if (look.sphere()) {
                glass(pose, collector, light);
            }
            pose.popPose();
        }
    }

    // ---------------------------------------------------------------- Gravity Boots

    /** One boot, {@code out} the side facing away from the other leg (-1 right, +1 left in model x). */
    private static void boot(PoseStack pose, SubmitNodeCollector collector, int light, Look look, float time, ModelPart leg, int out) {
        pose.pushPose();
        leg.translateAndRotate(pose);
        pose.scale(1 / 16.0F, 1 / 16.0F, 1 / 16.0F);
        int lit = look.bootsLit() ? LIGHT_FULL : light;
        int glow = look.bootsLit() ? GLOW : OFF;
        boolean pulse = look.bootsLit() && (int) time % 8 < 5;
        collector.submitCustomGeometry(pose, ModelParts.SOLID, (p, buffer) -> {
            // Shaft to mid calf and the shoe, white ceramic a little proud of the leg
            ModelParts.box(p, buffer, -2.3F, 6.0F, -2.3F, 2.3F, 11.0F, 2.3F, WHITE, CERAMIC, light);
            ModelParts.box(p, buffer, -2.4F, 5.7F, -2.4F, 2.4F, 6.0F, 2.4F, STEEL, STEEL, light);
            // The sole, steel, flared, with its glowing groove
            ModelParts.box(p, buffer, -2.6F, 11.0F, -2.9F, 2.6F, 11.5F, 2.6F, STEEL, STEEL, light);
            ModelParts.box(p, buffer, -2.5F, 11.5F, -2.8F, 2.5F, 11.75F, 2.5F, glow, glow, lit);
            ModelParts.box(p, buffer, -2.6F, 11.75F, -2.9F, 2.6F, 12.1F, 2.6F, DARK_STEEL, DARK_STEEL, light);
            // The keel down the shin
            ModelParts.box(p, buffer, -0.5F, 6.6F, -2.6F, 0.5F, 9.6F, -2.3F, MINT, MINT, light);
            // The disc on the outer ankle, its boss lit while the boots work
            float face = out * 2.3F;
            ModelParts.box(p, buffer, Math.min(face, face + out * 0.4F), 7.6F, -1.4F, Math.max(face, face + out * 0.4F), 10.4F, 1.4F, MINT, MINT, light);
            ModelParts.box(p, buffer, Math.min(face, face + out * 0.6F), 8.4F, -0.6F, Math.max(face, face + out * 0.6F), 9.6F, 0.6F,
                    pulse ? GLOW : glow, pulse ? GLOW : glow, lit);
        });
        // The wing: four blades fanning up and back from the disc, lit one after another
        for (int blade = 0; blade < 4; blade++) {
            pose.pushPose();
            pose.translate(out * 2.5F, 7.9F, 1.0F);
            pose.mulPose(Axis.XP.rotationDegrees(-(15 + blade * 18)));
            boolean on = look.bootsLit() && ((int) time - blade * 3 & 15) < 6;
            int colour = on ? GLOW : blade % 2 == 0 ? WHITE : CERAMIC;
            float length = 3.4F - blade * 0.3F;
            collector.submitCustomGeometry(pose, ModelParts.SOLID, (p, buffer) ->
                    ModelParts.box(p, buffer, Math.min(0, out * 0.5F), -0.25F, 0, Math.max(0, out * 0.5F), 0.25F, length, colour, colour, on ? LIGHT_FULL : light));
            pose.popPose();
        }
        pose.popPose();
    }

    /**
     * The field: rings of light leaving the soles, wide enough to hold both feet, that sink to the ground while they
     * spread and fade. The model's y runs down: the soles at 24, the ground the hover's height below them.
     */
    private static void field(PoseStack pose, SubmitNodeCollector collector, Look look, float time) {
        pose.pushPose();
        pose.scale(1 / 16.0F, 1 / 16.0F, 1 / 16.0F);
        float drop = (float) look.hover() * 16.0F;
        collector.submitCustomGeometry(pose, ModelParts.SEE_THROUGH, (p, buffer) -> {
            for (int ring = 0; ring < 3; ring++) {
                float q = ((time + ring * 16 / 3.0F) % 16) / 16.0F;
                float r = 4.2F + q * (2.5F + 2.0F * (float) look.hover());
                int alpha = (int) (230 * (1.0F - q * q));
                int colour = alpha << 24 | (q < 0.5F ? GLOW : MINT) & 0xFFFFFF;
                square(p, buffer, r, 24.4F + q * (drop - 0.6F), 0.9F, colour);
            }
        });
        pose.popPose();
    }

    /** A flat square ring of half-size {@code r} and width {@code w}, lying at height {@code y}. */
    private static void square(PoseStack.Pose p, VertexConsumer buffer, float r, float y, float w, int colour) {
        ModelParts.box(p, buffer, -r, y - 0.3F, -r, r, y, -r + w, colour, colour, LIGHT_FULL);
        ModelParts.box(p, buffer, -r, y - 0.3F, r - w, r, y, r, colour, colour, LIGHT_FULL);
        ModelParts.box(p, buffer, -r, y - 0.3F, -r + w, -r + w, y, r - w, colour, colour, LIGHT_FULL);
        ModelParts.box(p, buffer, r - w, y - 0.3F, -r + w, r, y, r - w, colour, colour, LIGHT_FULL);
    }

    // ---------------------------------------------------------------- Neural Chip

    /** The spinal implant at the nape: a node, three vertebrae, a ring, traces out to the sides; a pulse runs down it. */
    private static void chip(PoseStack pose, SubmitNodeCollector collector, int light, boolean lit, float time) {
        float head = lit ? (time % 32) / 32.0F * 7.5F - 0.8F : -9;
        collector.submitCustomGeometry(pose, ModelParts.SOLID, (p, buffer) -> {
            float z0 = 2.0F, z1 = 2.55F;
            // The node, its LED blinking even at rest
            ModelParts.box(p, buffer, -1.0F, -0.6F, z0, 1.0F, 0.9F, z1, STEEL, STEEL, light);
            boolean led = lit || (int) time % 24 < 12;
            ModelParts.box(p, buffer, -0.4F, -0.3F, z1, 0.4F, 0.3F, z1 + 0.15F, led ? GLOW : OFF, led ? GLOW : OFF, led ? LIGHT_FULL : light);
            float[][] plates = {{1.2F, 2.1F, 1.1F}, {2.5F, 3.7F, 1.6F}, {4.0F, 4.9F, 1.1F}};
            for (int i = 0; i < plates.length; i++) {
                float[] plate = plates[i];
                ModelParts.box(p, buffer, -plate[2], plate[0], z0, plate[2], plate[1], z1, WHITE, CERAMIC, light);
                float level = Math.max(0, 1 - Math.abs(i + 1 - head) / 1.3F);
                int inlay = level > 0.3F ? GLOW : OFF;
                ModelParts.box(p, buffer, -0.3F, plate[0] + 0.2F, z1, 0.3F, plate[1] - 0.2F, z1 + 0.12F, inlay, inlay, level > 0.3F ? LIGHT_FULL : light);
            }
            // The rail between them and the ring at the bottom
            ModelParts.box(p, buffer, -0.25F, 0.9F, z0, 0.25F, 5.4F, z1 - 0.1F, DARK_STEEL, DARK_STEEL, light);
            ModelParts.box(p, buffer, -0.7F, 5.4F, z0, 0.7F, 6.6F, z1, STEEL, STEEL, light);
            // The traces out to the sides of the middle vertebra
            boolean traces = Math.max(0, 1 - Math.abs(5.2F - head) / 1.4F) > 0.3F;
            int trace = traces ? GLOW : OFF;
            for (int side = -1; side <= 1; side += 2) {
                ModelParts.box(p, buffer, Math.min(side * 1.6F, side * 3.0F), 3.0F, z0, Math.max(side * 1.6F, side * 3.0F), 3.3F, z0 + 0.2F, trace, trace, traces ? LIGHT_FULL : light);
                ModelParts.box(p, buffer, Math.min(side * 2.7F, side * 3.0F), 3.3F, z0, Math.max(side * 2.7F, side * 3.0F), 4.6F, z0 + 0.2F, trace, trace, traces ? LIGHT_FULL : light);
            }
        });
    }

    // ---------------------------------------------------------------- Utility Belt

    /**
     * The Utility Belt, the user's "tecno-enamel" pick: a brown leather strap with a mint enamel stripe all round and
     * ceramic studs, a ceramic buckle plate with a lit slit; on the left hip a steel hanger the sphere hangs from
     * (collar and cap over the glass); the Projector and the Survey Tool upright on a steel rail on the right, held by a
     * leather strap with a mint latch; at the back a steel magnet plate with two mint bars the drone sits on folded,
     * an empty dock with a glow dot when it is out. The slit, the stripe and the bars breathe slowly. Body pixels, y
     * down, front -z. An empty holder shows nothing of its own.
     */
    private static void belt(PoseStack pose, SubmitNodeCollector collector, int light, Look look, float time) {
        float breath = 0.5F + 0.5F * Mth.sin(time * Mth.TWO_PI / 80.0F);
        int slit = mix(MINT, GLOW, breath), stripe = mix(MINT, PALE_MINT, breath);
        collector.submitCustomGeometry(pose, ModelParts.SOLID, (p, buffer) -> {
            // The strap, its lower edge and the enamel stripe
            ModelParts.box(p, buffer, -4.3F, 9.4F, -2.3F, 4.3F, 10.7F, 2.3F, LEATHER_HI, LEATHER, light);
            ModelParts.box(p, buffer, -4.32F, 10.5F, -2.32F, 4.32F, 10.7F, 2.32F, LEATHER_LO, LEATHER_LO, light);
            ModelParts.box(p, buffer, -4.33F, 9.85F, -2.33F, 4.33F, 10.25F, 2.33F, stripe, stripe, light);
            // Ceramic studs: two on each side, two at the back, one at the front by the hanger
            ModelParts.box(p, buffer, 4.3F, 9.65F, -1.25F, 4.38F, 10.45F, -0.75F, WHITE, CERAMIC, light);
            ModelParts.box(p, buffer, -4.38F, 9.65F, -1.25F, -4.3F, 10.45F, -0.75F, WHITE, CERAMIC, light);
            ModelParts.box(p, buffer, 4.3F, 9.65F, 0.75F, 4.38F, 10.45F, 1.25F, WHITE, CERAMIC, light);
            ModelParts.box(p, buffer, -4.38F, 9.65F, 0.75F, -4.3F, 10.45F, 1.25F, WHITE, CERAMIC, light);
            if (!look.cableSwapper()) {
                ModelParts.box(p, buffer, -3.55F, 9.65F, 2.3F, -3.05F, 10.45F, 2.38F, WHITE, CERAMIC, light);
            }
            ModelParts.box(p, buffer, 3.05F, 9.65F, 2.3F, 3.55F, 10.45F, 2.38F, WHITE, CERAMIC, light);
            ModelParts.box(p, buffer, 3.65F, 9.65F, -2.38F, 4.15F, 10.45F, -2.3F, WHITE, CERAMIC, light);
            // The buckle: a ceramic plate, a dark recess, the lit slit
            ModelParts.box(p, buffer, -1.25F, 9.1F, -2.65F, 1.25F, 11.0F, -2.3F, CERAMIC, STEEL, light);
            ModelParts.box(p, buffer, -1.0F, 9.4F, -2.7F, 1.0F, 10.7F, -2.65F, DARK_STEEL, DARK_STEEL, light);
            ModelParts.box(p, buffer, -0.7F, 9.85F, -2.75F, 0.7F, 10.25F, -2.7F, slit, slit, LIGHT_FULL);
            if (look.sphere()) {
                // Hanging on the left hip from a steel hanger with a mint mark: a collar and a cap over the glass
                ModelParts.box(p, buffer, 2.0F, 10.6F, -2.8F, 3.5F, 11.45F, -2.3F, STEEL, DARK_STEEL, light);
                ModelParts.box(p, buffer, 2.3F, 10.85F, -2.85F, 3.2F, 11.15F, -2.8F, MINT, MINT, light);
                ModelParts.box(p, buffer, SPHERE_X - 0.8F, 11.4F, SPHERE_Z - 0.8F, SPHERE_X + 0.8F, 11.8F, -2.8F, STEEL, DARK_STEEL, light);
                ModelParts.box(p, buffer, SPHERE_X - 1.0F, 11.8F, SPHERE_Z - 1.0F, SPHERE_X + 1.0F, 12.05F, SPHERE_Z + 1.0F, DARK_STEEL, DARK_STEEL, light);
                // The bolt the plasma leaps from, and the nub under the glass
                ModelParts.box(p, buffer, SPHERE_X - 0.15F, 12.05F, SPHERE_Z - 0.15F, SPHERE_X + 0.15F, SPHERE_Y, SPHERE_Z + 0.15F, STEEL, STEEL, light);
                ModelParts.box(p, buffer, SPHERE_X - 0.35F, SPHERE_Y + SPHERE_R - 0.1F, SPHERE_Z - 0.35F, SPHERE_X + 0.35F, SPHERE_Y + SPHERE_R + 0.3F,
                        SPHERE_Z + 0.35F, STEEL, DARK_STEEL, light);
                boolean charged = look.charge() > 0;
                float c = 0.32F + 0.08F * Mth.sin(time * 0.6F);
                int core = charged ? GLOW : DARK_STEEL;
                ModelParts.box(p, buffer, SPHERE_X - c, SPHERE_Y - c, SPHERE_Z - c, SPHERE_X + c, SPHERE_Y + c, SPHERE_Z + c, core, core,
                        charged ? LIGHT_FULL : light);
                if (charged) {
                    plasma(p, buffer, look.charge(), time, look.sphereTier());
                }
            }
            // The tools stand upright on the right of the buckle, on a steel rail, a leather strap with a mint latch across them
            if (look.projector()) {
                ModelParts.box(p, buffer, -3.9F, 7.4F, -3.05F, -2.7F, 11.9F, -2.4F, STEEL, DARK_STEEL, light);
                ModelParts.box(p, buffer, -3.75F, 6.8F, -2.95F, -2.85F, 7.4F, -2.5F, MINT, MINT, LIGHT_FULL);
            }
            if (look.surveyTool()) {
                ModelParts.box(p, buffer, -2.45F, 7.7F, -3.05F, -1.3F, 12.2F, -2.4F, CERAMIC, STEEL, light);
                ModelParts.box(p, buffer, -2.25F, 8.2F, -3.1F, -1.5F, 9.0F, -3.05F, DARK_STEEL, DARK_STEEL, light);
                ModelParts.box(p, buffer, -2.0F, 6.6F, -2.85F, -1.75F, 7.7F, -2.6F, MINT, MINT, LIGHT_FULL);
            }
            if (look.projector() || look.surveyTool()) {
                ModelParts.box(p, buffer, -4.1F, 9.1F, -2.4F, -1.55F, 10.9F, -2.3F, STEEL, DARK_STEEL, light);
                ModelParts.box(p, buffer, -4.1F, 10.3F, -3.3F, -1.15F, 10.95F, -3.05F, LEATHER_HI, LEATHER, light);
                ModelParts.box(p, buffer, -2.8F, 10.4F, -3.36F, -2.4F, 10.85F, -3.3F, CERAMIC, STEEL, light);
                ModelParts.box(p, buffer, -2.65F, 10.55F, -3.4F, -2.55F, 10.7F, -3.36F, MINT, MINT, light);
            }
            if (look.cableSwapper()) {
                // The Cable Swapper upright on the belt at the back right (the user's crop of the crimper): its steel die
                // plate with the dark slot on the belt, the two mint handles hanging below with their grip ridges
                ModelParts.box(p, buffer, -3.95F, 8.9F, 2.32F, -2.55F, 11.0F, 2.75F, CERAMIC, STEEL, light);
                ModelParts.box(p, buffer, -3.6F, 9.4F, 2.75F, -2.9F, 10.2F, 2.8F, SLOT_DARK, SLOT_DARK, light);
                // A leather loop round the plate, on the belt, under the slot, shut with a steel snap
                ModelParts.box(p, buffer, -4.0F, 10.3F, 2.3F, -2.5F, 10.9F, 2.82F, LEATHER_HI, LEATHER, light);
                ModelParts.box(p, buffer, -3.4F, 10.45F, 2.82F, -3.1F, 10.75F, 2.88F, CERAMIC, STEEL, light);
                for (float x0 : new float[] {-3.9F, -3.15F}) {
                    ModelParts.box(p, buffer, x0, 11.0F, 2.4F, x0 + 0.55F, 13.4F, 2.7F, MINT, MINT, light);
                    ModelParts.box(p, buffer, x0, 11.8F, 2.7F, x0 + 0.55F, 11.95F, 2.74F, TEAL, TEAL, light);
                    ModelParts.box(p, buffer, x0, 12.6F, 2.7F, x0 + 0.55F, 12.75F, 2.74F, TEAL, TEAL, light);
                }
            }
            if (look.drone()) {
                // The magnet plate in the middle of the back, mint bars at its sides; the drone sits on it folded flat
                ModelParts.box(p, buffer, -2.4F, 8.9F, 2.02F, 2.4F, 11.2F, 2.5F, STEEL, DARK_STEEL, light);
                ModelParts.box(p, buffer, 2.15F, 9.2F, 2.5F, 2.4F, 10.9F, 2.56F, stripe, stripe, light);
                ModelParts.box(p, buffer, -2.4F, 9.2F, 2.5F, -2.15F, 10.9F, 2.56F, stripe, stripe, light);
                if (look.droneOut()) {
                    // The empty dock, waiting
                    ModelParts.box(p, buffer, -1.5F, 9.5F, 2.5F, 1.5F, 10.6F, 2.53F, DARK_STEEL, DARK_STEEL, light);
                    ModelParts.box(p, buffer, -0.3F, 9.9F, 2.53F, 0.3F, 10.2F, 2.58F, GLOW, GLOW, LIGHT_FULL);
                } else {
                    ModelParts.box(p, buffer, -1.6F, 8.3F, 2.5F, 1.6F, 11.5F, 3.3F, WHITE, TEAL, light);
                    ModelParts.box(p, buffer, -1.65F, 9.7F, 2.45F, 1.65F, 10.0F, 3.35F, MINT, MINT, LIGHT_FULL);
                    ModelParts.box(p, buffer, -0.4F, 9.0F, 3.3F, 0.4F, 9.4F, 3.4F, GLOW, GLOW, LIGHT_FULL);
                    // The arms folded down its sides
                    ModelParts.box(p, buffer, -2.2F, 8.0F, 2.6F, -1.6F, 11.8F, 3.0F, STEEL, DARK_STEEL, light);
                    ModelParts.box(p, buffer, 1.6F, 8.0F, 2.6F, 2.2F, 11.8F, 3.0F, STEEL, DARK_STEEL, light);
                }
            }
        });
    }

    /** {@code a} to {@code b} by {@code t}, channel by channel. */
    static int mix(int a, int b, float t) {
        int out = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int ca = a >>> shift & 255, cb = b >>> shift & 255;
            out |= Math.round(ca + (cb - ca) * t) << shift;
        }
        return out;
    }

    /** The Energy Sphere's centre and radius, in body pixels (y down): under the belt, in front of the left hip. */
    private static final float SPHERE_X = 2.75F, SPHERE_Y = 13.55F, SPHERE_Z = -3.7F, SPHERE_R = 1.5F;
    /** Its glass: Minecraft's, a pale clear blue with white streaks across the front. */
    private static final int GLASS_TINT = 0x55BFE8EE, GLASS_EDGE = 0x99E8FBFF;

    /**
     * Plasma arcs from the core to the glass, as in a plasma ball: each a zigzag of two thin strokes, flickering to a new
     * place every few ticks. More of them the fuller the sphere.
     */
    /** The arcs' two colours by the sphere's tier: mint, emerald, the Fusion Crystal's violet. */
    private static final int[][] ARC_COLOURS = {{GLOW, MINT}, {GLOW, 0xFF00B894}, {0xFFE6C8FF, 0xFFA45CFF}};

    private static void plasma(PoseStack.Pose p, VertexConsumer buffer, float charge, float time, int tier) {
        int[] colours = ARC_COLOURS[Math.clamp(tier - 1, 0, ARC_COLOURS.length - 1)];
        int arcs = 2 + Math.round(charge * 3);
        int beat = (int) time / 3;
        for (int i = 0; i < arcs; i++) {
            long seed = beat * 31L + i * 977L;
            float yaw = hash(seed) * Mth.TWO_PI, pitch = (hash(seed + 1) - 0.5F) * 2.6F;
            float dx = Mth.cos(pitch) * Mth.cos(yaw), dy = Mth.sin(pitch), dz = Mth.cos(pitch) * Mth.sin(yaw);
            float bend = (hash(seed + 2) - 0.5F) * 0.9F;
            float r = SPHERE_R - 0.12F;
            float[] a = {SPHERE_X, SPHERE_Y, SPHERE_Z};
            float[] m = {SPHERE_X + dx * r * 0.5F + bend * dz, SPHERE_Y + dy * r * 0.5F + bend * 0.5F, SPHERE_Z + dz * r * 0.5F - bend * dx};
            float[] b = {SPHERE_X + dx * r, SPHERE_Y + dy * r, SPHERE_Z + dz * r};
            int colour = colours[i % 2];
            stroke(p, buffer, a, m, 0.12F, colour);
            stroke(p, buffer, m, b, 0.1F, colour);
            // Where it touches the glass, a bright spot
            ModelParts.box(p, buffer, b[0] - 0.13F, b[1] - 0.13F, b[2] - 0.13F, b[0] + 0.13F, b[1] + 0.13F, b[2] + 0.13F, GLOW, GLOW, LIGHT_FULL);
        }
    }

    private static float hash(long seed) {
        long x = seed * 0x9E3779B97F4A7C15L;
        x ^= x >>> 29;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 32;
        return (x & 0xFFFFFF) / (float) 0x1000000;
    }

    /** A thin glowing box from {@code a} to {@code b}. */
    private static void stroke(PoseStack.Pose p, VertexConsumer buffer, float[] a, float[] b, float w, int colour) {
        float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-3F) {
            return;
        }
        PoseStack.Pose turned = p.copy();
        turned.translate(a[0], a[1], a[2]);
        turned.rotate(new Quaternionf().rotationTo(0, 1, 0, dx / length, dy / length, dz / length));
        ModelParts.box(turned, buffer, -w / 2, 0.0F, -w / 2, w / 2, length, w / 2, colour, colour, LIGHT_FULL);
    }

    /** The glass's grid: {@code CELLS} cubes of {@code CELL} px a side, those within the radius filled. */
    private static final int CELLS = 6;
    private static final float CELL = 0.5F, ORB = 3.1F;

    /**
     * The sphere's glass, blocky as everything in Minecraft: half-pixel cubes filling a ball 3 px across, see-through,
     * only its outer faces drawn so the panes do not stack up. A few faces on the front are white, the streaks of
     * Minecraft's glass.
     */
    private static void glass(PoseStack pose, SubmitNodeCollector collector, int light) {
        collector.submitCustomGeometry(pose, ModelParts.SEE_THROUGH, (p, buffer) -> {
            float x0 = SPHERE_X - CELLS * CELL / 2, y0 = SPHERE_Y - CELLS * CELL / 2, z0 = SPHERE_Z - CELLS * CELL / 2;
            for (int x = 0; x < CELLS; x++) {
                for (int y = 0; y < CELLS; y++) {
                    for (int z = 0; z < CELLS; z++) {
                        if (!inOrb(x, y, z)) {
                            continue;
                        }
                        float ax = x0 + x * CELL, ay = y0 + y * CELL, az = z0 + z * CELL;
                        for (Direction side : Direction.values()) {
                            if (!inOrb(x + side.getStepX(), y + side.getStepY(), z + side.getStepZ())) {
                                boolean streak = side == Direction.NORTH && (x - y == 1 && y <= 2 || x - y == 3 && y == 0);
                                face(p, buffer, ax, ay, az, side, streak ? GLASS_EDGE : GLASS_TINT, light);
                            }
                        }
                    }
                }
            }
        });
    }

    static boolean inOrb(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= CELLS || y >= CELLS || z >= CELLS) {
            return false;
        }
        float cx = x - (CELLS - 1) / 2.0F, cy = y - (CELLS - 1) / 2.0F, cz = z - (CELLS - 1) / 2.0F;
        return cx * cx + cy * cy + cz * cz <= ORB * ORB;
    }

    /** One face of the cube at {@code (x, y, z)}, on {@code side}. */
    private static void face(PoseStack.Pose p, VertexConsumer buffer, float x, float y, float z, Direction side, int colour, int light) {
        float x1 = x + CELL, y1 = y + CELL, z1 = z + CELL;
        float nx = side.getStepX(), ny = side.getStepY(), nz = side.getStepZ();
        float[][] c = switch (side) {
            case DOWN -> new float[][] {{x, y, z}, {x1, y, z}, {x1, y, z1}, {x, y, z1}};
            case UP -> new float[][] {{x, y1, z}, {x, y1, z1}, {x1, y1, z1}, {x1, y1, z}};
            case NORTH -> new float[][] {{x, y, z}, {x, y1, z}, {x1, y1, z}, {x1, y, z}};
            case SOUTH -> new float[][] {{x, y, z1}, {x1, y, z1}, {x1, y1, z1}, {x, y1, z1}};
            case WEST -> new float[][] {{x, y, z}, {x, y, z1}, {x, y1, z1}, {x, y1, z}};
            case EAST -> new float[][] {{x1, y, z}, {x1, y1, z}, {x1, y1, z1}, {x1, y, z1}};
        };
        for (float[] v : c) {
            ModelParts.vertex(p, buffer, v[0], v[1], v[2], colour, light, nx, ny, nz);
        }
    }
}
