package com.cyberfarm.block.entity;

import java.util.List;

import com.cyberfarm.block.CyberFrameBlock;
import com.cyberfarm.block.MethaneExtractorBlock;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.item.MobCapsuleItem;
import com.cyberfarm.menu.MethaneExtractorMenu;
import com.cyberfarm.multiblock.Blueprint;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Methane Extractor: the "shower head" on top of a 3x3x4 chamber. It draws Methane from the mob
 * shut inside: zombies give the most, other monsters less, animals little. The chamber is
 *
 * <pre>
 *   roof  (y)    : 8 Cyber Frames around this extractor
 *   walls (y-1, y-2): Cyber Frame corners, glass in the middle of each side, empty centre
 *   floor (y-3)  : 3x3 Cyber Frames
 * </pre>
 */
public class MethaneExtractorBlockEntity extends MachineBlockEntity {
    public static final int TANK_CAPACITY = 8_000;
    public static final int ZOMBIE_RATE = 10;
    public static final int MONSTER_RATE = 5;
    public static final int ANIMAL_RATE = 2;
    private static final int OUTPUT_PER_SIDE = 200;
    private static final int CHECK_INTERVAL = 20;
    private static final int WORK_SOUND_INTERVAL = 50;

    private final GasTank tank = new GasTank(TANK_CAPACITY, resource -> false, this::setChanged);
    private boolean formed;
    private int mobCount;
    private int rate;
    private int checkCooldown;

    public MethaneExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METHANE_EXTRACTOR.get(), pos, state, 0, 0, 0, 0);
    }

    public GasTank getTank() {
        return tank;
    }

    /** Checks the 3x3x4 chamber below an extractor sitting at {@code top}, as laid out by its {@link Blueprint}. */
    public static boolean isStructureValid(Level level, BlockPos top) {
        return Blueprint.METHANE_CHAMBER.ghosts(top, Direction.NORTH).stream().allMatch(ghost -> ghost.ok().test(level.getBlockState(ghost.pos())));
    }

    /** Lights up (or turns off) the Cyber Frames of the chamber under {@code top}. */
    private static void setFramesFormed(Level level, BlockPos top, boolean formed) {
        for (BlockPos pos : BlockPos.betweenClosed(top.offset(-1, -3, -1), top.offset(1, 0, 1))) {
            BlockState frame = level.getBlockState(pos);
            if (frame.is(ModBlocks.CYBER_FRAME.get()) && frame.getValue(CyberFrameBlock.FORMED) != formed) {
                level.setBlock(pos, frame.setValue(CyberFrameBlock.FORMED, formed), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** The 1x1x2 space in the middle of the chamber. */
    private AABB chamber() {
        return new AABB(worldPosition.getX(), worldPosition.getY() - 2, worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY(), worldPosition.getZ() + 1);
    }

    private List<Mob> mobsInside(Level level) {
        return level.getEntitiesOfClass(Mob.class, chamber(), Entity::isAlive);
    }

    public static int rateOf(Mob mob) {
        if (mob.getType().builtInRegistryHolder().is(EntityTypeTags.ZOMBIES)) {
            return ZOMBIE_RATE;
        }
        if (mob instanceof Enemy) {
            return MONSTER_RATE;
        }
        return mob instanceof Animal ? ANIMAL_RATE : 0;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (--checkCooldown <= 0) {
            checkCooldown = CHECK_INTERVAL;
            formed = isStructureValid(level, pos);
            boolean changed = state.getValue(MethaneExtractorBlock.FORMED) != formed;
            if (changed) {
                level.setBlock(pos, state.setValue(MethaneExtractorBlock.FORMED, formed), Block.UPDATE_ALL);
                level.playSound(null, pos, (formed ? ModSounds.EXTRACTOR_ACTIVATE : ModSounds.EXTRACTOR_DEACTIVATE).get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            // Re-applied while formed, so a frame shared with a broken neighbour chamber lights up again
            if (formed || changed) {
                setFramesFormed(level, pos, formed);
            }
            List<Mob> mobs = formed ? mobsInside(level) : List.of();
            mobCount = mobs.size();
            rate = mobs.stream().mapToInt(MethaneExtractorBlockEntity::rateOf).sum();
        }
        // Bubbles from the chamber every couple of seconds while gas is actually being drawn
        if (rate > 0 && tank.fill(ModFluids.METHANE.get(), rate) > 0 && level.getGameTime() % WORK_SOUND_INTERVAL == 0) {
            level.playSound(null, pos.below(2), ModSounds.EXTRACTOR_WORK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        tank.pushToNeighbors(level, pos, OUTPUT_PER_SIDE);
    }

    /** Lets the mob in a Mob Capsule out inside the chamber. Returns a message key on failure, null on success. */
    public String releaseFromCapsule(ServerLevel level, ItemStack capsule) {
        if (!isStructureValid(level, worldPosition)) {
            return "message.cyberfarm.extractor.incomplete";
        }
        if (!mobsInside(level).isEmpty()) {
            return "message.cyberfarm.extractor.occupied";
        }
        if (MobCapsuleItem.release(level, capsule, Vec3.atBottomCenterOf(worldPosition.below(2))) == null) {
            return "message.cyberfarm.capsule.refused";
        }
        checkCooldown = 0;
        return null;
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> formed ? 1 : 0;
            case 1 -> mobCount;
            case 2 -> rate;
            case 3 -> tank.amount();
            case 4 -> tank.fluidCode();
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 5;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            setFramesFormed(level, pos, false);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("tank"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MethaneExtractorMenu(containerId, inventory, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
