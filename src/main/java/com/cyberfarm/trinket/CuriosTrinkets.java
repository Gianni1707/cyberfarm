package com.cyberfarm.trinket;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * With Curios installed, each trinket has a Curios slot of its own (data/cyberfarm/curios/slots, given to players by
 * data/cyberfarm/curios/entities/trinkets.json, filled by the tags under data/curios/tags/item). Only touched when
 * Curios is loaded.
 */
final class CuriosTrinkets {
    private CuriosTrinkets() {}

    static ItemStack get(Player player, TrinketType type) {
        return CuriosApi.getCuriosInventory(player).flatMap(curios -> curios.getStacksHandler(type.curiosSlot()))
                .filter(handler -> handler.getSlots() > 0)
                .map(handler -> handler.getStacks().getStackInSlot(0)).orElse(ItemStack.EMPTY);
    }

    static void set(Player player, TrinketType type, ItemStack stack) {
        CuriosApi.getCuriosInventory(player).flatMap(curios -> curios.getStacksHandler(type.curiosSlot()))
                .filter(handler -> handler.getSlots() > 0)
                .ifPresent(handler -> handler.getStacks().setStackInSlot(0, stack));
    }
}
