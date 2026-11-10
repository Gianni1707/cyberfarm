package com.cyberfarm.block.entity;

import com.cyberfarm.block.CoolantMixerBlock;
import com.cyberfarm.block.CyberFrameBlock;
import com.cyberfarm.block.HiddenFrameBlock;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.fluid.TankGroup;
import com.cyberfarm.menu.CoolantMixerMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import com.cyberfarm.port.ValueInput;
import com.cyberfarm.port.ValueOutput;
import com.cyberfarm.port.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Coolant Mixer: makes the Fission Reactor's better coolants with FE.
 * Water + Lapis Lazuli → Coolant; Coolant + Blue Ice → Cryo Coolant.
 * It works once assembled with its Cyber Frame, see {@link CoolantMixerBlock}.
 */
public class CoolantMixerBlockEntity extends MachineBlockEntity {
    public static final int TANK_CAPACITY = 8_000;
    public static final int BATCH = 500;
    public static final int PROCESS_TIME = 40;
    public static final int ENERGY_PER_TICK = 20;
    private static final int CAPACITY = 20_000;
    private static final int MAX_INPUT = 500;
    private static final int OUTPUT_PER_SIDE = 500;
    public static final int OPEN_TICKS = 60;
    /** How long the renderer goes on drawing after a stage changes, while the chunk rebuilds under it. */
    public static final int REBUILD_TICKS = 20;
    private static final int FORM_CHECK_INTERVAL = 10;
    private static final int SYNC_INTERVAL = 5;

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
    private int stageTicks;
    private int stageWas = -1;
    /** What the clients last heard, so the fluid in the sight glasses stays up to date. */
    private int syncedInput = -1;
    private int syncedOutput = -1;
    /** Client side, for the renderer: the stage it last saw, and when. */
    public int shownStage = -1;
    public float shownStageTime;

    public CoolantMixerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COOLANT_MIXER.get(), pos, state, 1, CAPACITY, MAX_INPUT, 0);
    }

    public TankGroup getFluids() {
        return fluids;
    }

    public GasTank getInput() {
        return input;
    }

    public GasTank getOutput() {
        return output;
    }

    @Override
    protected float progress() {
        return progress / (float) PROCESS_TIME;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        int stage = state.getValue(CoolantMixerBlock.STAGE);
        advanceStage(level, pos, state, stage);
        Recipe recipe = recipeFor(input.fluid(), getStack(0));
        boolean canRun = stage == CoolantMixerBlock.OPEN && recipe != null && input.amount() >= BATCH
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
        if (level.getGameTime() % SYNC_INTERVAL == 0 && (input.amount() != syncedInput || output.amount() != syncedOutput)) {
            syncedInput = input.amount();
            syncedOutput = output.amount();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** A frame on its right assembles it; the opening and the fold-back take their time before the next stage. */
    private void advanceStage(ServerLevel level, BlockPos pos, BlockState state, int stage) {
        if (stage != stageWas) {
            stageWas = stage;
            stageTicks = 0;
        }
        stageTicks++;
        if (stage == CoolantMixerBlock.LOOSE && level.getGameTime() % FORM_CHECK_INTERVAL == 0) {
            Direction side = CoolantMixerBlock.frameSide(state);
            BlockState frame = level.getBlockState(pos.relative(side));
            if (frame.is(ModBlocks.CYBER_FRAME.get()) && !frame.getValue(CyberFrameBlock.FORMED)) {
                level.setBlock(pos.relative(side), ModBlocks.HIDDEN_FRAME.get().defaultBlockState().setValue(HiddenFrameBlock.TO_MACHINE, side.getOpposite()), Block.UPDATE_ALL);
                level.setBlock(pos, level.getBlockState(pos).setValue(CoolantMixerBlock.STAGE, CoolantMixerBlock.OPENING), Block.UPDATE_ALL);
            }
        } else if (stage == CoolantMixerBlock.OPENING && stageTicks >= OPEN_TICKS) {
            level.setBlock(pos, state.setValue(CoolantMixerBlock.STAGE, CoolantMixerBlock.OPEN), Block.UPDATE_ALL);
        } else if (stage == CoolantMixerBlock.CLOSING && stageTicks >= OPEN_TICKS + REBUILD_TICKS) {
            level.setBlock(pos, state.setValue(CoolantMixerBlock.STAGE, CoolantMixerBlock.LOOSE), Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
