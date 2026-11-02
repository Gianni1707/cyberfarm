package com.cyberfarm.port;

import java.util.function.IntPredicate;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * What automation sees of a machine inventory: the same slots, but only the ones {@code canInsert}
 * allows take items in and only the ones {@code canExtract} allows give them out.
 */
public class SlotRulesItemHandler implements IItemHandler {
    private final IItemHandler inner;
    private final IntPredicate canInsert;
    private final IntPredicate canExtract;

    public SlotRulesItemHandler(IItemHandler inner, IntPredicate canInsert, IntPredicate canExtract) {
        this.inner = inner;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
    }

    /** How many of {@code stack} may go into {@code slot}; override to cap it further. */
    protected int insertLimit(int slot, ItemStack stack) {
        return stack.getCount();
    }

    @Override
    public int getSlots() {
        return inner.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inner.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !canInsert.test(slot)) {
            return stack;
        }
        int limit = Math.min(stack.getCount(), insertLimit(slot, stack));
        if (limit <= 0) {
            return stack;
        }
        if (limit == stack.getCount()) {
            return inner.insertItem(slot, stack, simulate);
        }
        ItemStack rest = inner.insertItem(slot, stack.copyWithCount(limit), simulate);
        return stack.copyWithCount(stack.getCount() - limit + rest.getCount());
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return canExtract.test(slot) ? inner.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return inner.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return canInsert.test(slot) && inner.isItemValid(slot, stack) && insertLimit(slot, stack) > 0;
    }
}
