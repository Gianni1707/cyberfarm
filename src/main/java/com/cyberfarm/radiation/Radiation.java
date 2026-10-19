package com.cyberfarm.radiation;

import java.util.HashSet;
import java.util.Set;

import com.cyberfarm.Config;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.block.entity.ReactorPortBlockEntity;
import com.cyberfarm.item.HazmatSuitItem;
import com.cyberfarm.registry.ModEffects;
import com.cyberfarm.registry.ModItems;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Nuclear contamination. Untreated waste, a leaking reactor or a meltdown add radiation to the chunk
 * they are in; it fades away by itself over minutes. Living things in a contaminated chunk get
 * Radiation Sickness, and very contaminated land withers.
 */
public final class Radiation {
    /** Radiation added every scan (5 s) per Depleted Fuel Rod carried, stored or lying on the ground. */
    public static final float PER_ROD = 2.5F;
    public static final float REACTOR_LEAK = 5.0F;
    public static final float MELTDOWN = 400.0F;
    /** From these levels: sickness, stronger sickness, worst sickness and withering land. */
    public static final float SICK = 20.0F;
    public static final float SICKER = 100.0F;
    public static final float SICKEST = 300.0F;
    public static final float WITHER = 50.0F;

    private static final int SCAN_INTERVAL = 100;
    private static final int EXPOSE_INTERVAL = 40;
    /** Containers are scanned this many chunks around each player: waste only matters where people are. */
    private static final int SCAN_RADIUS = 2;

    private Radiation() {}

    public static void add(ServerLevel level, BlockPos pos, float amount) {
        RadiationData.get(level).add(pos, amount);
    }

    /** A meltdown contaminates a 5x5 chunk area around the core, worst at the centre. */
    public static void meltdown(ServerLevel level, BlockPos core) {
        RadiationData data = RadiationData.get(level);
        ChunkPos centre = ChunkPos.containing(core);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                data.add(ChunkPos.pack(centre.x() + dx, centre.z() + dz), MELTDOWN / (1 + Math.max(Math.abs(dx), Math.abs(dz))));
            }
        }
    }

    public static float level(ServerLevel level, BlockPos pos) {
        return RadiationData.get(level).level(pos);
    }

    public static boolean isWaste(ItemStack stack) {
        return stack.is(ModItems.DEPLETED_FUEL_ROD.get());
    }

    /** Share of radiation the entity's armor keeps out: 25% per Hazmat Suit piece, all of it with the full suit. */
    public static float protection(LivingEntity entity) {
        int pieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (entity.getItemBySlot(slot).getItem() instanceof HazmatSuitItem) {
                pieces++;
            }
        }
        return pieces / 4.0F;
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long time = level.getGameTime();
        if (time % SCAN_INTERVAL == 0) {
            RadiationData.get(level).decay();
            scanWaste(level);
        }
        if (time % EXPOSE_INTERVAL == 0) {
            exposeAndWither(level);
        }
    }

    /** Waste carried by players and stored in containers near them (rods on the ground count themselves). */
    private static void scanWaste(ServerLevel level) {
        RadiationData data = RadiationData.get(level);
        Set<Long> scanned = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            data.add(player.blockPosition(), PER_ROD * countWaste(player.getInventory()));
            ChunkPos home = ChunkPos.containing(player.blockPosition());
            for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    long chunkPos = ChunkPos.pack(home.x() + dx, home.z() + dz);
                    LevelChunk chunk = level.getChunkSource().getChunkNow(home.x() + dx, home.z() + dz);
                    if (chunk == null || !scanned.add(chunkPos)) {
                        continue;
                    }
                    for (BlockEntity entity : chunk.getBlockEntities().values()) {
                        data.add(chunkPos, PER_ROD * countStoredWaste(level, entity));
                    }
                }
            }
        }
    }

    /**
     * Waste stored in a block: vanilla containers directly (each half of a double chest once), any
     * other mod's storage through its item capability. CyberFarm machines shield the waste they hold.
     */
    private static long countStoredWaste(ServerLevel level, BlockEntity entity) {
        if (entity instanceof Container container) {
            return countWaste(container);
        }
        if (entity instanceof MachineBlockEntity || entity instanceof ReactorPortBlockEntity) {
            return 0;
        }
        // shortcut: storage networks that expose the same items through several blocks count them more than once; dedupe by handler if that proves too harsh
        ResourceHandler<ItemResource> items = level.getCapability(Capabilities.Item.BLOCK, entity.getBlockPos(), entity.getBlockState(), entity, null);
        if (items == null) {
            return 0;
        }
        long rods = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.getResource(i).is(ModItems.DEPLETED_FUEL_ROD.get())) {
                rods += items.getAmountAsLong(i);
            }
        }
        return rods;
    }

    private static int countWaste(Container container) {
        int rods = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (isWaste(stack)) {
                rods += stack.getCount();
            }
        }
        return rods;
    }

    private static void exposeAndWither(ServerLevel level) {
        boolean wither = Config.RADIATION_WORLD_EFFECTS.get();
        for (Long2FloatMap.Entry entry : RadiationData.get(level).levels().long2FloatEntrySet()) {
            float radiation = entry.getFloatValue();
            int cx = ChunkPos.getX(entry.getLongKey());
            int cz = ChunkPos.getZ(entry.getLongKey());
            if (radiation < SICK || level.getChunkSource().getChunkNow(cx, cz) == null) {
                continue;
            }
            AABB box = new AABB(cx << 4, level.getMinY(), cz << 4, (cx << 4) + 16, level.getMaxY(), (cz << 4) + 16);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                expose(entity, radiation);
            }
            if (wither && radiation >= WITHER) {
                witherLand(level, cx, cz, radiation);
            }
        }
    }

    private static void expose(LivingEntity entity, float radiation) {
        float dose = radiation * (1.0F - protection(entity));
        if (dose < SICK) {
            return;
        }
        int amplifier = dose >= SICKEST ? 2 : dose >= SICKER ? 1 : 0;
        entity.addEffect(new MobEffectInstance(ModEffects.RADIATION_SICKNESS, 120, amplifier));
    }

    /** A few random surface blocks of the chunk die: grass dries up, crops wilt, leaves fall. */
    private static void witherLand(ServerLevel level, int cx, int cz, float radiation) {
        RandomSource random = level.getRandom();
        int attempts = Math.min(8, (int) (radiation / WITHER));
        for (int i = 0; i < attempts; i++) {
            int x = (cx << 4) + random.nextInt(16);
            int z = (cz << 4) + random.nextInt(16);
            BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
            BlockState state = level.getBlockState(top);
            if (state.is(BlockTags.LEAVES)) {
                level.destroyBlock(top, false);
            } else if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.FARMLAND)) {
                level.setBlockAndUpdate(top, Blocks.DIRT.defaultBlockState());
            }
            // Plants sit on top of the motion-blocking surface
            BlockPos plant = top.above();
            BlockState plantState = level.getBlockState(plant);
            if (plantState.getBlock() instanceof VegetationBlock && !plantState.is(Blocks.DEAD_BUSH)) {
                level.setBlockAndUpdate(plant, Blocks.DEAD_BUSH.defaultBlockState());
            }
        }
    }
}
