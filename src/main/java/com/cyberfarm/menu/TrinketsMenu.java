package com.cyberfarm.menu;

import com.cyberfarm.registry.ModMenus;
import com.cyberfarm.trinket.TrinketItem;
import com.cyberfarm.trinket.TrinketType;
import com.cyberfarm.trinket.Trinkets;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Trinkets tab of the inventory: the trinket slots beside the player, and the inventory under them. A menu of its
 * own, so its slots work like any container's, in creative too.
 */
public class TrinketsMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 8, SLOT_Y = 8;
    private static final int TRINKETS = TrinketType.COUNT;
    private static final int INVENTORY_END = TRINKETS + 36;

    public TrinketsMenu(int containerId, Inventory inventory) {
        super(ModMenus.TRINKETS.get(), containerId);
        Trinkets.slots(inventory.player, SLOT_X, SLOT_Y).forEach(this::addSlot);
        addStandardInventorySlots(inventory, 8, 84);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < TRINKETS) {
            if (!moveItemStackTo(stack, TRINKETS, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof TrinketItem trinket && !slots.get(trinket.type().ordinal()).hasItem()) {
            // Shift-click puts a trinket on
            int target = trinket.type().ordinal();
            if (!moveItemStackTo(stack, target, target + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TRINKETS + 27) {
            if (!moveItemStackTo(stack, TRINKETS + 27, INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, TRINKETS, TRINKETS + 27, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, copy);
        } else {
            slot.setChanged();
        }
        return stack.getCount() == copy.getCount() ? ItemStack.EMPTY : copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
