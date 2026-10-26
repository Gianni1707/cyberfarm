package com.cyberfarm.block.entity;

import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.fluid.TankGroup;
import com.cyberfarm.menu.PyrolysisOvenMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Pyrolysis Oven: splits Methane into carbon and hydrogen (CH4 -> C + 2H2) and draws the carbon
 * onto String into Carbon Fiber, the material of the advanced drone frames.
 */
public class PyrolysisOvenBlockEntity extends MachineBlockEntity {
    public static final int STRING = 0;
    public static final int OUTPUT = 1;
    public static final int TANK_CAPACITY = 8_000;
    public static final int METHANE_PER_FIBER = 250;
    public static final int PROCESS_TIME = 80;
    public static final int ENERGY_PER_TICK = 60;
    private static final int CAPACITY = 20_000;
    private static final int MAX_INPUT = 1_000;

    private final GasTank methane = new GasTank(TANK_CAPACITY, resource -> resource.is(ModFluids.METHANE.get().getFluidType()), this::setChanged);
    private final TankGroup fluids = TankGroup.of(new GasTank[] {methane});
    private int progress;

    public PyrolysisOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PYROLYSIS_OVEN.get(), pos, state, 2, CAPACITY, MAX_INPUT, 0);
    }

    public TankGroup getFluids() {
        return fluids;
    }

    @Override
    protected float progress() {
        return progress / (float) PROCESS_TIME;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean canRun = getStack(STRING).is(Items.STRING) && methane.amount() >= METHANE_PER_FIBER
                && hasRoomFor(OUTPUT, ModItems.CARBON_FIBER.get());
        if (canRun && energy.consume(ENERGY_PER_TICK)) {
            if (++progress >= PROCESS_TIME) {
                progress = 0;
                methane.drain(METHANE_PER_FIBER);
                ItemStack string = getStack(STRING);
                setStack(STRING, string.copyWithCount(string.getCount() - 1));
                addOne(OUTPUT, ModItems.CARBON_FIBER.get());
                level.playSound(null, pos, ModSounds.GENERATOR_CRACKLE.get(), SoundSource.BLOCKS, 0.8F, 0.8F);
            }
            setChanged();
        } else if (!canRun && progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return index == STRING && resource.is(Items.STRING);
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> progress;
            case 1 -> PROCESS_TIME;
            case 2 -> methane.amount();
            case 3 -> methane.fluidCode();
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 4;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        methane.serialize(output.child("methane"));
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        methane.deserialize(input.childOrEmpty("methane"));
        progress = input.getIntOr("progress", 0);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PyrolysisOvenMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
