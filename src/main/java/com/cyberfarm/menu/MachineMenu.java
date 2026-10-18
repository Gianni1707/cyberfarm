package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.energy.EnergyUtil;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * Base menu for CyberFarm machines. Machine slots come first, then the player inventory.
 * On the client the handler and data are empty placeholders filled by vanilla sync.
 */
public abstract class MachineMenu extends AbstractContainerMenu {
    protected final ContainerData data;
    private final ContainerLevelAccess access;
    private final Block block;
    private int machineSlotCount;

    protected MachineMenu(MenuType<?> type, int containerId, ContainerData data, ContainerLevelAccess access, Block block) {
        super(type, containerId);
        this.data = data;
        this.access = access;
        this.block = block;
        addDataSlots(data);
    }

    protected void addMachineSlot(ItemStacksResourceHandler handler, int index, int x, int y) {
        addSlot(new ResourceHandlerSlot(handler, handler::set, index, x, y));
        machineSlotCount++;
    }

    protected void addPlayerInventory(Inventory inventory, int top) {
        addStandardInventorySlots(inventory, 8, top);
    }

    public int getEnergy() {
        return EnergyUtil.combine(data.get(0), data.get(1));
    }

    public int getEnergyCapacity() {
        return EnergyUtil.combine(data.get(2), data.get(3));
    }

    /** Machine-specific data, indexed from 0 after the energy slots. */
    protected int extra(int index) {
        return data.get(MachineBlockEntity.ENERGY_DATA_SLOTS + index);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, block);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < machineSlotCount) {
            if (!moveItemStackTo(stack, machineSlotCount, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, machineSlotCount, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }
}
