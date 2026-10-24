package com.cyberfarm.block.entity;

import com.cyberfarm.block.CrystallizerBlock;
import com.cyberfarm.block.CyberFrameBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.fluid.TankGroup;
import com.cyberfarm.menu.CrystallizerMenu;
import com.cyberfarm.multiblock.Blueprint;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Crystallizer: the controller at the front of a 3x3x4 chamber, where a Fusion Crystal grows from
 * an Isotope Shard and Amethyst Shards bathed in Acid, with a lot of FE. Complete, the chamber is
 * drawn as one machine with the crystal growing in its middle; it only works complete.
 */
public class CrystallizerBlockEntity extends MachineBlockEntity {
    public static final int ISOTOPE = 0;
    public static final int AMETHYST = 1;
    public static final int OUTPUT = 2;
    public static final int TANK_CAPACITY = 8_000;
    public static final int ACID_PER_CRYSTAL = 2_000;
    public static final int AMETHYST_PER_CRYSTAL = 4;
    public static final int PROCESS_TIME = 600;
    public static final int ENERGY_PER_TICK = 1_000;
    private static final int CAPACITY = 1_000_000;
    private static final int MAX_INPUT = 8_000;
    private static final int SYNC_INTERVAL = 20;
    private static final int CHECK_INTERVAL = 20;

    private final GasTank acid = new GasTank(TANK_CAPACITY, resource -> resource.is(ModFluids.ACID.get().getFluidType()), this::setChanged);
    private int progress;
    /** What the clients last heard, so the floating crystal grows and shows a finished one. */
    private int syncedProgress = -1;
    private boolean syncedDone;
    private int syncedAcid = -1;
    private int checkCooldown;
    private boolean formed;
    /** Client side: when the last progress came in, so the crystal keeps growing smoothly between updates. */
    private long syncedAt;

    public CrystallizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTALLIZER.get(), pos, state, 3, CAPACITY, MAX_INPUT, 0);
    }

    /** Pipes only put acid in: none of it can be pulled back out. */
    private final TankGroup automationFluids = TankGroup.of(new GasTank[] {acid});

    /** What automation sees, here and through the ports: the isotope and amethyst go in, only the crystal comes out. */
    private final ResourceHandler<ItemResource> automationItems = new ResourceHandler<>() {
        @Override
        public int size() {
            return items.size();
        }

        @Override
        public ItemResource getResource(int index) {
            return items.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return items.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return items.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return items.isValid(index, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return index != OUTPUT ? items.insert(index, resource, amount, transaction) : 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return index == OUTPUT ? items.extract(index, resource, amount, transaction) : 0;
        }
    };

    public ResourceHandler<FluidResource> portFluids() {
        return automationFluids;
    }

    public ResourceHandler<ItemResource> portItems() {
        return automationItems;
    }

    /** The formed Crystallizer whose chamber {@code pos} is part of, or null: what a port passes everything on to. */
    public static @Nullable CrystallizerBlockEntity ofChamberBlock(Level level, BlockPos pos) {
        // The controller sits on the chamber's floor, at most two blocks across and three down from any of its blocks
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-2, -3, -2), pos.offset(2, 0, 2))) {
            BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof CrystallizerBlock && state.getValue(CrystallizerBlock.FORMED)
                    && level.getBlockEntity(at) instanceof CrystallizerBlockEntity crystallizer) {
                BlockPos centre = crystallizer.centre();
                if (Math.abs(pos.getX() - centre.getX()) <= 1 && Math.abs(pos.getZ() - centre.getZ()) <= 1
                        && pos.getY() - centre.getY() >= 0 && pos.getY() - centre.getY() <= 3) {
                    return crystallizer;
                }
            }
        }
        return null;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (--checkCooldown <= 0) {
            checkCooldown = CHECK_INTERVAL;
            formed = isComplete(level);
            if (state.getValue(CrystallizerBlock.FORMED) != formed) {
                state = state.setValue(CrystallizerBlock.FORMED, formed);
                level.setBlock(pos, state, Block.UPDATE_ALL);
                level.playSound(null, pos, (formed ? ModSounds.EXTRACTOR_ACTIVATE : ModSounds.EXTRACTOR_DEACTIVATE).get(), SoundSource.BLOCKS, 1.0F, 1.2F);
            }
            setPartsFormed(level, formed);
        }
        boolean canRun = formed && getStack(ISOTOPE).is(ModItems.ISOTOPE_SHARD.get()) && getStack(AMETHYST).getCount() >= AMETHYST_PER_CRYSTAL
                && getStack(AMETHYST).is(Items.AMETHYST_SHARD) && acid.amount() >= ACID_PER_CRYSTAL
                && hasRoomFor(OUTPUT, ModItems.FUSION_CRYSTAL.get());
        if (canRun && energy.consume(ENERGY_PER_TICK)) {
            if (++progress >= PROCESS_TIME) {
                progress = 0;
                acid.drain(ACID_PER_CRYSTAL);
                ItemStack isotope = getStack(ISOTOPE);
                setStack(ISOTOPE, isotope.copyWithCount(isotope.getCount() - 1));
                ItemStack amethyst = getStack(AMETHYST);
                setStack(AMETHYST, amethyst.copyWithCount(amethyst.getCount() - AMETHYST_PER_CRYSTAL));
                addOne(OUTPUT, ModItems.FUSION_CRYSTAL.get());
                BlockPos centre = centre();
                level.playSound(null, centre.above(2), ModSounds.CRYSTALLIZER_DONE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                level.sendParticles(ParticleTypes.END_ROD, centre.getX() + 0.5, centre.getY() + 2.0, centre.getZ() + 0.5, 24, 0.3, 0.4, 0.3, 0.05);
            } else if (progress % 40 == 1) {
                level.playSound(null, centre().above(2), ModSounds.CRYSTALLIZER_WORK.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            }
            setChanged();
        } else if (!canRun && progress != 0) {
            progress = 0;
            setChanged();
        }
        boolean done = !getStack(OUTPUT).isEmpty();
        boolean due = level.getGameTime() % SYNC_INTERVAL == 0;
        if (done != syncedDone || progress != syncedProgress && (progress == 0 || due) || acid.amount() != syncedAcid && due) {
            syncedProgress = progress;
            syncedDone = done;
            syncedAcid = acid.amount();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Also on clients: how far the crystal has grown at {@code time}, 0 to 1, running on smoothly between updates. */
    public float growth(float time) {
        return progress <= 0 ? 0.0F : Math.min(1.0F, (progress + Math.max(0.0F, time - syncedAt)) / PROCESS_TIME);
    }

    /** Also on clients: how full the acid tank is, 0 to 1. */
    public float acidFill() {
        return (float) acid.amount() / TANK_CAPACITY;
    }

    private Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    /** The middle of the chamber, on the ground. */
    public BlockPos centre() {
        return Blueprint.crystallizerCentre(worldPosition, facing());
    }

    private boolean isComplete(ServerLevel level) {
        return Blueprint.CRYSTALLIZER.ghosts(worldPosition, facing()).stream().allMatch(ghost -> ghost.ok().test(level.getBlockState(ghost.pos())));
    }

    /** Hides (or shows again) the chamber's frames and glass, which the controller draws whole. */
    private void setPartsFormed(ServerLevel level, boolean formed) {
        for (Blueprint.Ghost ghost : Blueprint.CRYSTALLIZER.ghosts(worldPosition, facing())) {
            BlockState part = level.getBlockState(ghost.pos());
            if (!ghost.pos().equals(worldPosition) && part.hasProperty(CyberFrameBlock.FORMED) && part.getValue(CyberFrameBlock.FORMED) != formed) {
                level.setBlock(ghost.pos(), part.setValue(CyberFrameBlock.FORMED, formed), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            setPartsFormed(server, false);
        }
    }

    /** Also on clients: a finished crystal waits in the output. */
    public boolean hasCrystal() {
        return !getStack(OUTPUT).isEmpty();
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
        return switch (index) {
            case ISOTOPE -> resource.is(ModItems.ISOTOPE_SHARD.get());
            case AMETHYST -> resource.is(Items.AMETHYST_SHARD);
            default -> false;
        };
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> progress;
            case 1 -> PROCESS_TIME;
            case 2 -> acid.amount();
            case 3 -> acid.fluidCode();
            case 4 -> formed ? 1 : 0;
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 5;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        acid.serialize(out.child("acid"));
        out.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        acid.deserialize(in.childOrEmpty("acid"));
        progress = in.getIntOr("progress", 0);
        if (level != null) {
            syncedAt = level.getGameTime();
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CrystallizerMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
