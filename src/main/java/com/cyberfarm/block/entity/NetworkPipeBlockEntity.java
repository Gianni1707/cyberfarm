package com.cyberfarm.block.entity;

import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import com.cyberfarm.port.PortBlockEntity;

import java.util.List;
import java.util.function.ToIntBiFunction;

import com.cyberfarm.block.NetworkPipeBlock;
import com.cyberfarm.energy.ConduitNetwork;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import com.cyberfarm.port.Caps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import com.cyberfarm.port.ValueInput;

/**
 * Shared by every cable: caches the blocks its network reaches and hands out whatever enters,
 * taking turns between them so none starves. While something goes through, every pipe of the network shows it behind its glass.
 */
public abstract class NetworkPipeBlockEntity extends PortBlockEntity implements NetworkPipeBlock.Puller {
    /** What the pipe's model draws behind its glass while something goes through: a {@link Fluid}, or {@link #ENERGY}. */
    public static final ModelProperty<Object> SHOWN = new ModelProperty<>();
    public static final Object ENERGY = "energy";
    /**
     * How long after the last transfer the glass empties, checked by a scheduled tick; also how long a fluid shows before another may take
     * its place, or two fluids taking turns on one network would rebuild its chunks' meshes every tick.
     */
    private static final int HOLD_TICKS = 40;

    private ConduitNetwork.Network network = new ConduitNetwork.Network(List.of(), java.util.Set.of());
    private int cachedVersion = -1;
    private int nextEndpoint;
    private @Nullable Object shown;
    private long shownSince = Long.MIN_VALUE / 2;
    private long lastMoved = Long.MIN_VALUE / 2;
    private int shownVersion = -1;

    protected NetworkPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** How much can enter the network per insertion, i.e. per pushing block per tick: set by the tier. */
    protected int rate() {
        return getBlockState().getBlock() instanceof NetworkPipeBlock pipe ? pipe.rate() : 0;
    }

    protected ConduitNetwork.Network network() {
        if (cachedVersion != ConduitNetwork.version()) {
            network = ConduitNetwork.find(level, worldPosition);
            cachedVersion = ConduitNetwork.version();
        }
        return network;
    }

    /**
     * Offers {@code amount} to the network's blocks, starting one further along on every call and
     * skipping the blocks a pipe pulls from. {@code insert} gives one receiver what is left and
     * returns how much it took.
     */
    protected <H> int distribute(Capability<H> capability, H self, int amount, ToIntBiFunction<H, Integer> insert) {
        if (level == null || amount <= 0) {
            return 0;
        }
        List<ConduitNetwork.Endpoint> targets = network().endpoints();
        int count = targets.size();
        if (count == 0) {
            return 0;
        }
        int start = Math.floorMod(nextEndpoint++, count);
        int moved = 0;
        for (int i = 0; i < count && moved < amount; i++) {
            ConduitNetwork.Endpoint target = targets.get((start + i) % count);
            // Nor a block whose chunk went away since the network was walked: asking it would load the chunk again
            if (target.source() || !level.isLoaded(target.pos())) {
                continue;
            }
            H receiver = Caps.find(capability, level, target.pos(), target.side());
            if (receiver != null && receiver != self) {
                moved += insert.applyAsInt(receiver, amount - moved);
            }
        }
        return moved;
    }

    /**
     * Something just went through this pipe: every pipe of the network shows it. Runs the network over only when what is shown or the
     * network changes, so a steady flow costs one comparison a tick.
     * shortcut: a simulated transfer counts too, so a machine that only asks can light the line for 2 s; if that shows, wait for the commit.
     */
    protected void flowed(Object what) {
        long now = level.getGameTime();
        lastMoved = now;
        boolean changed = shown != what;
        if ((!changed && shownVersion == ConduitNetwork.version()) || (changed && shown != null && now - shownSince < HOLD_TICKS)) {
            return;
        }
        shownVersion = ConduitNetwork.version();
        showAll(what, now);
        level.scheduleTick(worldPosition, getBlockState().getBlock(), HOLD_TICKS + 1);
    }

    /**
     * From the pipe's scheduled tick: empties the network's glass once nothing has gone through any of its pipes for {@link #HOLD_TICKS}.
     * shortcut: if the pipe watching the flow is broken while it runs and nothing flows after, the rest stays lit until the next transfer.
     */
    public void checkFlow() {
        long now = level.getGameTime();
        long last = lastMoved;
        for (BlockPos pos : network().pipes()) {
            if (level.getBlockEntity(pos) instanceof NetworkPipeBlockEntity pipe) {
                last = Math.max(last, pipe.lastMoved);
            }
        }
        if (now - last <= HOLD_TICKS) {
            level.scheduleTick(worldPosition, getBlockState().getBlock(), (int) (last + HOLD_TICKS + 1 - now));
        } else {
            showAll(null, now);
        }
    }

    private void showAll(@Nullable Object what, long now) {
        for (BlockPos pos : network().pipes()) {
            if (level.getBlockEntity(pos) instanceof NetworkPipeBlockEntity pipe && pipe.shown != what) {
                pipe.shown = what;
                pipe.shownSince = now;
                level.sendBlockUpdated(pos, pipe.getBlockState(), pipe.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public ModelData getModelData() {
        return shown == null ? ModelData.EMPTY : ModelData.builder().with(SHOWN, shown).build();
    }

    /** Only the client hears what is shown: it is not saved, so a loaded world starts with empty pipes until something flows. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (shown instanceof Fluid fluid) {
            tag.putString("shown", BuiltInRegistries.FLUID.getKey(fluid).toString());
        } else if (shown == ENERGY) {
            tag.putString("shown", (String) ENERGY);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Object loaded = input.getString("shown")
                .<Object>map(name -> name.equals(ENERGY) ? ENERGY : BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(name)))
                .orElse(null);
        if (loaded != shown) {
            shown = loaded;
            if (level != null && level.isClientSide()) {
                requestModelDataUpdate();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
            }
        }
    }
}
