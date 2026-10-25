package com.cyberfarm.drone.job;

import java.util.List;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Fisher Module: hovers over open water in the work area and fishes like a rod would, with the
 * vanilla fishing loot and odds: fish, junk and treasure. Each catch goes into the cargo; the drone
 * flies home when it is full.
 */
public class FishJob extends DroneJob {
    public static final int ENERGY_COST = 10;
    /** A catch takes this long at random, about as long as a fishing rod waits. */
    private static final int MIN_WAIT = 100;
    private static final int MAX_WAIT = 300;
    /**
     * A rod's chance of treasure. The fishing table hands treasure only to a real bobber in open
     * water, so the drone rolls the treasure table itself this often; the fishing table then gives
     * fish and junk in their vanilla proportions.
     * shortcut: any water gives treasure, not only open water (5x5 around); check the water around if that matters
     */
    private static final float TREASURE_CHANCE = 0.05F;

    private final BlockPos water;
    private int waitTicks = -1;

    private FishJob(BlockPos water) {
        this.water = water;
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (drone.cargo().isFull()) {
            return null;
        }
        BlockPos water = nearest(drone, blocksIn(dock.getWorkArea()), pos -> isOpenWater(level, pos));
        return water != null ? new FishJob(water) : null;
    }

    /** A water source with air above it, so the line reaches the surface. */
    private static boolean isOpenWater(ServerLevel level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos).isSource() && level.getBlockState(pos.above()).isAir();
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (!isOpenWater(level, water)) {
            return Result.DONE;
        }
        return switch (travelTo(drone, water)) {
            case MOVING -> Result.CONTINUE;
            case FAILED -> Result.DONE;
            case ARRIVED -> fish(drone, level);
        };
    }

    private Result fish(DroneEntity drone, ServerLevel level) {
        if (waitTicks < 0) {
            waitTicks = MIN_WAIT + level.getRandom().nextInt(MAX_WAIT - MIN_WAIT + 1);
            level.playSound(null, water, SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F, 1.0F);
        }
        if (--waitTicks > 0) {
            return Result.CONTINUE;
        }
        Vec3 surface = Vec3.atCenterOf(water).add(0.0, 0.5, 0.0);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, surface)
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .create(LootContextParamSets.FISHING);
        ResourceKey<LootTable> table = level.getRandom().nextFloat() < TREASURE_CHANCE ? BuiltInLootTables.FISHING_TREASURE : BuiltInLootTables.FISHING;
        List<ItemStack> catches = level.getServer().reloadableRegistries().getLootTable(table).getRandomItems(params);
        drone.cargo().collect(level, water, catches);
        level.playSound(null, water, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.6F, 1.0F);
        level.sendParticles(ParticleTypes.SPLASH, surface.x, surface.y, surface.z, 12, 0.3, 0.05, 0.3, 0.1);
        drone.useJobEnergy(ENERGY_COST);
        return drone.cargo().isFull() ? Result.RETURN : Result.DONE;
    }
}
