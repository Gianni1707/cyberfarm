package com.cyberfarm.block;

import java.util.Map;

import com.cyberfarm.block.entity.ItemPipeBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jspecify.annotations.Nullable;

/**
 * Item Pipe: carries items between inventories of any mod. Inventories can push into it (hoppers,
 * machines); right-click the arm towards an inventory to make the pipe pull from it, right-click it
 * again to open the filter, sneak + right-click to stop pulling. Items go to the nearest inventory
 * on the network that takes them.
 */
public class ItemPipeBlock extends NetworkPipeBlock {
    /** Per side: the pipe pulls items out of the inventory there. */
    public static final Map<Direction, BooleanProperty> EXTRACT = Maps.newEnumMap(Map.of(
            Direction.NORTH, BooleanProperty.create("extract_north"),
            Direction.EAST, BooleanProperty.create("extract_east"),
            Direction.SOUTH, BooleanProperty.create("extract_south"),
            Direction.WEST, BooleanProperty.create("extract_west"),
            Direction.UP, BooleanProperty.create("extract_up"),
            Direction.DOWN, BooleanProperty.create("extract_down")));

    private final MapCodec<ItemPipeBlock> codec = simpleCodec(properties -> new ItemPipeBlock(properties, rate()));

    public ItemPipeBlock(Properties properties, int rate) {
        super(properties, rate);
        BlockState state = defaultBlockState();
        for (BooleanProperty property : EXTRACT.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<ItemPipeBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        EXTRACT.values().forEach(builder::add);
    }

    @Override
    protected boolean canConnectToMachine(Level level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.Item.BLOCK, pos, side) != null;
    }

    /** Whether {@code side} leads to an inventory (not another pipe), so it can be set to pull. */
    public boolean isInventorySide(BlockState state, LevelReader level, BlockPos pos, Direction side) {
        return state.getValue(CONNECTIONS.get(side)) && !sameKind(level.getBlockState(pos.relative(side)));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
        // Pulling stops when the inventory is gone
        return isInventorySide(updated, level, pos, direction) ? updated : updated.setValue(EXTRACT.get(direction), false);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Vec3 offset = hit.getLocation().subtract(Vec3.atCenterOf(pos));
        Direction side = Direction.getApproximateNearest(offset.x, offset.y, offset.z);
        boolean pulling = EXTRACT.values().stream().anyMatch(state::getValue);
        boolean toggle = isInventorySide(state, level, pos, side) && (player.isShiftKeyDown() || !state.getValue(EXTRACT.get(side)));
        if (!toggle) {
            // The filter belongs to the pulling pipe, the first of the line
            if (!pulling) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
                player.openMenu(pipe);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            boolean extract = !state.getValue(EXTRACT.get(side));
            level.setBlock(pos, state.setValue(EXTRACT.get(side), extract), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.6F, extract ? 1.4F : 0.8F);
            player.sendOverlayMessage(Component.translatable(extract ? "message.cyberfarm.item_pipe.extract" : "message.cyberfarm.item_pipe.normal"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ITEM_PIPE.get(), (lvl, pos, st, pipe) -> pipe.serverTick(lvl, pos, st));
    }
}
