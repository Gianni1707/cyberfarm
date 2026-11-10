package com.cyberfarm.block.entity;

import net.minecraft.world.level.block.ComposterBlock;

import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.menu.BioDigesterMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModSounds;
import net.minecraft.sounds.SoundSource;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.cyberfarm.port.ValueInput;
import com.cyberfarm.port.ValueOutput;
import com.cyberfarm.port.ItemResource;

/**
 * Bio-Digester: digests Biomass, which the Biomass Press makes of organic waste, into Biogas,
 * then pushes it into adjacent pipes and tanks. Needs no power.
 */
public class BioDigesterBlockEntity extends SmallMultiblockBlockEntity {
    public static final int TANK_CAPACITY = 8_000;
    public static final int PROCESS_TIME = 40;
    public static final int MB_PER_BIOMASS = 250;
    private static final int OUTPUT_PER_SIDE = 200;
    private static final int OPEN_TICKS = 60;
    private static final int SYNC_INTERVAL = 5;

    private final GasTank tank = new GasTank(TANK_CAPACITY, resource -> false, this::setChanged);
    private int progress;
    /** What the clients last heard, for the sight glass. */
    private int syncedTank = -1;

    public BioDigesterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIO_DIGESTER.get(), pos, state, 1, 0, 0, 0);
    }

    public GasTank getTank() {
        return tank;
    }

    @Override
    protected float progress() {
        return progress / (float) PROCESS_TIME;
    }

    @Override
    protected boolean isWorking() {
        return progress > 0;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean open = advanceStage(level, pos, state);
        ItemStack input = getStack(0);
        if (open && input.is(ModItems.BIOMASS.get()) && tank.amount() + MB_PER_BIOMASS <= tank.capacity()) {
            if (++progress >= PROCESS_TIME) {
                progress = 0;
                input.shrink(1);
                setStack(0, input);
                tank.fill(ModFluids.BIOGAS.get(), MB_PER_BIOMASS);
                level.playSound(null, pos, ModSounds.BIO_DIGESTER_WORK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            setChanged();
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }
        tank.pushToNeighbors(level, pos, OUTPUT_PER_SIDE);
        if (level.getGameTime() % SYNC_INTERVAL == 0 && tank.amount() != syncedTank) {
            syncedTank = tank.amount();
            level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public int openTicks() {
        return OPEN_TICKS;
    }

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return resource.toStack().is(ModItems.BIOMASS.get());
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> progress;
            case 1 -> PROCESS_TIME;
            case 2 -> tank.amount();
            case 3 -> tank.fluidCode();
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
        tank.serialize(output.child("tank"));
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("tank"));
        progress = input.getIntOr("progress", 0);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BioDigesterMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
