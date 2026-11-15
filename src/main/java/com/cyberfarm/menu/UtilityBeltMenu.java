package com.cyberfarm.menu;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.registry.ModMenus;
import com.cyberfarm.trinket.UtilityBeltItem;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Utility Belt's holders, opened with its key: Energy Sphere, Multiblock Projector, Survey Tool, Pocket Drone. The
 * slots read and write the belt the player wears, so what goes in shows on the belt at once.
 */
public class UtilityBeltMenu extends AbstractContainerMenu {
    /** Where the four holders sit on the screen, left to right. */
    public static final int SLOT_X = 44, SLOT_Y = 35, SLOT_PITCH = 26;
    private static final int HOLDERS = UtilityBeltItem.SLOTS;
    private static final String[] ICONS = {"belt_sphere", "belt_projector", "belt_survey_tool", "belt_drone"};

    public UtilityBeltMenu(int containerId, Inventory inventory) {
        super(ModMenus.UTILITY_BELT.get(), containerId);
        BeltContents belt = new BeltContents(inventory.player);
        for (int i = 0; i < HOLDERS; i++) {
            addSlot(new Holder(belt, i));
        }
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
        if (index < HOLDERS) {
            if (!moveItemStackTo(stack, HOLDERS, HOLDERS + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int holder = -1;
            for (int i = 0; i < HOLDERS && holder < 0; i++) {
                if (UtilityBeltItem.fits(i, stack) && !slots.get(i).hasItem()) {
                    holder = i;
                }
            }
            if (holder < 0 || !moveItemStackTo(stack, holder, holder + 1, false)) {
                return ItemStack.EMPTY;
            }
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
        return !UtilityBeltItem.worn(player).isEmpty();
    }

    private static final class Holder extends Slot {
        private final int holder;

        Holder(Container container, int holder) {
            super(container, holder, SLOT_X + holder * SLOT_PITCH, SLOT_Y);
            this.holder = holder;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return UtilityBeltItem.fits(holder, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public Identifier getNoItemIcon() {
            return CyberFarm.id("container/slot/" + ICONS[holder]);
        }
    }

    /** The worn belt's holders seen as a container. */
    private record BeltContents(Player player) implements Container {
        @Override
        public int getContainerSize() {
            return HOLDERS;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < HOLDERS; i++) {
                if (!getItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return UtilityBeltItem.content(player, slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty() || count <= 0) {
                return ItemStack.EMPTY;
            }
            setItem(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return removeItem(slot, 1);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            UtilityBeltItem.update(player, slot, old -> stack);
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(Player user) {
            return user == player;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < HOLDERS; i++) {
                setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
