package com.cyberfarm.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cyberfarm.item.ProjectorItem;
import com.cyberfarm.multiblock.Blueprint;
import com.cyberfarm.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * Draws the Multiblock Projector's ghost blocks: translucent copies of the blocks still to place, and
 * red ones where a block is wrong or must be removed. Correct blocks are not drawn.
 */
public final class ProjectionRenderer {
    private static final int MAX_DISTANCE = 64;
    private static final int RED = 0xC0FF3030;
    private static final Map<Block, RenderType> TYPES = new HashMap<>();

    private record Ghost(BlockPos pos, Block block, boolean wrong) {}

    // shortcut: one projection per client, kept in a static between extract and submit; fine for a single local player
    private static List<Ghost> ghosts = List.of();
    private static long lastMessage;

    private ProjectionRenderer() {}

    public static void extract(ExtractLevelRenderStateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ghosts = List.of();
        if (player == null) {
            return;
        }
        ItemStack stack = held(player);
        if (stack == null) {
            return;
        }
        Level level = event.getLevel();
        Blueprint blueprint = ProjectorItem.blueprint(stack);
        ProjectorItem.Placement placement = ProjectorItem.pinned(stack);
        if (placement == null && minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            // Not pinned: the projection follows the block the player looks at
            placement = ProjectorItem.placement(level, hit.getBlockPos(), hit.getDirection(), player, blueprint);
        }
        if (placement == null) {
            return;
        }
        BlockPos controller = placement.controller();
        Direction facing = placement.facing();
        if (controller.distSqr(player.blockPosition()) > MAX_DISTANCE * MAX_DISTANCE) {
            return;
        }
        List<Ghost> found = new ArrayList<>();
        int missing = 0;
        int wrong = 0;
        for (Blueprint.Ghost ghost : blueprint.ghosts(controller, facing)) {
            var state = level.getBlockState(ghost.pos());
            if (ghost.ok().test(state)) {
                continue;
            }
            if (ghost.show() == null) {
                // Must be empty: show what is in the way, in red
                found.add(new Ghost(ghost.pos(), state.getBlock(), true));
                wrong++;
            } else if (state.isAir() || state.canBeReplaced()) {
                found.add(new Ghost(ghost.pos(), ghost.show(), false));
                missing++;
            } else {
                found.add(new Ghost(ghost.pos(), ghost.show(), true));
                wrong++;
            }
        }
        ghosts = found;
        long time = level.getGameTime();
        if (time - lastMessage >= 10 || time < lastMessage) {
            lastMessage = time;
            Component name = ProjectorItem.name(blueprint);
            player.sendOverlayMessage(missing + wrong == 0
                    ? Component.translatable("message.cyberfarm.projector.complete", name)
                    : Component.translatable("message.cyberfarm.projector.status", name, missing, wrong));
        }
    }

    private static ItemStack held(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof ProjectorItem) {
                return stack;
            }
        }
        return null;
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        if (ghosts.isEmpty()) {
            return;
        }
        List<Ghost> toDraw = ghosts;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        // Gentle pulse so the ghosts read as a projection, not as real blocks
        float pulse = 0.5F + 0.15F * (float) Math.sin(System.currentTimeMillis() / 300.0);
        int alpha = (int) (pulse * 255) << 24;
        Map<Block, List<Ghost>> byBlock = new HashMap<>();
        for (Ghost ghost : toDraw) {
            byBlock.computeIfAbsent(ghost.block(), b -> new ArrayList<>()).add(ghost);
        }
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        byBlock.forEach((block, list) -> event.getSubmitNodeCollector().submitCustomGeometry(poseStack, type(block), (pose, buffer) -> {
            for (Ghost ghost : list) {
                BlockPos pos = ghost.pos();
                float inset = ghost.wrong() ? -0.01F : 0.15F;
                int color = ghost.wrong() ? RED : alpha | 0xFFFFFF;
                cube(pose, buffer, pos.getX() + inset, pos.getY() + inset, pos.getZ() + inset, 1 - 2 * inset, color);
            }
        }));
        poseStack.popPose();
    }

    private static RenderType type(Block block) {
        return TYPES.computeIfAbsent(block, b -> RenderTypes.entityTranslucent(texture(b)));
    }

    /** The block's main texture: CyberFarm blocks by name, a few vanilla ones the structures use. */
    private static Identifier texture(Block block) {
        if (block == ModBlocks.METHANE_EXTRACTOR.get()) {
            return Identifier.fromNamespaceAndPath("cyberfarm", "textures/block/methane_extractor_side.png");
        }
        Identifier key = BuiltInRegistries.BLOCK.getKey(block);
        if (key.getNamespace().equals("cyberfarm") || key.getNamespace().equals("minecraft") && key.getPath().endsWith("glass")) {
            return key.withPath(path -> "textures/block/" + path + ".png");
        }
        // Anything else in the way: a red frame is enough
        return Identifier.fromNamespaceAndPath("cyberfarm", "textures/block/cyber_frame.png");
    }

    private static void cube(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float size, int color) {
        float x1 = x + size;
        float y1 = y + size;
        float z1 = z + size;
        face(pose, buffer, color, 0, -1, 0, x, y, z, x1, y, z, x1, y, z1, x, y, z1);
        face(pose, buffer, color, 0, 1, 0, x, y1, z1, x1, y1, z1, x1, y1, z, x, y1, z);
        face(pose, buffer, color, 0, 0, -1, x1, y1, z, x1, y, z, x, y, z, x, y1, z);
        face(pose, buffer, color, 0, 0, 1, x, y1, z1, x, y, z1, x1, y, z1, x1, y1, z1);
        face(pose, buffer, color, -1, 0, 0, x, y1, z, x, y, z, x, y, z1, x, y1, z1);
        face(pose, buffer, color, 1, 0, 0, x1, y1, z1, x1, y, z1, x1, y, z, x1, y1, z);
    }

    private static void face(PoseStack.Pose pose, VertexConsumer buffer, int color, float nx, float ny, float nz, float... c) {
        float[][] uv = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        for (int i = 0; i < 4; i++) {
            buffer.addVertex(pose, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]).setColor(color).setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, nx, ny, nz);
        }
    }
}
