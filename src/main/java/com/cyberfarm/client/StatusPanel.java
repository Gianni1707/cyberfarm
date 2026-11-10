package com.cyberfarm.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.HiddenFrameBlock;
import com.cyberfarm.block.entity.DockTankBlockEntity;
import com.cyberfarm.block.entity.FluidTankBlockEntity;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.block.entity.MachineStatus;
import com.cyberfarm.client.renderer.ModelParts;
import com.cyberfarm.drone.DroneTask;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.entity.MechEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.cyberfarm.port.client.GuiGraphicsExtractor;
import com.cyberfarm.port.client.ScreenProjection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.jspecify.annotations.Nullable;

/**
 * A small panel just over what the player looks at, following it on screen. Over a CyberFarm machine: whether it works,
 * the FE it makes and uses, its buffer and how far its batch is, unless a tooltip mod (Jade, WTHIT) already covers machines.
 * Over a tank: what is in it and how full. Over a drone, from further away: what it is doing, its charge and what it carries. Drawn on the HUD at a point worked out
 * from the world, so it stays sharp and the same size, and nothing in the world can cover it.
 */
public final class StatusPanel {
    private static final ResourceLocation LAYER = CyberFarm.id("status_panel");
    private static final double GAP = 0.2;
    private static final double DRONE_REACH = 32.0;
    private static final int CARGO_ROWS = 4;
    private static final int LINE = 10;
    private static final int BAR = 4;
    private static final int MIN_WIDTH = 80;
    private static final boolean TOOLTIP_MOD = ModList.get().isLoaded("jade") || ModList.get().isLoaded("wthit") || ModList.get().isLoaded("waila");

    private static final int MARK = 0xFF00B894;
    private static final int MARK_FAINT = 0x3000B894;
    private static final int GREEN = 0xFF6CFF6C;
    private static final int AMBER = 0xFFFFB347;
    private static final int GREY = 0xFF8A9AA6;
    private static final int TEXT = 0xFFD8F8FF;
    private static final int BACKGROUND = 0xB0101820;

    /** A line of text and, with {@code bar} at 0 or more, a bar filled that far under it. */
    private record Row(Component text, int colour, float bar, int barColour) {
        Row(Component text, int colour) {
            this(text, colour, -1.0F, 0);
        }
    }

    private record Panel(Vec3 at, List<Row> rows) {}

    private static @Nullable DroneEntity lookedAtDrone;
    private static long lookedAtTick = -1;

    private StatusPanel() {}

    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), LAYER.getPath(),
                (gui, graphics, partialTick, width, height) -> render(new GuiGraphicsExtractor(graphics), partialTick));
    }

    private static void render(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.level == null || minecraft.player == null
                || minecraft.player.getVehicle() instanceof MechEntity) {
            return;
        }
        Panel panel = null;
        DroneEntity drone = lookedAtDrone(minecraft);
        if (drone != null) {
            panel = new Panel(drone.getPosition(partialTick).add(0.0, drone.getBbHeight() + GAP, 0.0), droneRows(drone));
        } else if (!TOOLTIP_MOD && minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            panel = machinePanel(minecraft.level, hit.getBlockPos());
        }
        if (panel != null) {
            draw(graphics, minecraft, panel);
        }
    }

    /** The drone in the crosshair, with no block in between; looked for once a tick, as it reaches far. */
    private static @Nullable DroneEntity lookedAtDrone(Minecraft minecraft) {
        long now = minecraft.level.getGameTime();
        if (now == lookedAtTick) {
            return lookedAtDrone != null && lookedAtDrone.isAlive() ? lookedAtDrone : null;
        }
        lookedAtTick = now;
        lookedAtDrone = null;
        Entity viewer = minecraft.getCameraEntity();
        if (viewer == null) {
            return null;
        }
        Vec3 from = viewer.getEyePosition();
        Vec3 to = from.add(viewer.getViewVector(1.0F).scale(DRONE_REACH));
        HitResult blocks = minecraft.level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, viewer));
        Vec3 end = blocks.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(viewer, from, end, viewer.getBoundingBox().expandTowards(end.subtract(from)).inflate(1.0),
                entity -> entity instanceof DroneEntity, from.distanceToSqr(end));
        lookedAtDrone = hit != null ? (DroneEntity) hit.getEntity() : null;
        return lookedAtDrone;
    }

    private static @Nullable Panel machinePanel(Level level, BlockPos looked) {
        BlockState state = level.getBlockState(looked);
        BlockPos pos = state.getBlock() instanceof HiddenFrameBlock frame ? frame.machine(level, looked, state) : looked;
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof DockTankBlockEntity tank) {
            return tankPanel(level, state.getBlock().getName(), tank.tank().fluid(), tank.tank().amount(), tank.tank().capacity(),
                    new Vec3(pos.getX() + 0.5, pos.getY() + 1.0 + GAP, pos.getZ() + 0.5));
        }
        if (entity instanceof FluidTankBlockEntity cell && cell.runner() != null) {
            // A tank of several blocks shows one panel, over its middle
            FluidTankBlockEntity runner = cell.runner();
            BlockPos low = runner.boxMin();
            BlockPos high = runner.boxMax();
            return tankPanel(level, state.getBlock().getName(), runner.shownFluid(), runner.shownAmount(), runner.shownCapacity(),
                    new Vec3((low.getX() + high.getX() + 1) / 2.0, high.getY() + 1.0 + GAP, (low.getZ() + high.getZ() + 1) / 2.0));
        }
        if (!(entity instanceof MachineBlockEntity machine)) {
            return null;
        }
        MachineStatus status = machine.status;
        // Nothing to say about a machine with no buffer, no batches and nothing going on, such as a tank
        if (status.capacity() == 0 && status.progress() < 0 && !status.working()) {
            return null;
        }
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(machine.getDisplayName(), TEXT));
        rows.add(new Row(Component.translatable(status.working() ? "hud.cyberfarm.machine.working" : "hud.cyberfarm.machine.idle"), status.working() ? MARK : GREY));
        if (status.made() > 0) {
            rows.add(new Row(Component.translatable("hud.cyberfarm.machine.made", rate(status.made())), GREEN));
        }
        if (status.used() > 0) {
            rows.add(new Row(Component.translatable("hud.cyberfarm.machine.used", rate(status.used())), AMBER));
        }
        if (status.capacity() > 0) {
            rows.add(new Row(Component.translatable("gui.cyberfarm.energy", String.format("%,d", status.energy()), String.format("%,d", status.capacity())),
                    TEXT, (float) status.energy() / status.capacity(), MARK));
        }
        if (status.progress() >= 0) {
            rows.add(new Row(Component.translatable("hud.cyberfarm.machine.progress", Math.round(status.progress() * 100)), TEXT, status.progress(), status.working() ? MARK : GREY));
        }
        // Over the machine's top: the frames piled on it are part of it
        BlockPos.MutableBlockPos top = pos.mutable().move(0, 1, 0);
        while (level.getBlockState(top).getBlock() instanceof HiddenFrameBlock) {
            top.move(0, 1, 0);
        }
        return new Panel(new Vec3(pos.getX() + 0.5, top.getY() + GAP, pos.getZ() + 0.5), rows);
    }

    private static Panel tankPanel(Level level, Component name, Fluid fluid, int amount, int capacity, Vec3 at) {
        Component contents = amount > 0 ? fluid.getFluidType().getDescription() : Component.translatable("gui.cyberfarm.tank.empty");
        return new Panel(at, List.of(new Row(name, TEXT),
                new Row(Component.translatable("gui.cyberfarm.tank", contents, String.format("%,d", amount), String.format("%,d", capacity)),
                        amount > 0 ? TEXT : GREY, (float) amount / capacity, 0xFF000000 | ModelParts.liquidColour(level, amount > 0 ? fluid : Fluids.EMPTY))));
    }

    private static List<Row> droneRows(DroneEntity drone) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(drone.label(), TEXT));
        int status = drone.getStatus();
        DroneTask task = drone.getJob();
        rows.add(status == DroneEntity.STATUS_WORKING && task != null
                ? new Row(Component.translatable("gui.cyberfarm.dock.work." + task.name().toLowerCase(Locale.ROOT)), MARK)
                : new Row(Component.translatable("gui.cyberfarm.dock.status." + switch (status) {
                    case DroneEntity.STATUS_CHARGING -> "charging";
                    case DroneEntity.STATUS_RETURNING -> "returning";
                    case DroneEntity.STATUS_WORKING -> "working";
                    default -> "idle";
                }), status == DroneEntity.STATUS_IDLE ? GREY : AMBER));
        int battery = drone.getBatteryPercent();
        rows.add(new Row(Component.translatable("hud.cyberfarm.drone.battery", battery), TEXT, battery / 100.0F, battery < 25 ? AMBER : MARK));
        if (drone.getLiquidAmount() > 0) {
            rows.add(new Row(Component.translatable("hud.cyberfarm.drone.liquid", String.format("%,d", drone.getLiquidAmount()), drone.getLiquidShown().getFluidType().getDescription()), TEXT));
            if (drone.isWaitingForTank()) {
                rows.add(new Row(Component.translatable("hud.cyberfarm.drone.tank_full"), AMBER));
            }
        }
        List<ItemStack> cargo = drone.getCargoShown();
        if (cargo.isEmpty() && drone.getLiquidAmount() == 0) {
            rows.add(new Row(Component.translatable("hud.cyberfarm.drone.no_cargo"), GREY));
        }
        for (ItemStack stack : cargo.subList(0, Math.min(cargo.size(), CARGO_ROWS))) {
            rows.add(new Row(Component.translatable("hud.cyberfarm.drone.cargo", stack.getCount(), stack.getHoverName()), TEXT));
        }
        if (cargo.size() > CARGO_ROWS) {
            rows.add(new Row(Component.translatable("hud.cyberfarm.drone.more_cargo", cargo.size() - CARGO_ROWS), GREY));
        }
        return rows;
    }

    private static void draw(GuiGraphicsExtractor graphics, Minecraft minecraft, Panel panel) {
        // Behind the camera the projection would mirror it onto the screen
        Entity viewer = minecraft.getCameraEntity();
        if (viewer == null || panel.at().subtract(viewer.getEyePosition()).dot(viewer.getViewVector(1.0F)) <= 0.0) {
            return;
        }
        Font font = minecraft.font;
        int width = MIN_WIDTH;
        int height = 0;
        for (Row row : panel.rows()) {
            width = Math.max(width, font.width(row.text()));
            height += LINE + (row.bar() >= 0 ? BAR : 0);
        }
        // Two screen pixels per unit (three on very large GUI scales) whatever the GUI scale: small, and its text stays sharp
        double guiScale = minecraft.getWindow().getGuiScale();
        float scale = (float) Math.min(1.0, Math.max(2.0, Math.ceil(guiScale / 2)) / guiScale);
        Vec3 point = ScreenProjection.project(panel.at());
        float screenX = (float) (point.x + 1.0) / 2.0F * graphics.guiWidth();
        float screenY = (float) (1.0 - point.y) / 2.0F * graphics.guiHeight();
        // Sits on the point, centred, growing upwards, and never leaves the screen
        float halfWidth = (width + 8) * scale / 2.0F;
        float left = Mth.clamp(screenX - halfWidth, 2.0F, Math.max(2.0F, graphics.guiWidth() - 2.0F - 2.0F * halfWidth));
        float top = Mth.clamp(screenY - (height + 6) * scale, 2.0F, Math.max(2.0F, graphics.guiHeight() - 2.0F - (height + 6) * scale));

        graphics.pose().pushMatrix();
        graphics.pose().translate(left + 4 * scale, top + 4 * scale);
        graphics.pose().scale(scale);
        graphics.fill(-4, -4, width + 4, height + 2, BACKGROUND);
        graphics.fill(-4, -4, -3, height + 2, MARK);
        int y = 0;
        for (Row row : panel.rows()) {
            graphics.text(font, row.text(), 0, y, row.colour(), true);
            y += LINE;
            if (row.bar() >= 0) {
                graphics.fill(0, y - 1, width, y + 2, MARK_FAINT);
                graphics.fill(0, y - 1, Math.round(width * Mth.clamp(row.bar(), 0.0F, 1.0F)), y + 2, row.barColour());
                y += BAR;
            }
        }
        graphics.pose().popMatrix();
    }

    /** FE per tick: one decimal while small, whole numbers above ten. */
    private static String rate(float perTick) {
        return perTick >= 10 || perTick == Math.round(perTick) ? String.format("%,d", Math.round(perTick)) : String.format("%.1f", perTick);
    }
}
