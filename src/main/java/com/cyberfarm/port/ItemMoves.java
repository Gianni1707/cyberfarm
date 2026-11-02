package com.cyberfarm.port;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/** Moving items between Forge item handlers, the 26.x ResourceHandlerUtil way (simulate in place of transactions). */
public final class ItemMoves {
    private ItemMoves() {}

    /**
     * Moves up to {@code max} items matching {@code filter} from {@code from} into {@code to}, stacking
     * onto what is already there. Returns how many moved (or would move, when simulating).
     */
    public static int move(@Nullable IItemHandler from, @Nullable IItemHandler to, Predicate<ItemResource> filter, int max, boolean simulate) {
        if (from == null || to == null || max <= 0) {
            return 0;
        }
        int moved = 0;
        for (int i = 0; i < from.getSlots() && moved < max; i++) {
            ItemStack stack = from.getStackInSlot(i);
            if (stack.isEmpty() || !filter.test(ItemResource.of(stack))) {
                continue;
            }
            ItemStack offered = from.extractItem(i, Math.min(max - moved, stack.getCount()), true);
            int fits = offered.getCount() - ItemHandlerHelper.insertItemStacked(to, offered, true).getCount();
            if (fits <= 0) {
                continue;
            }
            if (!simulate) {
                ItemStack taken = from.extractItem(i, fits, false);
                ItemStack rest = ItemHandlerHelper.insertItemStacked(to, taken, false);
                if (!rest.isEmpty()) {
                    // The target took less than it said: give the rest back where it came from
                    from.insertItem(i, rest, false);
                    fits -= rest.getCount();
                }
            }
            moved += fits;
        }
        return moved;
    }

    /** Whether at least one of {@code resource} fits in {@code to} right now. */
    public static boolean fits(IItemHandler to, ItemResource resource) {
        return ItemHandlerHelper.insertItemStacked(to, resource.toStack(1), true).isEmpty();
    }
}
