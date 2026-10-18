package com.cyberfarm;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Gives every player the Drone Engineer's Handbook once, on their first login.
 * The handbook is a GuideME guide, so this only runs when GuideME is installed;
 * GuideME is looked up by id to keep it an optional dependency.
 */
public final class HandbookGifting {
    public static final Identifier HANDBOOK_ID = CyberFarm.id("handbook");
    private static final String GUIDEME = "guideme";
    private static final String RECEIVED_TAG = "cyberfarm.received_handbook";

    private HandbookGifting() {}

    static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ModList.get().isLoaded(GUIDEME)) {
            return;
        }
        Player player = event.getEntity();
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
        if (persisted.getBooleanOr(RECEIVED_TAG, false)) {
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

    @SuppressWarnings("unchecked")
    public static ItemStack createHandbook() {
        Item guideItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(GUIDEME, "guide"));
        var component = (DataComponentType<Identifier>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.fromNamespaceAndPath(GUIDEME, "guide_id"));
        if (guideItem == Items.AIR || component == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(guideItem);
        stack.set(component, HANDBOOK_ID);
        return stack;
    }
}
