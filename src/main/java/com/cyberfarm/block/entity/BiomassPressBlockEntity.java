package com.cyberfarm.block.entity;

import net.minecraft.world.level.block.ComposterBlock;

import com.cyberfarm.port.SlotRulesItemHandler;

import net.minecraftforge.items.IItemHandler;

import com.cyberfarm.block.BiomassPressBlock;
import com.cyberfarm.menu.BiomassPressMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.cyberfarm.port.ValueInput;
import com.cyberfarm.port.ValueOutput;
import com.cyberfarm.port.Caps;
import com.cyberfarm.port.ItemMoves;
import com.cyberfarm.port.ItemResource;

/**
 * Biomass Press: presses organic waste (anything a composter takes, plus rotten flesh) into Biomass, with
 * the chance a composter would take it, and pushes it out of its nozzle into the container there. Needs no power.
 */
public class BiomassPressBlockEntity extends SmallMultiblockBlockEntity {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    public static final int PROCESS_TIME = 40;
    private static final float ROTTEN_FLESH_CHANCE = 0.8F;
    /** Ticks between Biomass going out of the nozzle, a hopper's pace. */
    private static final int PUSH_INTERVAL = 8;
    private static final int OPEN_TICKS = 50;
    private static final int SYNC_INTERVAL = 5;

    private int progress;
    /** What the clients last heard, for the mash in the chamber. */
    private int syncedContents = -1;

    /** What automation sees: waste goes in, only Biomass comes out. */
    private final IItemHandler automationItems = new SlotRulesItemHandler(items, slot -> slot == INPUT, slot -> slot == OUTPUT);

    public BiomassPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIOMASS_PRESS.get(), pos, state, 2, 0, 0, 0);
    }

    /** The chance an item gives a Biomass, as in a composter; 0 if it is not organic. */
    public static float biomassChance(ItemStack stack) {
        if (stack.is(Items.ROTTEN_FLESH)) {
            return ROTTEN_FLESH_CHANCE;
        }
        return ComposterBlock.COMPOSTABLES.getFloat(stack.getItem());
    }

    /** How much is inside, waste and Biomass, which the mash in the chamber shows. */
    public int contents() {
        return getStack(INPUT).getCount() + getStack(OUTPUT).getCount();
    }

    public IItemHandler automationItems() {
        return automationItems;
    }

    @Override
    protected float progress() {
        return progress / (float) PROCESS_TIME;
    }

    @Override
    protected boolean isWorking() {
        return progress > 0;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean open = advanceStage(level, pos, state);
        ItemStack input = getStack(INPUT);
        float chance = biomassChance(input);
        if (open && chance > 0.0F && hasRoomFor(OUTPUT, ModItems.BIOMASS.get())) {
            if (++progress >= PROCESS_TIME) {
                progress = 0;
                setStack(INPUT, input.copyWithCount(input.getCount() - 1));
                if (level.getRandom().nextFloat() < chance) {
                    addOne(OUTPUT, ModItems.BIOMASS.get());
                }
                level.playSound(null, pos, ModSounds.BIOMASS_PRESS_WORK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            setChanged();
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }
        if (level.getGameTime() % PUSH_INTERVAL == 0 && !getStack(OUTPUT).isEmpty()) {
            Direction nozzle = BiomassPressBlock.nozzle(state);
            IItemHandler target = Caps.items(level, pos.relative(nozzle), nozzle.getOpposite());
            if (target != null) {
                ItemMoves.move(automationItems, target, resource -> true, 1, false);
            }
        }
        if (level.getGameTime() % SYNC_INTERVAL == 0 && contents() != syncedContents) {
            syncedContents = contents();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public int openTicks() {
        return OPEN_TICKS;
    }

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return index == INPUT && biomassChance(resource.toStack()) > 0.0F;
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> progress;
            case 1 -> PROCESS_TIME;
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 2;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("progress", 0);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BiomassPressMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
