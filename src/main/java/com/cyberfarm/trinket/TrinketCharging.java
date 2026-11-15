package com.cyberfarm.trinket;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** What the Human Dock does to the player on it: charges the belt's sphere and drone, then every FE item they carry. */
public final class TrinketCharging {
    private TrinketCharging() {}

    /** Charges with up to {@code budget} FE. Returns the FE used. */
    public static int chargePlayer(Player player, int budget) {
        int used = chargeHolder(player, UtilityBeltItem.SPHERE, budget);
        used += chargeHolder(player, UtilityBeltItem.DRONE, budget - used);
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && used < budget; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                continue;
            }
            EnergyHandler handler = ItemAccess.forPlayerSlot(player, slot).getCapability(Capabilities.Energy.ITEM);
            if (handler != null) {
                try (Transaction transaction = Transaction.openRoot()) {
                    used += handler.insert(budget - used, transaction);
                    transaction.commit();
                }
            }
        }
        return used;
    }

    /** Only the Energy Sphere on the belt, up to {@code budget} FE: what a Transmitter Dish sends from afar. Returns the FE used. */
    public static int chargeSphere(Player player, int budget) {
        return chargeHolder(player, UtilityBeltItem.SPHERE, budget);
    }

    /** The belt's sphere or drone, which no other mod's charger reaches inside the belt. */
    private static int chargeHolder(Player player, int holder, int budget) {
        ItemStack stack = UtilityBeltItem.content(player, holder);
        int energy = holder == UtilityBeltItem.SPHERE ? EnergySphereItem.energy(stack) : PocketDroneItem.energy(stack);
        int capacity = holder == UtilityBeltItem.SPHERE ? EnergySphereItem.capacity(stack) : PocketDroneItem.CAPACITY;
        int added = stack.isEmpty() ? 0 : Math.min(budget, capacity - energy);
        if (added <= 0) {
            return 0;
        }
        UtilityBeltItem.update(player, holder, held -> holder == UtilityBeltItem.SPHERE
                ? EnergySphereItem.withEnergy(held, energy + added) : PocketDroneItem.withEnergy(held, energy + added));
        return added;
    }
}
