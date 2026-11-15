package com.cyberfarm.trinket;

import java.util.function.UnaryOperator;

import com.cyberfarm.registry.ModItems;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Utility Belt: a leather belt worn in the Belt slot, with four holders of its own (opened with its key): the Energy
 * Sphere that powers every trinket, the Multiblock Projector, the Survey Tool and the Pocket Drone. What it holds is
 * drawn on the player's belt, and the two tools come to hand with their keys.
 */
public class UtilityBeltItem extends TrinketItem {
    public static final int SPHERE = 0, PROJECTOR = 1, SURVEY_TOOL = 2, DRONE = 3, SLOTS = 4;
    /** Where the holders are kept in the belt's NBT (26.x: the container component). */
    private static final String HOLDERS = "belt";

    public UtilityBeltItem(Properties properties) {
        super(properties, TrinketType.BELT);
    }

    /** What the belt holds in {@code slot}. */
    public static ItemStack content(ItemStack belt, int slot) {
        CompoundTag tag = belt.getTagElement(HOLDERS);
        if (tag == null) {
            return ItemStack.EMPTY;
        }
        // Only the one asked for is read: this runs every tick for the sphere's charge
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if ((entry.getByte("Slot") & 255) == slot) {
                return ItemStack.of(entry);
            }
        }
        return ItemStack.EMPTY;
    }

    /** A copy of the belt with {@code stack} in {@code slot}. */
    public static ItemStack withContent(ItemStack belt, int slot, ItemStack stack) {
        NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        CompoundTag old = belt.getTagElement(HOLDERS);
        if (old != null) {
            ContainerHelper.loadAllItems(old, items);
        }
        items.set(slot, stack);
        ItemStack copy = belt.copy();
        ContainerHelper.saveAllItems(copy.getOrCreateTagElement(HOLDERS), items, true);
        return copy;
    }

    /** Which item each holder takes. */
    public static boolean fits(int slot, ItemStack stack) {
        return switch (slot) {
            case SPHERE -> stack.getItem() instanceof EnergySphereItem;
            case PROJECTOR -> stack.is(ModItems.MULTIBLOCK_PROJECTOR.get());
            case SURVEY_TOOL -> stack.is(ModItems.SURVEY_TOOL.get());
            case DRONE -> stack.getItem() instanceof PocketDroneItem;
            default -> false;
        };
    }

    /** The belt the player wears, or empty. */
    public static ItemStack worn(Player player) {
        return Trinkets.worn(player, TrinketType.BELT, UtilityBeltItem.class);
    }

    /** What the worn belt holds in {@code slot}; empty without a belt. */
    public static ItemStack content(Player player, int slot) {
        return content(worn(player), slot);
    }

    /** Changes what the worn belt holds in {@code slot}, and saves the belt (which syncs it). */
    public static void update(Player player, int slot, UnaryOperator<ItemStack> change) {
        ItemStack belt = worn(player);
        if (!belt.isEmpty()) {
            Trinkets.set(player, TrinketType.BELT, withContent(belt, slot, change.apply(content(belt, slot))));
        }
    }

    // ---------------------------------------------------------------- power for the trinkets

    /** FE in the Energy Sphere on the worn belt; 0 without one. */
    public static int energy(Player player) {
        return EnergySphereItem.energy(content(player, SPHERE));
    }

    /** Takes up to {@code amount} FE from the sphere. Returns what it took. */
    public static int use(Player player, int amount) {
        int taken = Math.min(amount, energy(player));
        if (taken > 0) {
            update(player, SPHERE, sphere -> EnergySphereItem.withEnergy(sphere, EnergySphereItem.energy(sphere) - taken));
        }
        return taken;
    }
}
