package com.cyberfarm.port;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/** The 26.x ItemResource: an item with its NBT and no count, usable as a map key. */
public final class ItemResource {
    public static final ItemResource EMPTY = new ItemResource(Items.AIR, null);

    private final Item item;
    private final @Nullable CompoundTag tag;

    private ItemResource(Item item, @Nullable CompoundTag tag) {
        this.item = item;
        this.tag = tag;
    }

    public static ItemResource of(ItemStack stack) {
        return stack.isEmpty() ? EMPTY : new ItemResource(stack.getItem(), stack.getTag() == null ? null : stack.getTag().copy());
    }

    public static ItemResource of(ItemLike item) {
        return item.asItem() == Items.AIR ? EMPTY : new ItemResource(item.asItem(), null);
    }

    public boolean isEmpty() {
        return item == Items.AIR;
    }

    public Item getItem() {
        return item;
    }

    public boolean is(Item other) {
        return item == other;
    }

    @SuppressWarnings("deprecation")
    public boolean is(TagKey<Item> itemTag) {
        return item.builtInRegistryHolder().is(itemTag);
    }

    /** Whether {@code stack} holds this item with the same NBT. */
    public boolean matches(ItemStack stack) {
        return stack.getItem() == item && Objects.equals(stack.getTag(), tag);
    }

    public ItemStack toStack(int count) {
        if (isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, count);
        if (tag != null) {
            stack.setTag(tag.copy());
        }
        return stack;
    }

    public ItemStack toStack() {
        return toStack(1);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ItemResource other && other.item == item && Objects.equals(other.tag, tag);
    }

    @Override
    public int hashCode() {
        return 31 * item.hashCode() + Objects.hashCode(tag);
    }
}
