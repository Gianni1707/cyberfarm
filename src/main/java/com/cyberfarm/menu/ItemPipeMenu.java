package com.cyberfarm.menu;

import com.cyberfarm.block.entity.ItemFilter;
import com.cyberfarm.block.entity.ItemPipeBlockEntity;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Item Pipe filter GUI. The 9 filter slots are ghost slots: clicking one with an item copies it as a
 * sample, clicking it empty-handed clears it; no item is ever taken from the player.
 */
public class ItemPipeMenu extends MachineMenu {
    public static final int FILTER_X = 8;
    public static final int FILTER_Y = 20;
    public static final int BUTTON_WHITELIST = 0;
    public static final int BUTTON_COMPONENTS = 1;
    public static final int BUTTON_MOD = 2;
    public static final int BUTTON_TAG_PREVIOUS = 3;
    public static final int BUTTON_TAG_NEXT = 4;

    private final @Nullable ItemPipeBlockEntity pipe;
    private final Container samples;

    public ItemPipeMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 4), ContainerLevelAccess.NULL);
    }

    public ItemPipeMenu(int containerId, Inventory inventory, @Nullable ItemPipeBlockEntity pipe, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.ITEM_PIPE.get(), containerId, data, access,
                pipe != null ? pipe.getBlockState().getBlock() : ModBlocks.ITEM_PIPE.get());
        this.pipe = pipe;
        this.samples = pipe != null ? pipe.filter().items() : new SimpleContainer(ItemFilter.SLOTS);
        for (int i = 0; i < ItemFilter.SLOTS; i++) {
            addSlot(new Slot(samples, i, FILTER_X + i * 18, FILTER_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        addPlayerInventory(inventory, 84);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < ItemFilter.SLOTS) {
            ItemStack carried = getCarried();
            samples.setItem(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    /** Shift-clicking an item in the inventory adds it as a sample, if it is not one already. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = slots.get(index).getItem();
        if (index < ItemFilter.SLOTS || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int free = -1;
        for (int i = 0; i < ItemFilter.SLOTS; i++) {
            if (ItemStack.isSameItemSameComponents(samples.getItem(i), stack)) {
                return ItemStack.EMPTY;
            }
            if (free < 0 && samples.getItem(i).isEmpty()) {
                free = i;
            }
        }
        if (free >= 0) {
            samples.setItem(free, stack.copyWithCount(1));
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (pipe == null) {
            return false;
        }
        ItemFilter filter = pipe.filter();
        switch (id) {
            case BUTTON_WHITELIST -> filter.toggleWhitelist();
            case BUTTON_COMPONENTS -> filter.toggleComponents();
            case BUTTON_MOD -> filter.toggleMod();
            case BUTTON_TAG_PREVIOUS -> filter.cycleTag(-1);
            case BUTTON_TAG_NEXT -> filter.cycleTag(1);
            default -> {
                return false;
            }
        }
        pipe.setChanged();
        return true;
    }

    public boolean isWhitelist() {
        return extra(0) != 0;
    }

    public boolean matchesComponents() {
        return extra(1) != 0;
    }

    public boolean matchesMod() {
        return extra(2) != 0;
    }

    public int getTagIndex() {
        return extra(3);
    }

    /** The sample whose tags the tag button steps through. */
    public ItemStack firstSample() {
        for (int i = 0; i < ItemFilter.SLOTS; i++) {
            if (!samples.getItem(i).isEmpty()) {
                return samples.getItem(i);
            }
        }
        return ItemStack.EMPTY;
    }
}
