package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.energy.EnergyUtil;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
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
    /** The addon slots sit on a tab outside the right edge of the 176 px wide GUI. */
    public static final int ADDON_X = 182;
    public static final int ADDON_Y = 20;
    public static final int ADDON_PITCH = 24;

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
        if (hasAddons()) {
            // The machine's own addons on the server; on the client a placeholder that vanilla sync fills
            ItemStacksResourceHandler addons = access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof MachineBlockEntity machine ? machine.getAddons() : null)
                    .orElseGet(() -> new ItemStacksResourceHandler(MachineBlockEntity.ADDON_SLOTS));
            for (int i = 0; i < MachineBlockEntity.ADDON_SLOTS; i++) {
                addMachineSlot(addons, i, ADDON_X, ADDON_Y + i * ADDON_PITCH);
            }
        }
        addStandardInventorySlots(inventory, 8, top);
    }

    /** Whether the machine takes Speed, Efficiency and Battery addons; the same on server and client. */
    public boolean hasAddons() {
        return false;
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

    /**
     * The client's machine slots are placeholders that take anything, so it would show an item going into a slot that
     * refuses it until the server took it back: anything going into a machine slot waits for the server's word.
     */
    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (player.level().isClientSide() && slotId >= 0 && slotId < machineSlotCount && (input == ContainerInput.SWAP || !getCarried().isEmpty())) {
            return;
        }
        super.clicked(slotId, button, input, player);
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
        } else if (player.level().isClientSide() || !moveItemStackTo(stack, 0, machineSlotCount, false)) {
            // On the client, see clicked: only the server knows what goes in, and tells us
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
