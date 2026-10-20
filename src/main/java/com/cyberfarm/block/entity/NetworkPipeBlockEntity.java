package com.cyberfarm.block.entity;

import java.util.List;
import java.util.function.ToIntBiFunction;

import com.cyberfarm.block.NetworkPipeBlock;
import com.cyberfarm.energy.ConduitNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * Shared by every cable: caches the blocks its network reaches and hands out whatever enters,
 * taking turns between them so none starves.
 */
public abstract class NetworkPipeBlockEntity extends BlockEntity implements NetworkPipeBlock.Puller {
    private List<ConduitNetwork.Endpoint> endpoints = List.of();
    private int cachedVersion = -1;
    private int nextEndpoint;

    protected NetworkPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** How much can enter the network per insertion, i.e. per pushing block per tick: set by the tier. */
    protected int rate() {
        return getBlockState().getBlock() instanceof NetworkPipeBlock pipe ? pipe.rate() : 0;
    }

    private List<ConduitNetwork.Endpoint> endpoints() {
        if (cachedVersion != ConduitNetwork.version()) {
            endpoints = ConduitNetwork.findEndpoints(level, worldPosition);
            cachedVersion = ConduitNetwork.version();
        }
        return endpoints;
    }

    /**
     * Offers {@code amount} to the network's blocks, starting one further along on every call and
     * skipping the blocks a pipe pulls from. {@code insert} gives one receiver what is left and
     * returns how much it took.
     */
    protected <H> int distribute(BlockCapability<H, Direction> capability, H self, int amount, ToIntBiFunction<H, Integer> insert) {
        if (level == null || amount <= 0) {
            return 0;
        }
        List<ConduitNetwork.Endpoint> targets = endpoints();
        int count = targets.size();
        if (count == 0) {
            return 0;
        }
        int start = Math.floorMod(nextEndpoint++, count);
        int moved = 0;
        for (int i = 0; i < count && moved < amount; i++) {
            ConduitNetwork.Endpoint target = targets.get((start + i) % count);
            if (target.source()) {
                continue;
            }
            H receiver = level.getCapability(capability, target.pos(), target.side());
            if (receiver != null && receiver != self) {
                moved += insert.applyAsInt(receiver, amount - moved);
            }
        }
        return moved;
    }
}
