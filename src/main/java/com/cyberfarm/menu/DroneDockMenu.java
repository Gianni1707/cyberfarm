package com.cyberfarm.menu;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.drone.DroneTask;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

public class DroneDockMenu extends MachineMenu {
    public static final int CARGO_X = 98;
    public static final int CARGO_Y = 17;
    public static final int INVENTORY_Y = 104;
    public static final int BUTTON_FILTER = 0;
    public static final int BUTTON_PAUSE = 1;

    private final ContainerLevelAccess access;

    public DroneDockMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStacksResourceHandler(DroneDockBlockEntity.CARGO_SLOTS),
                new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + DroneDockBlockEntity.EXTRA_DATA), ContainerLevelAccess.NULL,
                ModBlocks.DRONE_DOCK.get());
    }

    /** {@code block} is the dock's own block, a Drone Dock or a Builder Swarm Dock, so the menu stays open on either. */
    public DroneDockMenu(int containerId, Inventory inventory, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access, Block block) {
        super(ModMenus.DRONE_DOCK.get(), containerId, data, access, block);
        this.access = access;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addMachineSlot(items, row * 3 + col, CARGO_X + col * 18, CARGO_Y + row * 18);
            }
        }
        addPlayerInventory(inventory, INVENTORY_Y);
    }

    /** The Filter button swaps this menu for the dock's filter; Return/Resume holds or frees the drone. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_FILTER && id != BUTTON_PAUSE) {
            return false;
        }
        access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof DroneDockBlockEntity dock) {
                if (id == BUTTON_FILTER) {
                    dock.openFilter(player);
                } else {
                    dock.togglePaused();
                }
            }
        });
        return true;
    }

    public int getAreaWidth() {
        return extra(0);
    }

    public int getAreaDepth() {
        return extra(1);
    }

    public int getDroneStatus() {
        return extra(2);
    }

    public int getDroneEnergyPercent() {
        return extra(3);
    }

    public boolean hasTransportRoute() {
        return extra(4) != 0;
    }

    public boolean isQuarryStarted() {
        return extra(5) != 0;
    }

    public int getQuarryLayer() {
        return extra(6);
    }

    /** The job the drone is doing, or null when it has none. */
    public @Nullable DroneTask getDroneTask() {
        int task = extra(7);
        return task > 0 && task <= DroneTask.values().length ? DroneTask.values()[task - 1] : null;
    }

    public int getDroneModules() {
        return extra(8);
    }

    public BlockPos getDronePos() {
        return new BlockPos(combined(9), combined(11), combined(13));
    }

    public int getDroneEnergy() {
        return combined(15);
    }

    public int getDroneCapacity() {
        return combined(17);
    }

    public boolean isPaused() {
        return extra(19) != 0;
    }

    public boolean hasTaxiDestination() {
        return extra(20) != 0;
    }

    public boolean hasBuild() {
        return extra(21) != 0;
    }

    public int getBuildRemaining() {
        return extra(22);
    }

    public int getBuildObstacles() {
        return extra(23);
    }

    /** The layer being built, counting from 1 at the bottom; 0 when nothing is left to place. */
    public int getBuildLayer() {
        return extra(24);
    }

    public int getBuildLayers() {
        return extra(25);
    }

    /** {@code inDock}: whether the dock has any of it to give the drones. */
    public record MissingBlock(Item item, int count, boolean inDock) {}

    /**
     * The blocks the current layer still needs (up to {@link DroneDockBlockEntity#BUILD_LIST} kinds);
     * with no layer left for the drones, the blocks left for the player.
     */
    public List<MissingBlock> getBuildMissing() {
        List<MissingBlock> missing = new ArrayList<>();
        for (int i = 0; i < DroneDockBlockEntity.BUILD_LIST; i++) {
            int item = combined(27 + 3 * i) - 1;
            if (item >= 0) {
                int count = extra(29 + 3 * i);
                missing.add(new MissingBlock(BuiltInRegistries.ITEM.byId(item), Math.abs(count), count > 0));
            }
        }
        return missing;
    }

    public int getDroneCount() {
        return extra(57);
    }

    public int getMaxDrones() {
        return extra(58);
    }

    /** Kinds of block the list leaves out. */
    public int getBuildMissingMore() {
        return extra(26);
    }

    /** An int sent as two 16-bit halves, starting at {@code index}. */
    private int combined(int index) {
        return EnergyUtil.combine(extra(index), extra(index + 1));
    }
}
