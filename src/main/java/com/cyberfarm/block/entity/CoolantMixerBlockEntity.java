package com.cyberfarm.block.entity;

import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.fluid.TankGroup;
import com.cyberfarm.menu.CoolantMixerMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModFluids;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Coolant Mixer: makes the Fission Reactor's better coolants with FE.
 * Water + Lapis Lazuli → Coolant; Coolant + Blue Ice → Cryo Coolant.
 */
public class CoolantMixerBlockEntity extends MachineBlockEntity {
    public static final int TANK_CAPACITY = 8_000;
    public static final int BATCH = 500;
    public static final int PROCESS_TIME = 40;
    public static final int ENERGY_PER_TICK = 20;
    private static final int CAPACITY = 20_000;
    private static final int MAX_INPUT = 500;
    private static final int OUTPUT_PER_SIDE = 500;

    private record Recipe(Fluid input, Item item, Fluid output) {}

    private static @Nullable Recipe recipeFor(Fluid input, ItemStack item) {
        if (input == Fluids.WATER && item.is(Items.LAPIS_LAZULI)) {
            return new Recipe(Fluids.WATER, Items.LAPIS_LAZULI, ModFluids.COOLANT.get());
        }
        if (input == ModFluids.COOLANT.get() && item.is(Items.BLUE_ICE)) {
            return new Recipe(ModFluids.COOLANT.get(), Items.BLUE_ICE, ModFluids.CRYO_COOLANT.get());
        }
        return null;
    }

    private final GasTank input = new GasTank(TANK_CAPACITY,
            resource -> resource.is(Fluids.WATER.getFluidType()) || resource.is(ModFluids.COOLANT_TYPE.get()), this::setChanged);
    private final GasTank output = new GasTank(TANK_CAPACITY, resource -> false, this::setChanged);
    private final TankGroup fluids = TankGroup.of(new GasTank[] {input}, output);
    private int progress;

    public CoolantMixerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COOLANT_MIXER.get(), pos, state, 1, CAPACITY, MAX_INPUT, 0);
    }

    public TankGroup getFluids() {
        return fluids;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        Recipe recipe = recipeFor(input.fluid(), getStack(0));
        boolean canRun = recipe != null && input.amount() >= BATCH
                && output.capacity() - output.amount() >= BATCH
                && (output.amount() == 0 || output.fluid() == recipe.output());
        if (canRun && energy.consume(ENERGY_PER_TICK)) {
            if (++progress >= PROCESS_TIME) {
                progress = 0;
                input.drain(BATCH);
                output.fill(recipe.output(), BATCH);
                ItemStack item = getStack(0);
                setStack(0, item.copyWithCount(item.getCount() - 1));
                level.playSound(null, pos, ModSounds.COOLANT_MIXER_WORK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            setChanged();
        } else if (!canRun && progress != 0) {
            progress = 0;
            setChanged();
        }
        output.pushToNeighbors(level, pos, OUTPUT_PER_SIDE);
    }

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return resource.is(Items.LAPIS_LAZULI) || resource.is(Items.BLUE_ICE);
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> progress;
            case 1 -> PROCESS_TIME;
            case 2 -> input.amount();
            case 3 -> input.fluidCode();
            case 4 -> output.amount();
            case 5 -> output.fluidCode();
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 6;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        input.serialize(out.child("input"));
        output.serialize(out.child("output"));
        out.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        input.deserialize(in.childOrEmpty("input"));
        output.deserialize(in.childOrEmpty("output"));
        progress = in.getIntOr("progress", 0);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CoolantMixerMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
