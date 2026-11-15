package com.cyberfarm.block.entity;

import java.util.List;

import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.trinket.TrinketCharging;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Human Dock: the player's own dock, assembled with a Cyber Frame above the pad. Whoever stands on it gets their Energy
 * Sphere, Pocket Drone and every FE item they carry charged, from the dock's buffer, fed by cables or a Battery Bank.
 */
public class HumanDockBlockEntity extends SmallMultiblockBlockEntity {
    public static final int CAPACITY = 500_000;
    public static final int MAX_INPUT = 8_000;
    /** FE per tick it gives each player standing on it. */
    public static final int CHARGE_RATE = 4_000;
    /** Charges in bursts, each covering the ticks since the last: every write to a player's belt is synced. */
    private static final int INTERVAL = 10;
    public static final int OPEN_TICKS = 40;

    private boolean charging;

    public HumanDockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HUMAN_DOCK.get(), pos, state, 0, CAPACITY, MAX_INPUT, 0);
    }

    @Override
    protected boolean isWorking() {
        return charging;
    }

    /** The server must answer true, or the arch rising is not shown to the players around. */
    @Override
    public int openTicks() {
        return OPEN_TICKS;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        // Only once its frame is on and the arch is up
        if (!advanceStage(level, pos, state) || level.getGameTime() % INTERVAL != 0) {
            return;
        }
        // The pad is low: a player on it stands inside the block's space or just above it
        List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1.6, pos.getZ() + 1), player -> player.isAlive() && !player.isSpectator());
        boolean was = charging;
        charging = false;
        for (Player player : players) {
            int budget = Math.min(energy.getEnergy(), CHARGE_RATE * INTERVAL);
            if (budget <= 0) {
                break;
            }
            int used = TrinketCharging.chargePlayer(player, budget);
            if (used > 0) {
                energy.drain(used);
                charging = true;
            }
        }
        if (charging != was) {
            setChanged();
        }
    }



    /** It has no screen: a right-click does nothing (HumanDockBlock). */
    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return null;
    }
}
