package com.cyberfarm.block;


import com.cyberfarm.block.entity.FluidTankBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Liquid Tank or Gas Tank: stores one fluid, liquids or gases like the matching pipe. Passive: it takes what pipes and buckets put in,
 * and gives it out to pipes set to pull from it. Blocks of a kind that fill a box are one tank (see {@link FluidTankBlockEntity}); each
 * side shows what it is: a plain face with the sight bar, a socket where a pipe plugs in, or open into the next block of the tank.
 */
public class FluidTankBlock extends MachineBlock {
    public enum Side implements StringRepresentable {
        FACE, SOCKET, JOINED;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final Map<Direction, EnumProperty<Side>> SIDES = sides();
    private final boolean gas;

    public FluidTankBlock(Properties properties, boolean gas) {
        super(properties);
        this.gas = gas;
        BlockState state = defaultBlockState();
        for (EnumProperty<Side> side : SIDES.values()) {
            state = state.setValue(side, Side.FACE);
        }
        registerDefaultState(state);
    }

    private static Map<Direction, EnumProperty<Side>> sides() {
        Map<Direction, EnumProperty<Side>> sides = new EnumMap<>(Direction.class);
        for (Direction side : Direction.values()) {
            sides.put(side, EnumProperty.create(side.getSerializedName(), Side.class));
        }
        return sides;
    }

    public boolean gas() {
        return gas;
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        SIDES.values().forEach(builder::add);
    }

    private static boolean plugged(BlockState neighbor, Direction side) {
        return neighbor.getBlock() instanceof NetworkPipeBlock && neighbor.getValue(NetworkPipeBlock.CONNECTIONS.get(side.getOpposite()));
    }

    /** All its sides as they should be now: its tank decides which are open, the pipes next door the rest. */
    public BlockState withSides(BlockState state, BlockGetter level, BlockPos pos, FluidTankBlockEntity tank) {
        for (Direction side : Direction.values()) {
            state = state.setValue(SIDES.get(side), tank.joined(side) ? Side.JOINED : plugged(level.getBlockState(pos.relative(side)), side) ? Side.SOCKET : Side.FACE);
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(SIDES.get(direction)) == Side.JOINED) {
            return state;
        }
        return state.setValue(SIDES.get(direction), plugged(neighborState, direction) ? Side.SOCKET : Side.FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        for (Direction side : Direction.values()) {
            state = state.setValue(SIDES.get(side), plugged(context.getLevel().getBlockState(context.getClickedPos().relative(side)), side) ? Side.SOCKET : Side.FACE);
        }
        return state;
    }

    /** Buckets of any mod fill or empty the tank, like a cauldron. One it can't take says why, and never pours out beside the tank. Other items are used as usual. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) || !FluidUtil.getFluidHandler(stack).isPresent()) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (!FluidUtil.interactWithFluidHandler(player, hand, tank.handler()) && !level.isClientSide()) {
            player.displayClientMessage(tank.refusal(), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** No GUI, and nothing on a plain click, so tanks can be built up against each other. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    /** Shift + right click, with anything in hand, tells how full it is: as an event, since sneaking with an item never reaches the block. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isSecondaryUseActive() && event.getLevel().getBlockEntity(event.getPos()) instanceof FluidTankBlockEntity tank) {
            if (!event.getLevel().isClientSide()) {
                event.getEntity().displayClientMessage(tank.status(), true);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.FLUID_TANK.get());
    }
}
