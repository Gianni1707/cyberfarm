package com.cyberfarm.block.entity;

import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.menu.TrinketTableMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trinket Table: a workbench, once its 2x2 is assembled. Like a crafting table it takes no power and keeps nothing: its
 * GUI is a 3x3 grid (TrinketTableMenu, recipes in TrinketRecipes). Each thing made sets its arms going through one
 * work loop (TrinketTableRenderer), lit while they move.
 */
public class TrinketTableBlockEntity extends SmallMultiblockBlockEntity {
    private static final int OPEN_TICKS = 60;
    /** How long the arms work after something is made: one loop of TrinketTableRenderer. */
    public static final int WORK_TICKS = 100;

    private long workUntil;

    public TrinketTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRINKET_TABLE.get(), pos, state, 0, 0, 0, 0);
    }

    /** The server must answer true, or the opening is not shown to the players around. */
    @Override
    public int openTicks() {
        return OPEN_TICKS;
    }

    @Override
    protected boolean isWorking() {
        return level != null && level.getGameTime() < workUntil;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        advanceStage(level, pos, state);
    }

    /** Something was taken out of the grid: the arms go through their loop, lit at once rather than at the next status. */
    public void crafted() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        workUntil = server.getGameTime() + WORK_TICKS;
        BlockState state = getBlockState();
        if (state.getBlock() instanceof LitMachineBlock && !state.getValue(LitMachineBlock.LIT)) {
            server.setBlock(worldPosition, state.setValue(LitMachineBlock.LIT, true), Block.UPDATE_CLIENTS);
        }
        server.playSound(null, worldPosition, ModSounds.ASSEMBLER_WORK.get(), SoundSource.BLOCKS, 0.6F, 1.6F);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TrinketTableMenu(containerId, inventory, ContainerLevelAccess.create(level, worldPosition));
    }
}
