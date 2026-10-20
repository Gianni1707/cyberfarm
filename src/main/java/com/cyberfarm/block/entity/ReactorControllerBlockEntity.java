package com.cyberfarm.block.entity;

import com.cyberfarm.Config;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.ReactorControllerBlock;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.fluid.TankGroup;
import com.cyberfarm.item.FuelRodItem;
import com.cyberfarm.menu.ReactorMenu;
import com.cyberfarm.multiblock.Multiblock;
import com.cyberfarm.multiblock.TowerSize;
import com.cyberfarm.radiation.Radiation;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
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

/**
 * Fission Reactor controller: the brain of a cooling-tower multiblock in three sizes (see
 * {@link TowerSize}). Burning Fuel Rods heat the core; water from the ports absorbs the heat and
 * leaves as Steam, which the ports push out to Steam Turbines. Bigger towers burn more rods, take
 * more power and move more heat. Shell blocks are Reactor Casings or Reactor Ports; the inside must
 * be empty.
 */
public class ReactorControllerBlockEntity extends MultiblockControllerBlockEntity<TowerSize> {
    /** Room for the largest tower's rods; smaller towers only burn the first ones. */
    public static final int FUEL_SLOTS = 8;
    public static final int WASTE_SLOTS = 4;
    public static final int TANK_CAPACITY = 16_000;
    /** Heat of one rod per tick at 100% power; each mB of water absorbs one unit and becomes one mB of steam. */
    public static final int HEAT_PER_ROD = 50;
    public static final int BOOST_STEP = 50;
    public static final double COOLANT_FACTOR = 2.5;
    public static final double CRYO_FACTOR = 6.0;
    /** Heat moved per mB of coolant worn out. */
    public static final int COOLANT_WEAR = 400;
    public static final int MELTDOWN_HEAT = 200_000;
    public static final int ALARM_PERMILLE = 800;
    private static final int PASSIVE_COOLING = 20;
    private static final int PORT_OUTPUT = 2_500;
    private static final int HUM_INTERVAL = 80;

    private final GasTank water = new GasTank(TANK_CAPACITY, resource -> resource.is(Fluids.WATER.getFluidType()), this::setChanged);
    private final GasTank steam = new GasTank(TANK_CAPACITY, resource -> false, this::setChanged);
    /** Primary loop: Coolant or Cryo Coolant; empty means plain water. */
    private final GasTank coolant = new GasTank(TANK_CAPACITY,
            resource -> resource.is(ModFluids.COOLANT_TYPE.get()) || resource.is(ModFluids.CRYO_COOLANT_TYPE.get()), this::setChanged);
    private final TankGroup automationFluids = TankGroup.of(new GasTank[] {water, coolant}, steam);
    /** Burn progress in percent-ticks: a rod loses a second of fuel every 2000, so faster at higher power. */
    private int burnProgress;
    private int activeRods;
    private int steamRate;
    private int heat;
    private int boost;
    private int coolantWear;

    public ReactorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_CONTROLLER.get(), pos, state, FUEL_SLOTS + WASTE_SLOTS, 0, 0, 0, TowerSize.values());
    }

    // ---------------------------------------------------------------- structure

    @Override
    protected boolean isShellBlock(BlockState state) {
        return state.is(ModBlocks.REACTOR_CASING.get()) || state.is(ModBlocks.REACTOR_PORT.get());
    }

    @Override
    protected boolean isPort(BlockState state) {
        return state.is(ModBlocks.REACTOR_PORT.get());
    }

    /**
     * Right-clicking a casing or port of a formed tower opens its controller. The controller sits on
     * the second layer, at most a large tower's width away sideways.
     */
    public static InteractionResult openFromShell(Level level, BlockPos clicked, Player player) {
        int reach = TowerSize.LARGE.base() * 2;
        int below = TowerSize.LARGE.height() - 2;
        return Multiblock.openController(level, clicked, player, clicked.offset(-reach, -below, -reach), clicked.offset(reach, 1, reach),
                state -> state.getBlock() instanceof ReactorControllerBlock ? sizeOf(state, TowerSize.values()) : null);
    }

    // ---------------------------------------------------------------- tick

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        state = checkStructure(level, pos, state);
        if (state == null) {
            return;
        }
        Direction facing = state.getValue(MachineBlock.FACING);
        heat = Math.max(0, heat - PASSIVE_COOLING);
        TowerSize size = size();
        if (size == null) {
            steamRate = 0;
            activeRods = 0;
            return;
        }
        boost = Math.min(boost, size.maxBoost());

        activeRods = 0;
        for (int i = 0; i < size.rods(); i++) {
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
                burnRods(size.rods());
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
            meltdown(level, pos, facing, size);
            return;
        }
        if (temperaturePermille() >= ALARM_PERMILLE && level.getGameTime() % 20 == 0) {
            level.playSound(null, pos, ModSounds.REACTOR_ALARM.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        }
        boolean running = produced > 0;
        if (running && level.getGameTime() % HUM_INTERVAL == 0) {
            level.playSound(null, size.floorCentre(pos, facing).above(size.height() / 2), ModSounds.REACTOR_HUM.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (running != state.getValue(ReactorControllerBlock.LIT)) {
            level.setBlock(pos, state.setValue(ReactorControllerBlock.LIT, running), Block.UPDATE_CLIENTS);
        }
        for (BlockPos port : ports) {
            steam.pushToNeighbors(level, port, PORT_OUTPUT);
        }
    }

    /** Breaking a hot tower does not save it: the core melts down anyway. */
    @Override
    protected boolean onStructureBroken(ServerLevel level, BlockPos pos, Direction facing, TowerSize previous) {
        if (temperaturePermille() < ALARM_PERMILLE) {
            return false;
        }
        meltdown(level, pos, facing, previous);
        return true;
    }

    @Override
    protected void onStructureChecked(ServerLevel level) {
        moveSpentRods();
    }

    /** Heat the primary loop can move per tick: more in bigger towers, and with Coolant or Cryo Coolant. */
    public int transferCapacity() {
        TowerSize size = size();
        if (size == null) {
            return 0;
        }
        FluidResource loop = coolant.getResource(0);
        double factor = coolant.amount() == 0 ? 1.0
                : loop.is(ModFluids.CRYO_COOLANT_TYPE.get()) ? CRYO_FACTOR : COOLANT_FACTOR;
        return (int) (size.baseTransfer() * factor);
    }

    public int temperaturePermille() {
        return (int) Math.min(1000L, (long) heat * 1000 / MELTDOWN_HEAT);
    }

    /** Power from the GUI, in steps of {@link #BOOST_STEP}%, from 0 (control rods in) to the tower's maximum. */
    public void changeBoost(int steps) {
        TowerSize size = size();
        int max = size != null ? size.maxBoost() : TowerSize.SMALL.maxBoost();
        boost = Math.clamp(boost + steps * BOOST_STEP, 0, max);
        setChanged();
    }

    /** The core overheated: the rods melt and the core blows up (power and block damage from the common config). */
    private void meltdown(ServerLevel level, BlockPos pos, Direction facing, TowerSize size) {
        meltRods();
        explode(level, size.floorCentre(pos, facing).above(), size);
    }

    private void meltRods() {
        heat = 0;
        boost = 0;
        for (int i = 0; i < FUEL_SLOTS; i++) {
            setStack(i, ItemStack.EMPTY);
        }
    }

    private static void explode(ServerLevel level, BlockPos core, TowerSize size) {
        Radiation.meltdown(level, core);
        level.explode(null, core.getX() + 0.5, core.getY() + 0.5, core.getZ() + 0.5,
                Config.MELTDOWN_EXPLOSION_POWER.get().floatValue() * size.explosionScale(),
                Config.MELTDOWN_DESTROYS_BLOCKS.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    /** One second of burning: every active rod loses a point of fuel. */
    private void burnRods(int usable) {
        for (int i = 0; i < usable; i++) {
            ItemStack rod = getStack(i);
            if (rod.getItem() instanceof FuelRodItem && !FuelRodItem.isSpent(rod)) {
                ItemStack burnt = rod.copy();
                burnt.setDamageValue(rod.getDamageValue() + 1);
                setStack(i, burnt);
            }
        }
        moveSpentRods();
    }

    /**
     * Spent rods become Depleted Fuel Rods in the waste slots. While those are full the spent rods
     * wait in place and the reactor leaks radiation.
     */
    private void moveSpentRods() {
        for (int i = 0; i < FUEL_SLOTS; i++) {
            ItemStack rod = getStack(i);
            if (rod.getItem() instanceof FuelRodItem && FuelRodItem.isSpent(rod)) {
                if (addWaste()) {
                    setStack(i, ItemStack.EMPTY);
                } else if (level instanceof ServerLevel serverLevel) {
                    Radiation.add(serverLevel, worldPosition, Radiation.REACTOR_LEAK / 5);
                }
            }
        }
    }

    private boolean addWaste() {
        for (int i = FUEL_SLOTS; i < FUEL_SLOTS + WASTE_SLOTS; i++) {
            if (hasRoomFor(i, ModItems.DEPLETED_FUEL_ROD.get())) {
                addOne(i, ModItems.DEPLETED_FUEL_ROD.get());
                return true;
            }
        }
        return false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        TowerSize size = sizeOf(state, TowerSize.values());
        if (level instanceof ServerLevel serverLevel && size != null && temperaturePermille() >= ALARM_PERMILLE) {
            // Breaking a hot controller melts the core down too. The rods melt instead of dropping; the
            // blast waits for the next tick, so it does not hit the controller while it is being removed.
            meltRods();
            BlockPos core = size.floorCentre(pos, state.getValue(MachineBlock.FACING)).above();
            MinecraftServer server = serverLevel.getServer();
            server.schedule(new TickTask(server.getTickCount(), () -> explode(serverLevel, core, size)));
        }
        super.preRemoveSideEffects(pos, state);
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

    @Override
    public ResourceHandler<ItemResource> portItems() {
        return automationItems;
    }

    /** What ports expose for fluids: water and coolant go in, steam comes out. */
    @Override
    public ResourceHandler<FluidResource> portFluids() {
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
            case 13 -> size() != null ? size().ordinal() + 1 : 0;
            case 14 -> size() != null ? size().rods() : 0;
            case 15 -> size() != null ? size().maxBoost() : 0;
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 16;
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
