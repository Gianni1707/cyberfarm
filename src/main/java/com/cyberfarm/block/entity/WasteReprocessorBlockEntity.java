package com.cyberfarm.block.entity;

import com.cyberfarm.menu.WasteReprocessorMenu;
import com.cyberfarm.radiation.Radiation;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Waste Reprocessor: treats a Depleted Fuel Rod with FE into a block of harmless Vitrified Waste,
 * recovering an Uranium Ingot one time in three. Waste inside it is shielded and does not contaminate.
 */
public class WasteReprocessorBlockEntity extends MachineBlockEntity {
    public static final int INPUT = 0;
    public static final int WASTE_OUTPUT = 1;
    public static final int URANIUM_OUTPUT = 2;
    public static final int PROCESS_TIME = 200;
    public static final int ENERGY_PER_TICK = 40;
    private static final int CAPACITY = 40_000;
    private static final int MAX_INPUT = 1_000;
    /** One treated rod in this many gives back an Uranium Ingot. */
    public static final int URANIUM_CHANCE = 3;

    private int progress;

    public WasteReprocessorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WASTE_REPROCESSOR.get(), pos, state, 3, CAPACITY, MAX_INPUT, 0);
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean canRun = Radiation.isWaste(getStack(INPUT)) && hasRoom(WASTE_OUTPUT, ModItems.VITRIFIED_WASTE.get())
                && hasRoom(URANIUM_OUTPUT, ModItems.URANIUM_INGOT.get());
        if (canRun && energy.consume(ENERGY_PER_TICK)) {
            if (++progress >= PROCESS_TIME) {
                progress = 0;
                ItemStack input = getStack(INPUT);
                setStack(INPUT, input.copyWithCount(input.getCount() - 1));
                grow(WASTE_OUTPUT, ModItems.VITRIFIED_WASTE.get());
                if (level.getRandom().nextInt(URANIUM_CHANCE) == 0) {
                    grow(URANIUM_OUTPUT, ModItems.URANIUM_INGOT.get());
                }
                level.playSound(null, pos, ModSounds.REPROCESSOR_WORK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            setChanged();
        } else if (!canRun && progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    private boolean hasRoom(int slot, Item item) {
        ItemStack stack = getStack(slot);
        return stack.isEmpty() || (stack.is(item) && stack.getCount() < stack.getMaxStackSize());
    }

    private void grow(int slot, Item item) {
        ItemStack stack = getStack(slot);
        setStack(slot, stack.isEmpty() ? new ItemStack(item) : stack.copyWithCount(stack.getCount() + 1));
    }

    /** Only waste goes in; the outputs are taken out by hand or by hoppers. */
    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return index == INPUT && resource.is(ModItems.DEPLETED_FUEL_ROD.get());
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> progress;
            case 1 -> PROCESS_TIME;
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 2;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("progress", 0);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new WasteReprocessorMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
