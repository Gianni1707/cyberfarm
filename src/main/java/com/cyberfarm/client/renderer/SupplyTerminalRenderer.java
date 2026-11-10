package com.cyberfarm.client.renderer;

import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.entity.SupplyTerminalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.cyberfarm.port.client.VertexConsumer;

import com.cyberfarm.port.client.SubmitNodeCollector;
import com.cyberfarm.port.client.BlockEntityRenderer;
import com.cyberfarm.port.client.BlockEntityRenderState;
import com.cyberfarm.port.client.ModelFeatureRenderer;
import com.cyberfarm.port.client.CameraRenderState;
import net.minecraft.core.Direction;
import com.cyberfarm.port.client.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Supply Terminal's board, showing the village's real contracts: a lit card per open contract with how much is
 * delivered, the event's in steel (blinking in a raid). A finished contract's card drops behind the board's lip, and
 * a new one rises. The header is lit while a mechanist works it.
 * <p>
 * Up to 14 boxes, 84 quads per terminal, within the default 64 blocks; the model alone shows the dim cards further off.
 * Coordinates are model pixels of the north-facing model (tools/machinemodels.py, supply_terminal_parts()).
 */
public class SupplyTerminalRenderer extends BlockEntityRenderer<SupplyTerminalBlockEntity, SupplyTerminalRenderer.State> {
    /** The places' left edges: three ordinary ones, then the event's. */
    private static final float[] PLACE_X = {1.2F, 4.6F, 8.0F, 12.1F};
    private static final float CARD_W = 2.5F, CARD_H = 3.8F, CARD_Y = 10.0F, DROP = 3.8F, EVENT_CAP = 0.9F;
    private static final float CARD_Z0 = 16.0F - 15.3F, CARD_Z1 = 16.0F - 15.22F;
    private static final float FILL_TICKS = 10.0F, BLINK_TICKS = 10.0F;
    private static final int MINT = 0xFF5EC2B8, STEEL_TOP = 0xFFBDC6C5, STEEL = 0xFFB1BEBC, SLOT = 0xFF030909, LAMP = 0xFFD1FEE7, LAMP_OFF = 0xFF055B57;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        int light;
        boolean header;
        /** For each place: the kind of its card (0 none), how far it has sunk, how full it is. */
        final int[] kind = new int[SupplyTerminalBlockEntity.CONTRACTS];
        final float[] sunk = new float[SupplyTerminalBlockEntity.CONTRACTS];
        final float[] fill = new float[SupplyTerminalBlockEntity.CONTRACTS];
        boolean blink;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SupplyTerminalBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        if (level == null) {
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos().relative(state.facing));
        float time = level.getGameTime() + partialTick;
        state.header = entity.boardHasMechanist();
        state.blink = time % (2.0F * BLINK_TICKS) < BLINK_TICKS;
        for (int i = 0; i < SupplyTerminalBlockEntity.CONTRACTS; i++) {
            entity.cards[i].update(entity.boardKind(i), entity.boardFill(i), time);
            state.kind[i] = entity.cards[i].kind;
            float move = ease((time - entity.cards[i].moveStart) / SupplyTerminalBlockEntity.Card.MOVE_TICKS);
            state.sunk[i] = switch (entity.cards[i].motion) {
                case SupplyTerminalBlockEntity.Card.DROPPING -> DROP * move;
                case SupplyTerminalBlockEntity.Card.RISING -> DROP * (1.0F - move);
                default -> 0.0F;
            };
            float grown = ease((time - entity.cards[i].fillStart) / FILL_TICKS);
            state.fill[i] = Mth.lerp(grown, entity.cards[i].fillFrom, entity.cards[i].fillTo) / SupplyTerminalBlockEntity.FILL_STEPS;
        }
    }

    private static float ease(float x) {
        float t = Mth.clamp(x, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            if (state.header) {
                ModelParts.box(pose, buffer, 1.4F, 14.65F, 16.0F - 15.68F, 14.6F, 15.0F, 16.0F - 15.64F, LAMP, LAMP, LightCoordsUtil.FULL_BRIGHT);
            }
            for (int i = 0; i < PLACE_X.length; i++) {
                if (state.kind[i] != 0) {
                    card(pose, buffer, PLACE_X[i], CARD_Y - state.sunk[i], state.kind[i], state.fill[i], state);
                }
            }
        });
        poseStack.popPose();
    }

    /** A card with its dark window filling up; an event's in steel, with a light on top. */
    private static void card(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, int kind, float fill, State state) {
        boolean event = kind != SupplyTerminalBlockEntity.CARD_NORMAL;
        int light = state.light;
        float windowTop = y + CARD_H - 0.4F - (event ? EVENT_CAP : 0.0F);
        ModelParts.box(pose, buffer, x, y, CARD_Z0, x + CARD_W, y + CARD_H, CARD_Z1, event ? STEEL_TOP : MINT, event ? STEEL : MINT, light);
        ModelParts.box(pose, buffer, x + 0.5F, y + 0.4F, CARD_Z0 - 0.03F, x + CARD_W - 0.5F, windowTop, CARD_Z0, SLOT, SLOT, light);
        if (fill > 0.02F) {
            ModelParts.box(pose, buffer, x + 0.5F, y + 0.4F, CARD_Z0 - 0.05F, x + CARD_W - 0.5F, Mth.lerp(fill, y + 0.4F, windowTop), CARD_Z0 - 0.03F,
                    LAMP, LAMP, LightCoordsUtil.FULL_BRIGHT);
        }
        if (event) {
            boolean lit = kind == SupplyTerminalBlockEntity.CARD_MARKET || state.blink;
            ModelParts.box(pose, buffer, x + 0.2F, y + CARD_H - 0.7F, CARD_Z0 - 0.05F, x + CARD_W - 0.2F, y + CARD_H - 0.2F, CARD_Z0,
                    lit ? LAMP : LAMP_OFF, lit ? LAMP : LAMP_OFF, lit ? LightCoordsUtil.FULL_BRIGHT : light);
        }
    }

    @Override
    public AABB getRenderBoundingBox(SupplyTerminalBlockEntity entity) {
        return new AABB(entity.getBlockPos());
    }
}
