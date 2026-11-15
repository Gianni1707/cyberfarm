package com.cyberfarm.menu;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.block.entity.TrinketTableBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;
import com.cyberfarm.trinket.TrinketRecipes;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import com.cyberfarm.port.PortMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Trinket Table's grid, laid out like the crafting table's: 3x3, the result on the right, the inventory under them.
 * The result shows as soon as the grid makes something; taking it uses one item from each filled slot. What is left
 * in the grid goes back to the player when the screen closes.
 */
public class TrinketTableMenu extends PortMenu {
    private static final int RESULT = 0, GRID_START = 1, GRID_END = 10, INVENTORY_END = 46;

    private final ContainerLevelAccess access;
    private final Player player;
    private final SimpleContainer grid = new SimpleContainer(9) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();

    public TrinketTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public TrinketTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.TRINKET_TABLE.get(), containerId);
        this.access = access;
        this.player = inventory.player;
        addSlot(new ResultSlot(result, 0, 124, 35));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(grid, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }
        addStandardInventorySlots(inventory, 8, 84);
    }

    private List<ItemStack> gridItems() {
        List<ItemStack> items = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            items.add(grid.getItem(i));
        }
        return items;
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == grid) {
            result.setItem(0, TrinketRecipes.result(gridItems()));
            broadcastChanges();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player user, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == RESULT) {
            // Into the inventory, the hotbar last, as the crafting table does
            if (!moveItemStackTo(stack, GRID_END, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (index < GRID_END) {
            if (!moveItemStackTo(stack, GRID_END, INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, GRID_START, GRID_END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(user, stack);
        return copy;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(carried, slot);
    }

    @Override
    public void removed(Player user) {
        super.removed(user);
        access.execute((level, pos) -> clearContainer(user, grid));
    }

    @Override
    public boolean stillValid(Player user) {
        return stillValid(access, user, ModBlocks.TRINKET_TABLE.get());
    }

    /** The result: taking it uses one item from each filled slot of the grid and sets the arms going. */
    private final class ResultSlot extends Slot {
        ResultSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player user, ItemStack carried) {
            for (int i = 0; i < 9; i++) {
                if (!grid.getItem(i).isEmpty()) {
                    grid.removeItem(i, 1);
                }
            }
            access.execute((level, pos) -> {
                if (level.getBlockEntity(pos) instanceof TrinketTableBlockEntity table) {
                    table.crafted();
                }
            });
            super.onTake(user, carried);
        }
    }
}
