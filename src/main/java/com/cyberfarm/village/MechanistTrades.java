package com.cyberfarm.village;

import java.util.List;

import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModVillagers;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.event.village.VillagerTradesEvent;

/** The Drone Mechanist's trades (data on 26.x, code on 1.20.1): a villager picks two per level, as vanilla does. */
public final class MechanistTrades {
    private static final float PRICE_MULTIPLIER = 0.05F;

    private MechanistTrades() {}

    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != ModVillagers.DRONE_MECHANIST.get()) {
            return;
        }
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
        trades.get(1).add(trade(Items.EMERALD, 1, ModItems.BASIC_CIRCUIT.get(), 2, 12, 2));
        trades.get(1).add(trade(Items.REDSTONE, 20, Items.EMERALD, 1, 16, 2));
        trades.get(2).add(trade(Items.EMERALD, 3, ModItems.BATTERY_CELL.get(), 2, 12, 10));
        trades.get(2).add(trade(Items.COPPER_INGOT, 10, Items.EMERALD, 1, 16, 10));
        trades.get(3).add(trade(Items.EMERALD, 6, ModItems.ADVANCED_CIRCUIT.get(), 1, 8, 20));
        trades.get(3).add(trade(Items.EMERALD, 4, ModItems.ALUMINUM_INGOT.get(), 4, 12, 15));
        trades.get(4).add(trade(Items.EMERALD, 8, ModItems.CARBON_FIBER.get(), 4, 8, 20));
        trades.get(4).add(trade(Items.EMERALD, 10, ModItems.SPEED_ADDON.get(), 1, 4, 25));
        trades.get(4).add(trade(Items.EMERALD, 12, ModItems.STACK_ADDON.get(), 1, 4, 25));
        trades.get(5).add(trade(Items.EMERALD, 16, ModItems.ANCHOR_ADDON.get(), 1, 4, 30));
        trades.get(5).add(trade(Items.EMERALD, 24, ModItems.ELITE_CIRCUIT.get(), 1, 4, 30));
    }

    private static VillagerTrades.ItemListing trade(ItemLike wants, int wantCount, ItemLike gives, int giveCount, int maxUses, int xp) {
        return (trader, random) -> new MerchantOffer(new ItemStack(wants, wantCount), new ItemStack(gives, giveCount), maxUses, xp, PRICE_MULTIPLIER);
    }
}
