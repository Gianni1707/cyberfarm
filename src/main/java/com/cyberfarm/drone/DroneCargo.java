package com.cyberfarm.drone;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.cyberfarm.block.entity.DroneDockBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** A drone's cargo hold: what it harvests, digs or carries, and the supplies it takes from its dock. */
public class DroneCargo {
    public static final int SLOTS = 9;

    private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(SLOTS);

    /** The hold as a resource handler, for moving items in and out of other inventories. */
    public ItemStacksResourceHandler handler() {
        return items;
    }

    public boolean isFull() {
        for (int i = 0; i < items.size(); i++) {
            if (items.getResource(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return ResourceHandlerUtil.isEmpty(items);
    }

    public int count() {
        int count = 0;
        for (int i = 0; i < items.size(); i++) {
            count += items.getAmountAsInt(i);
        }
        return count;
    }

    public boolean contains(Predicate<ItemStack> filter) {
        return find(filter) >= 0;
    }

    /** Removes and returns one item of the first stack matching {@code filter}, or EMPTY. */
    public ItemStack takeOne(Predicate<ItemStack> filter) {
        int slot = find(filter);
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ItemUtil.getStack(items, slot);
        items.set(slot, ItemResource.of(stack), stack.getCount() - 1);
        return stack.copyWithCount(1);
    }

    /** One item of the first stack, or EMPTY: what the Transport claw shows. */
    public ItemStack firstItem() {
        ItemStack shown = ItemStack.EMPTY;
        for (int i = 0; i < items.size() && shown.isEmpty(); i++) {
            shown = ItemUtil.getStack(items, i).copyWithCount(1);
        }
        return shown;
    }

    /** Stores what fits of {@code stack}; returns the rest. */
    public ItemStack store(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = ResourceHandlerUtil.insertStacking(items, ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
            return stack.copyWithCount(stack.getCount() - inserted);
        }
    }

    /** Stores the drops; whatever does not fit falls on the ground at {@code pos}. */
    public void collect(ServerLevel level, BlockPos pos, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            ItemStack rest = store(drop);
            if (!rest.isEmpty()) {
                Block.popResource(level, pos, rest);
            }
        }
    }

    /** Moves everything the dock can take into it. */
    public void unloadInto(DroneDockBlockEntity dock) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = ItemUtil.getStack(items, i);
            if (!stack.isEmpty()) {
                ItemStack rest = dock.deposit(stack);
                items.set(i, ItemResource.of(rest), rest.getCount());
            }
        }
    }

    /** Empties the hold, handing every stack to {@code drop}. */
    public void dropAll(Consumer<ItemStack> drop) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = ItemUtil.getStack(items, i);
            if (!stack.isEmpty()) {
                drop.accept(stack);
                items.set(i, ItemResource.EMPTY, 0);
            }
        }
    }

    private int find(Predicate<ItemStack> filter) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = ItemUtil.getStack(items, i);
            if (!stack.isEmpty() && filter.test(stack)) {
                return i;
            }
        }
        return -1;
    }

    public void save(ValueOutput output) {
        items.serialize(output);
    }

    public void load(ValueInput input) {
        items.deserialize(input);
    }
}
