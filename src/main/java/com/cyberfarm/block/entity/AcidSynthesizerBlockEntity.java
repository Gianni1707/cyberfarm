package com.cyberfarm.block.entity;

import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.fluid.TankGroup;
import com.cyberfarm.menu.AcidSynthesizerMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import com.cyberfarm.port.ValueInput;
import com.cyberfarm.port.ValueOutput;
import com.cyberfarm.port.ItemResource;

/**
 * Acid Synthesizer: Water + Sulfur (ours or any mod's, by the common tag) → Acid, with FE.
 * The Methane Extractor's chamber needs the Acid to dissolve its mob.
 */
public class AcidSynthesizerBlockEntity extends SmallMultiblockBlockEntity {
    public static final TagKey<Item> SULFUR = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "dusts/sulfur"));
    public static final int TANK_CAPACITY = 8_000;
    public static final int BATCH = 500;
    public static final int PROCESS_TIME = 40;
    public static final int ENERGY_PER_TICK = 40;
    private static final int CAPACITY = 20_000;
    private static final int MAX_INPUT = 500;
    private static final int OUTPUT_PER_SIDE = 500;
    private static final int OPEN_TICKS = 70;

    private final GasTank input = new GasTank(TANK_CAPACITY, resource -> resource.is(Fluids.WATER.getFluidType()), this::setChanged);
    private final GasTank output = new GasTank(TANK_CAPACITY, resource -> false, this::setChanged);
    private final TankGroup fluids = TankGroup.of(new GasTank[] {input}, output);
    /** What the clients last heard, so the fluid in the sight glasses stays up to date. */
    private int syncedInput = -1;
    private int syncedOutput = -1;
    private boolean syncedWorking;
    private static final int SYNC_INTERVAL = 5;

    public AcidSynthesizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ACID_SYNTHESIZER.get(), pos, state, 1, CAPACITY, MAX_INPUT, 0);
    }

    public TankGroup getFluids() {
        return fluids;
    }

    public static boolean isSulfur(ItemResource resource) {
        return resource.toStack().is(SULFUR);
    }

    @Override
    protected float progress() {
        return progress / (float) workTime(PROCESS_TIME);
    }

    @Override
    public boolean takesAddons() {
        return true;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean open = advanceStage(level, pos, state);
        boolean canRun = open && getStack(0).is(SULFUR) && input.amount() >= BATCH && output.capacity() - output.amount() >= BATCH;
        if (work(canRun, ENERGY_PER_TICK, PROCESS_TIME)) {
            input.drain(BATCH);
            output.fill(ModFluids.ACID.get(), BATCH);
            take(0, 1);
            level.playSound(null, pos, ModSounds.COOLANT_MIXER_WORK.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        output.pushToNeighbors(level, pos, OUTPUT_PER_SIDE);
        boolean working = progress > 0;
        if (working != syncedWorking || level.getGameTime() % SYNC_INTERVAL == 0
                && (input.amount() != syncedInput || output.amount() != syncedOutput)) {
            syncedInput = input.amount();
            syncedOutput = output.amount();
            syncedWorking = working;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public GasTank getInput() {
        return input;
    }

    public GasTank getOutput() {
        return output;
    }

    /** Also on clients: a batch is being made. */
    public boolean isWorking() {
        return progress > 0;
    }

    @Override
    public int openTicks() {
        return OPEN_TICKS;
    }

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return isSulfur(resource);
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> progress;
            case 1 -> workTime(PROCESS_TIME);
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
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        input.deserialize(in.childOrEmpty("input"));
        output.deserialize(in.childOrEmpty("output"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AcidSynthesizerMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
