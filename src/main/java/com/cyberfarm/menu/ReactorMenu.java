package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.block.entity.ReactorControllerBlockEntity;
import com.cyberfarm.item.FuelRodItem;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

public class ReactorMenu extends MachineMenu {
    public static final int FUEL_X = 50;
    public static final int FUEL_Y = 20;
    public static final int WASTE_X = 50;
    public static final int WASTE_Y = 62;
    public static final int BUTTON_BOOST_DOWN = 0;
    public static final int BUTTON_BOOST_UP = 1;

    private final @Nullable ReactorControllerBlockEntity reactor;

    public ReactorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, clientItems(),
                new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 16), ContainerLevelAccess.NULL);
    }

    /** Client copy with the same slot rules, so shift-click predicts the server. */
    private static ItemStacksResourceHandler clientItems() {
        return new ItemStacksResourceHandler(ReactorControllerBlockEntity.FUEL_SLOTS + ReactorControllerBlockEntity.WASTE_SLOTS) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return index < ReactorControllerBlockEntity.FUEL_SLOTS && resource.getItem() instanceof FuelRodItem
                        && !FuelRodItem.isSpent(resource.toStack());
            }

            @Override
            protected int getCapacity(int index, ItemResource resource) {
                return index < ReactorControllerBlockEntity.FUEL_SLOTS ? 1 : super.getCapacity(index, resource);
            }
        };
    }

    public ReactorMenu(int containerId, Inventory inventory, @Nullable ReactorControllerBlockEntity reactor,
            ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.REACTOR.get(), containerId, data, access, ModBlocks.REACTOR_CONTROLLER.get());
        this.reactor = reactor;
        for (int i = 0; i < ReactorControllerBlockEntity.FUEL_SLOTS; i++) {
            addMachineSlot(items, i, FUEL_X + (i % 4) * 18, FUEL_Y + (i / 4) * 18);
        }
        for (int i = 0; i < ReactorControllerBlockEntity.WASTE_SLOTS; i++) {
            addMachineSlot(items, ReactorControllerBlockEntity.FUEL_SLOTS + i, WASTE_X + i * 18, WASTE_Y);
        }
        addPlayerInventory(inventory, 136);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (reactor == null || (id != BUTTON_BOOST_DOWN && id != BUTTON_BOOST_UP)) {
            return false;
        }
        reactor.changeBoost(id == BUTTON_BOOST_UP ? 1 : -1);
        return true;
    }

    public boolean isFormed() {
        return extra(0) != 0;
    }

    public int getActiveRods() {
        return extra(1);
    }

    public int getWater() {
        return extra(2);
    }

    public int getWaterFluid() {
        return extra(3);
    }

    public int getSteam() {
        return extra(4);
    }

    public int getSteamFluid() {
        return extra(5);
    }

    public int getSteamRate() {
        return extra(6);
    }

    public int getCoolant() {
        return extra(7);
    }

    public int getCoolantFluid() {
        return extra(8);
    }

    /** Core temperature from 0 (cold) to 1000 (meltdown). */
    public int getTemperature() {
        return extra(9);
    }

    public int getBoost() {
        return extra(10);
    }

    public int getTransferCapacity() {
        return extra(11);
    }

    public int getHeatProduced() {
        return extra(12);
    }

    /** 0 while the tower is incomplete, then 1 small, 2 medium, 3 large. */
    public int getSize() {
        return extra(13);
    }

    /** How many of the fuel slots the tower burns. */
    public int getUsableRods() {
        return extra(14);
    }

    public int getMaxBoost() {
        return extra(15);
    }
}
