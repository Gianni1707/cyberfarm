package com.cyberfarm.block.entity;

import com.cyberfarm.menu.ElectrolyzerMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Electrolyzer: turns Alumina (ours or any mod's, by the common tag) into Aluminum Ingots with a
 * steady flow of FE, the way aluminum is really made. The first machine a drone frame needs.
 */
public class ElectrolyzerBlockEntity extends SmallMultiblockBlockEntity {
    private static final int OPEN_TICKS = 60;

    public static final TagKey<Item> ALUMINA = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dusts/alumina"));
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    public static final int PROCESS_TIME = 100;
    public static final int ENERGY_PER_TICK = 80;
    private static final int CAPACITY = 20_000;
    private static final int MAX_INPUT = 500;

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTROLYZER.get(), pos, state, 2, CAPACITY, MAX_INPUT, 0);
    }

    public static boolean isAlumina(ItemResource resource) {
        return resource.toStack().is(ALUMINA);
    }

    @Override
    protected float progress() {
        return progress / (float) workTime(PROCESS_TIME);
    }

    @Override
    public int openTicks() {
        return OPEN_TICKS;
    }

    @Override
    public boolean takesAddons() {
        return true;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean open = advanceStage(level, pos, state);
        boolean canRun = open && getStack(INPUT).is(ALUMINA) && hasRoomFor(OUTPUT, ModItems.ALUMINUM_INGOT.get());
        if (work(canRun, ENERGY_PER_TICK, PROCESS_TIME)) {
            take(INPUT, 1);
            addOne(OUTPUT, ModItems.ALUMINUM_INGOT.get());
            level.playSound(null, pos, ModSounds.COOLANT_MIXER_WORK.get(), SoundSource.BLOCKS, 0.8F, 1.3F);
        }
    }

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return index == INPUT && isAlumina(resource);
    }

    /** Automation takes out only what the machine made. */
    @Override
    protected boolean isOutput(int index) {
        return index == OUTPUT;
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> progress;
            case 1 -> workTime(PROCESS_TIME);
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 2;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ElectrolyzerMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
