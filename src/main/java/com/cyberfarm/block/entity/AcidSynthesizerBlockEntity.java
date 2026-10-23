package com.cyberfarm.block.entity;

import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.fluid.TankGroup;
import com.cyberfarm.menu.AcidSynthesizerMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Acid Synthesizer: Water + Sulfur (ours or any mod's, by the common tag) → Acid, with FE.
 * The Methane Extractor's chamber needs the Acid to dissolve its mob.
 */
public class AcidSynthesizerBlockEntity extends MachineBlockEntity {
    public static final TagKey<Item> SULFUR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dusts/sulfur"));
    public static final int TANK_CAPACITY = 8_000;
    public static final int BATCH = 500;
    public static final int PROCESS_TIME = 40;
    public static final int ENERGY_PER_TICK = 20;
    private static final int CAPACITY = 20_000;
    private static final int MAX_INPUT = 500;
    private static final int OUTPUT_PER_SIDE = 500;

    private final GasTank input = new GasTank(TANK_CAPACITY, resource -> resource.is(Fluids.WATER.getFluidType()), this::setChanged);
    private final GasTank output = new GasTank(TANK_CAPACITY, resource -> false, this::setChanged);
    private final TankGroup fluids = TankGroup.of(new GasTank[] {input}, output);
    private int progress;
    /** What the clients last heard, so the fluid in the front windows stays up to date. */
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
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean canRun = getStack(0).is(SULFUR) && input.amount() >= BATCH && output.capacity() - output.amount() >= BATCH;
        if (canRun && energy.consume(ENERGY_PER_TICK)) {
            if (++progress >= PROCESS_TIME) {
                progress = 0;
                input.drain(BATCH);
                output.fill(ModFluids.ACID.get(), BATCH);
                ItemStack item = getStack(0);
                setStack(0, item.copyWithCount(item.getCount() - 1));
                level.playSound(null, pos, ModSounds.COOLANT_MIXER_WORK.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            setChanged();
        } else if (!canRun && progress != 0) {
            progress = 0;
            setChanged();
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return isSulfur(resource);
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
        return new AcidSynthesizerMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
