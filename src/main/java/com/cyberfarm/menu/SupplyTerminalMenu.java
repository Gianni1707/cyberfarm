package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.block.entity.SupplyTerminalBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class SupplyTerminalMenu extends MachineMenu {
    public static final int DELIVERY_X = 151;
    public static final int DELIVERY_Y = 25;
    public static final int PAYOUT_Y = 61;
    public static final int INVENTORY_Y = 112;
    private static final int GLOBAL = SupplyTerminalBlockEntity.CONTRACTS * SupplyTerminalBlockEntity.CONTRACT_DATA;

    public SupplyTerminalMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStacksResourceHandler(2),
                new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + GLOBAL + 7), ContainerLevelAccess.NULL);
    }

    public SupplyTerminalMenu(int containerId, Inventory inventory, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.SUPPLY_TERMINAL.get(), containerId, data, access, ModBlocks.SUPPLY_TERMINAL.get());
        addMachineSlot(items, SupplyTerminalBlockEntity.DELIVERY, DELIVERY_X, DELIVERY_Y);
        addMachineSlot(items, SupplyTerminalBlockEntity.PAYOUT, DELIVERY_X, PAYOUT_Y);
        addPlayerInventory(inventory, INVENTORY_Y);
    }

    private int field(int contract, int field) {
        return extra(contract * SupplyTerminalBlockEntity.CONTRACT_DATA + field);
    }

    private static ItemStack stack(int id, int count) {
        return id < 0 ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.byId(id), Math.max(1, count));
    }

    /** The item contract {@code index} asks for, or empty for a free place on the board. */
    public ItemStack contractItem(int index) {
        return stack(field(index, 0), 1);
    }

    public int contractRequired(int index) {
        return field(index, 1);
    }

    public int contractDelivered(int index) {
        return field(index, 2);
    }

    public int contractEmeralds(int index) {
        return field(index, 3);
    }

    /** The bonus item the contract pays besides its emeralds, or empty. */
    public ItemStack contractBonus(int index) {
        return stack(field(index, 4), field(index, 5));
    }

    public SupplyTerminalBlockEntity.Kind contractKind(int index) {
        return SupplyTerminalBlockEntity.Kind.values()[Math.clamp(field(index, 6), 0, SupplyTerminalBlockEntity.Kind.values().length - 1)];
    }

    /** Seconds until the contract closes, or -1 for none. */
    public int contractSecondsLeft(int index) {
        return field(index, 7);
    }

    /** The level of the Drone Mechanist working the terminal, 0 for none. */
    public int mechanistLevel() {
        return extra(GLOBAL);
    }

    public int nextContractSeconds() {
        return extra(GLOBAL + 1);
    }

    public int prosperity() {
        return extra(GLOBAL + 2);
    }

    /** The village's level, or -1 while nobody has founded it. */
    public int villageLevel() {
        return extra(GLOBAL + 3);
    }

    /** Where the terminal stands: the middle of the village. */
    public BlockPos terminalPos() {
        return new BlockPos(extra(GLOBAL + 4), extra(GLOBAL + 5), extra(GLOBAL + 6));
    }
}
