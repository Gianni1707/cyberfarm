package com.cyberfarm;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.ModList;

/**
 * Gives every player the Drone Engineer's Handbook once, on their first login.
 * The handbook is a Patchouli book, so this only runs when Patchouli is installed;
 * Patchouli is looked up by id to keep it an optional dependency.
 */
public final class HandbookGifting {
    public static final ResourceLocation HANDBOOK_ID = CyberFarm.id("handbook");
    private static final String PATCHOULI = "patchouli";
    private static final String RECEIVED_TAG = "cyberfarm.received_handbook";

    private HandbookGifting() {}

    static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ModList.get().isLoaded(PATCHOULI)) {
            return;
        }
        Player player = event.getEntity();
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(RECEIVED_TAG)) {
            return;
        }

        ItemStack handbook = createHandbook();
        if (handbook.isEmpty()) {
            return;
        }
        if (!player.addItem(handbook)) {
            player.drop(handbook, false);
        }
        persisted.putBoolean(RECEIVED_TAG, true);
        data.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** Patchouli's guide book item, told which book it is through its NBT; empty without Patchouli. */
    public static ItemStack createHandbook() {
        Item book = BuiltInRegistries.ITEM.get(new ResourceLocation(PATCHOULI, "guide_book"));
        if (book == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(book);
        stack.getOrCreateTag().putString("patchouli:book", HANDBOOK_ID.toString());
        return stack;
    }
}
