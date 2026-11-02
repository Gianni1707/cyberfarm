package com.cyberfarm.port;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * A Forge ItemStackHandler with the 26.x helpers the machines use (resources, set, size), and the
 * 26.x override points: {@link #isValid}, {@link #getCapacity} and {@link #onContentsChanged(int, ItemStack)}.
 */
public class ItemStacksResourceHandler extends ItemStackHandler {
    public ItemStacksResourceHandler(int size) {
        super(size);
    }

    public int size() {
        return getSlots();
    }

    public ItemResource getResource(int index) {
        return ItemResource.of(getStackInSlot(index));
    }

    public int getAmountAsInt(int index) {
        return getStackInSlot(index).getCount();
    }

    public long getAmountAsLong(int index) {
        return getStackInSlot(index).getCount();
    }

    public ItemStack getItem(int index) {
        return getStackInSlot(index);
    }

    public void set(int index, ItemResource resource, int amount) {
        setStackInSlot(index, resource.toStack(amount));
    }

    public void setItem(int index, ItemStack stack) {
        setStackInSlot(index, stack);
    }

    /** Whether automation and players may put this item into the slot. */
    public boolean isValid(int index, ItemResource resource) {
        return true;
    }

    /** How many of {@code resource} the slot holds at most (the stack size caps it too). */
    protected int getCapacity(int index, ItemResource resource) {
        return 99;
    }

    protected void onContentsChanged(int index, ItemStack previousContents) {}

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isValid(slot, ItemResource.of(stack));
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(stack.getMaxStackSize(), getCapacity(slot, ItemResource.of(stack)));
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        ItemStack previous = stacks.get(slot);
        stacks.set(slot, stack);
        onContentsChanged(slot, previous);
    }

    @Override
    protected void onContentsChanged(int slot) {
        onContentsChanged(slot, ItemStack.EMPTY);
    }

    public void serialize(ValueOutput output) {
        output.tag().merge(serializeNBT());
    }

    public void deserialize(ValueInput input) {
        if (input.tag().contains("Size")) {
            deserializeNBT(input.tag());
        }
    }
}
