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

    /** Charges with up to {@code budget} FE, for {@code ticks} ticks of charging. Returns the FE used. */
    public static int chargePlayer(Player player, int budget, int ticks) {
        int used = chargeHolder(player, UtilityBeltItem.SPHERE, budget);
        used += chargeHolder(player, UtilityBeltItem.DRONE, budget - used);
        return used + chargeInventory(player, budget - used, ticks, false);
    }

    /**
     * The belt's sphere feeding every FE item the player carries, any mod's, up to {@code budget} FE. Spare spheres are
     * left alone: moving FE from one sphere to another would only spin it around. Returns the FE taken from the belt.
     */
    public static int chargeFromSphere(Player player, int budget, int ticks) {
        int used = chargeInventory(player, Math.min(budget, UtilityBeltItem.energy(player)), ticks, true);
        return UtilityBeltItem.use(player, used);
    }

    /**
     * Every FE item in the inventory, armour and offhand included, up to {@code budget} FE. An item takes at most its own
     * per-tick rate in one insert, so each gets one insert per tick of charging. Returns the FE used.
     */
    private static int chargeInventory(Player player, int budget, int ticks, boolean skipSpheres) {
        int used = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && used < budget; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || skipSpheres && stack.getItem() instanceof EnergySphereItem) {
                continue;
            }
            EnergyHandler handler = ItemAccess.forPlayerSlot(player, slot).getCapability(Capabilities.Energy.ITEM);
            if (handler != null) {
                for (int tick = 0; tick < ticks && used < budget; tick++) {
                    try (Transaction transaction = Transaction.openRoot()) {
                        int added = handler.insert(budget - used, transaction);
                        transaction.commit();
                        if (added == 0) {
                            break;
                        }
                        used += added;
                    }
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
