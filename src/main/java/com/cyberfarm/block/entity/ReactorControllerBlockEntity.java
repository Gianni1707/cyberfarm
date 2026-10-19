package com.cyberfarm.block.entity;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.Config;
import com.cyberfarm.block.CyberFrameBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.ReactorControllerBlock;
import com.cyberfarm.block.ReactorPortBlock;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.fluid.TankGroup;
import com.cyberfarm.item.FuelRodItem;
import com.cyberfarm.menu.ReactorMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Fission Reactor controller: the brain of a cooling-tower multiblock, 5x5 at the base and 7 high.
 * Burning Fuel Rods heat the core; water from the ports absorbs the heat and leaves as Steam,
 * which the ports push out to Steam Turbines. Seen from the side, relative to the floor centre:
 *
 * <pre>
 *   y6   ■ ■ ■ ■ ■    open rim
 *   y3-5   ■ ■ ■      3x3 waist, hollow
 *   y1-2 ■ ■ ■ ■ ■    5x5 ring with the controller (y1, middle of a side) and ports; 3x3 core inside
 *   y0   ■ ■ ■ ■ ■    floor
 * </pre>
 * Shell blocks are Reactor Casings or Reactor Ports; the inside must be empty.
 */
public class ReactorControllerBlockEntity extends MachineBlockEntity {
    public static final int FUEL_SLOTS = 4;
    public static final int WASTE_SLOTS = 4;
    public static final int TANK_CAPACITY = 16_000;
    /** Heat of one rod per tick at 100% power; each mB of water absorbs one unit and becomes one mB of steam. */
    public static final int HEAT_PER_ROD = 50;
    public static final int BOOST_STEP = 50;
    public static final int MAX_BOOST = 500;
    /** Heat the primary loop moves to the water per tick with plain water; coolants multiply it. */
    public static final int BASE_TRANSFER = 200;
    public static final double COOLANT_FACTOR = 2.5;
    public static final double CRYO_FACTOR = 6.0;
    /** Heat moved per mB of coolant worn out. */
    private static final int COOLANT_WEAR = 400;
    public static final int MELTDOWN_HEAT = 200_000;
    public static final int ALARM_PERMILLE = 800;
    private static final int PASSIVE_COOLING = 20;
    private static final int PORT_OUTPUT = 1_000;
    private static final int CHECK_INTERVAL = 20;
    private static final int HUM_INTERVAL = 80;

    private final GasTank water = new GasTank(TANK_CAPACITY, resource -> resource.is(Fluids.WATER.getFluidType()), this::setChanged);
    private final GasTank steam = new GasTank(TANK_CAPACITY, resource -> false, this::setChanged);
    /** Primary loop: Coolant or Cryo Coolant; empty means plain water. */
    private final GasTank coolant = new GasTank(TANK_CAPACITY,
            resource -> resource.is(ModFluids.COOLANT_TYPE.get()) || resource.is(ModFluids.CRYO_COOLANT_TYPE.get()), this::setChanged);
    private final TankGroup automationFluids = TankGroup.of(new GasTank[] {water, coolant}, steam);
    private List<BlockPos> ports = List.of();
    private int checkCooldown;
    /** Burn progress in percent-ticks: a rod loses a second of fuel every 2000, so faster at higher power. */
    private int burnProgress;
    private int activeRods;
    private int steamRate;
    private int heat;
    private int boost;
    private int coolantWear;

    public ReactorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_CONTROLLER.get(), pos, state, FUEL_SLOTS + WASTE_SLOTS, 0, 0, 0);
    }

    // ---------------------------------------------------------------- structure

    private enum Shape { SHELL, EMPTY, ANY }

    /** Floor centre of the tower: two blocks behind the controller's face, one block down. */
    private static BlockPos floorCentre(BlockPos controller, Direction facing) {
        return controller.relative(facing.getOpposite(), 2).below();
    }

    private static Shape shapeAt(int dx, int dy, int dz) {
        int ring = Math.max(Math.abs(dx), Math.abs(dz));
        return switch (dy) {
            case 0 -> Shape.SHELL;
            case 3, 4, 5 -> ring == 2 ? Shape.ANY : ring == 1 ? Shape.SHELL : Shape.EMPTY;
            default -> ring == 2 ? Shape.SHELL : Shape.EMPTY;
        };
    }

    private static boolean isShellBlock(BlockState state) {
        return state.is(ModBlocks.REACTOR_CASING.get()) || state.is(ModBlocks.REACTOR_PORT.get());
    }

    /** The ports of a complete tower, or null when the structure is not complete. */
    private static @Nullable List<BlockPos> scan(Level level, BlockPos controller, Direction facing) {
        BlockPos floor = floorCentre(controller, facing);
        List<BlockPos> found = new ArrayList<>();
        for (int dy = 0; dy <= 6; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = floor.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    switch (shapeAt(dx, dy, dz)) {
                        case SHELL -> {
                            if (pos.equals(controller)) {
                                continue;
                            }
                            if (!isShellBlock(state)) {
                                return null;
                            }
                            if (state.is(ModBlocks.REACTOR_PORT.get())) {
                                found.add(pos.immutable());
                            }
                        }
                        case EMPTY -> {
                            if (!state.isAir()) {
                                return null;
                            }
                        }
                        case ANY -> {}
                    }
                }
            }
        }
        return found;
    }

    /** Whether {@code pos} is a shell block of the tower driven by this controller. */
    private static boolean inShell(BlockPos controller, Direction facing, BlockPos pos) {
        BlockPos rel = pos.subtract(floorCentre(controller, facing));
        return Math.abs(rel.getX()) <= 2 && Math.abs(rel.getZ()) <= 2 && rel.getY() >= 0 && rel.getY() <= 6
                && shapeAt(rel.getX(), rel.getY(), rel.getZ()) == Shape.SHELL;
    }

    /**
     * Right-clicking a casing or port of a formed tower opens its controller. The controller sits on
     * the second layer, so it is at most 4 blocks away sideways and between 5 below and 1 above.
     */
    public static InteractionResult openFromShell(Level level, BlockPos clicked, Player player) {
        for (BlockPos pos : BlockPos.betweenClosed(clicked.offset(-4, -5, -4), clicked.offset(4, 1, 4))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ReactorControllerBlock && state.getValue(ReactorControllerBlock.FORMED)
                    && inShell(pos, state.getValue(MachineBlock.FACING), clicked)) {
                if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ReactorControllerBlockEntity controller) {
                    player.openMenu(controller);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    /** Lights up (or turns off) every casing and port of the tower. */
    private void setShellFormed(Level level, Direction facing, boolean formed) {
        BlockPos floor = floorCentre(worldPosition, facing);
        for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-2, 0, -2), floor.offset(2, 6, 2))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.REACTOR_CASING.get()) && state.getValue(CyberFrameBlock.FORMED) != formed) {
                level.setBlock(pos, state.setValue(CyberFrameBlock.FORMED, formed), Block.UPDATE_CLIENTS);
            } else if (state.is(ModBlocks.REACTOR_PORT.get()) && state.getValue(ReactorPortBlock.FORMED) != formed) {
                // Full update, so pipes and cables next to the port connect or disconnect
                level.setBlock(pos, state.setValue(ReactorPortBlock.FORMED, formed), Block.UPDATE_ALL);
            }
        }
    }

    private void setPorts(Level level, List<BlockPos> newPorts) {
        for (BlockPos port : ports) {
            if (!newPorts.contains(port) && level.getBlockEntity(port) instanceof ReactorPortBlockEntity entity) {
                entity.setController(null);
            }
        }
        for (BlockPos port : newPorts) {
            if (level.getBlockEntity(port) instanceof ReactorPortBlockEntity entity) {
                entity.setController(worldPosition);
            }
        }
        ports = newPorts;
    }

    public boolean isFormed() {
        return getBlockState().getValue(ReactorControllerBlock.FORMED);
    }

    // ---------------------------------------------------------------- tick

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(MachineBlock.FACING);
        if (--checkCooldown <= 0) {
            checkCooldown = CHECK_INTERVAL;
            List<BlockPos> found = scan(level, pos, facing);
            boolean formed = found != null;
            // Ports first, so their pipes find the capabilities when the shell update reaches them
            setPorts(level, formed ? found : List.of());
            if (formed != state.getValue(ReactorControllerBlock.FORMED)) {
                state = state.setValue(ReactorControllerBlock.FORMED, formed).setValue(ReactorControllerBlock.LIT, false);
                level.setBlock(pos, state, Block.UPDATE_ALL);
                level.playSound(null, pos, (formed ? ModSounds.REACTOR_ACTIVATE : ModSounds.REACTOR_DEACTIVATE).get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                setShellFormed(level, facing, formed);
            } else if (formed) {
                // Re-applied while formed, for casings placed back or shared with a broken neighbour
                setShellFormed(level, facing, true);
            }
            moveSpentRods();
        }
        heat = Math.max(0, heat - PASSIVE_COOLING);
        if (!state.getValue(ReactorControllerBlock.FORMED)) {
            steamRate = 0;
            activeRods = 0;
            return;
        }

        activeRods = 0;
        for (int i = 0; i < FUEL_SLOTS; i++) {
            ItemStack rod = getStack(i);
            if (rod.getItem() instanceof FuelRodItem && !FuelRodItem.isSpent(rod)) {
                activeRods++;
            }
        }
        // Fission runs whatever the cooling: that is where the danger is
        int produced = activeRods * HEAT_PER_ROD * boost / 100;
        heat += produced;
        if (produced > 0) {
            burnProgress += boost;
            if (burnProgress >= 2_000) {
                burnProgress -= 2_000;
                burnRods();
            }
        }

        // The primary loop carries heat to the water, which leaves as steam
        int absorbed = Math.min(Math.min(heat, transferCapacity()), Math.min(water.amount(), steam.capacity() - steam.amount()));
        if (absorbed > 0) {
            water.drain(absorbed);
            steam.fill(ModFluids.STEAM.get(), absorbed);
            heat -= absorbed;
            if (coolant.amount() > 0) {
                coolantWear += absorbed;
                coolant.drain(coolantWear / COOLANT_WEAR);
                coolantWear %= COOLANT_WEAR;
            }
        }
        steamRate = absorbed;

        if (heat >= MELTDOWN_HEAT) {
            meltdown(level, pos, facing);
            return;
        }
        if (temperaturePermille() >= ALARM_PERMILLE && level.getGameTime() % 20 == 0) {
            level.playSound(null, pos, ModSounds.REACTOR_ALARM.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        }
        boolean running = produced > 0;
        if (running && level.getGameTime() % HUM_INTERVAL == 0) {
            level.playSound(null, floorCentre(pos, facing).above(3), ModSounds.REACTOR_HUM.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (running != state.getValue(ReactorControllerBlock.LIT)) {
            level.setBlock(pos, state.setValue(ReactorControllerBlock.LIT, running), Block.UPDATE_CLIENTS);
        }
        for (BlockPos port : ports) {
            steam.pushToNeighbors(level, port, PORT_OUTPUT);
        }
    }

    /** Heat the primary loop can move per tick: plain water, or more with Coolant or Cryo Coolant. */
    public int transferCapacity() {
        FluidResource loop = coolant.getResource(0);
        double factor = coolant.amount() == 0 ? 1.0
                : loop.is(ModFluids.CRYO_COOLANT_TYPE.get()) ? CRYO_FACTOR : COOLANT_FACTOR;
        return (int) (BASE_TRANSFER * factor);
    }

    public int temperaturePermille() {
        return (int) Math.min(1000L, (long) heat * 1000 / MELTDOWN_HEAT);
    }

    /** Power from the GUI, in steps of {@link #BOOST_STEP}%, from 0 (control rods in) to {@link #MAX_BOOST}%. */
    public void changeBoost(int steps) {
        boost = Math.clamp(boost + steps * BOOST_STEP, 0, MAX_BOOST);
        setChanged();
    }

    /** The core overheated: the rods melt and the core blows up (power and block damage from the common config). */
    private void meltdown(ServerLevel level, BlockPos pos, Direction facing) {
        heat = 0;
        boost = 0;
        for (int i = 0; i < FUEL_SLOTS; i++) {
            setStack(i, ItemStack.EMPTY);
        }
        BlockPos core = floorCentre(pos, facing).above();
        level.explode(null, core.getX() + 0.5, core.getY() + 0.5, core.getZ() + 0.5,
                Config.MELTDOWN_EXPLOSION_POWER.get().floatValue(),
                Config.MELTDOWN_DESTROYS_BLOCKS.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    /** One second of burning: every active rod loses a point of fuel. */
    private void burnRods() {
        for (int i = 0; i < FUEL_SLOTS; i++) {
            ItemStack rod = getStack(i);
            if (rod.getItem() instanceof FuelRodItem && !FuelRodItem.isSpent(rod)) {
                ItemStack burnt = rod.copy();
                burnt.setDamageValue(rod.getDamageValue() + 1);
                setStack(i, burnt);
            }
        }
        moveSpentRods();
    }

    /** Spent rods become Depleted Fuel Rods in the waste slots; they wait in place while those are full. */
    private void moveSpentRods() {
        for (int i = 0; i < FUEL_SLOTS; i++) {
            ItemStack rod = getStack(i);
            if (rod.getItem() instanceof FuelRodItem && FuelRodItem.isSpent(rod) && addWaste()) {
                setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private boolean addWaste() {
        for (int i = FUEL_SLOTS; i < FUEL_SLOTS + WASTE_SLOTS; i++) {
            ItemStack waste = getStack(i);
            if (waste.isEmpty()) {
                setStack(i, new ItemStack(ModItems.DEPLETED_FUEL_ROD.get()));
                return true;
            }
            if (waste.is(ModItems.DEPLETED_FUEL_ROD.get()) && waste.getCount() < waste.getMaxStackSize()) {
                setStack(i, waste.copyWithCount(waste.getCount() + 1));
                return true;
            }
        }
        return false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            setPorts(level, List.of());
            setShellFormed(level, state.getValue(MachineBlock.FACING), false);
        }
    }

    // ---------------------------------------------------------------- automation

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return index < FUEL_SLOTS && resource.getItem() instanceof FuelRodItem && !FuelRodItem.isSpent(resource.toStack());
    }

    @Override
    protected int getSlotLimit(int index, ItemResource resource) {
        return index < FUEL_SLOTS ? 1 : Integer.MAX_VALUE;
    }

    /** What ports expose for items: Fuel Rods go in, Depleted Fuel Rods come out. */
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
            return index < FUEL_SLOTS ? items.insert(index, resource, amount, transaction) : 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return index >= FUEL_SLOTS ? items.extract(index, resource, amount, transaction) : 0;
        }
    };

    public ResourceHandler<ItemResource> getAutomationItems() {
        return automationItems;
    }

    /** What ports expose for fluids: water and coolant go in, steam comes out. */
    public ResourceHandler<FluidResource> getAutomationFluids() {
        return automationFluids;
    }

    // ---------------------------------------------------------------- GUI and saving

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> isFormed() ? 1 : 0;
            case 1 -> activeRods;
            case 2 -> water.amount();
            case 3 -> water.fluidCode();
            case 4 -> steam.amount();
            case 5 -> steam.fluidCode();
            case 6 -> steamRate;
            case 7 -> coolant.amount();
            case 8 -> coolant.fluidCode();
            case 9 -> temperaturePermille();
            case 10 -> boost;
            case 11 -> transferCapacity();
            case 12 -> activeRods * HEAT_PER_ROD * boost / 100;
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 13;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        water.serialize(output.child("water"));
        steam.serialize(output.child("steam"));
        coolant.serialize(output.child("coolant"));
        output.putInt("burn_progress", burnProgress);
        output.putInt("heat", heat);
        output.putInt("boost", boost);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        water.deserialize(input.childOrEmpty("water"));
        steam.deserialize(input.childOrEmpty("steam"));
        coolant.deserialize(input.childOrEmpty("coolant"));
        burnProgress = input.getIntOr("burn_progress", 0);
        heat = input.getIntOr("heat", 0);
        boost = input.getIntOr("boost", 0);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ReactorMenu(containerId, inventory, this, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
