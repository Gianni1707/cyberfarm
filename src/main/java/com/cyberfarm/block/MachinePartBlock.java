package com.cyberfarm.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Never placed: its states name the models of the parts that move while a small multiblock opens, which the machines'
 * renderers draw as a piston draws what it pushes. So they look exactly like the static model they become.
 */
public class MachinePartBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 42);
    public static final int TURBINE_DUCT = 0, TURBINE_NOZZLE = 1, TURBINE_DOOR_LEFT = 2, TURBINE_DOOR_RIGHT = 3;
    public static final int MIXER_WATER_TANK = 4, MIXER_COOLANT_TANK = 5, MIXER_WATER_SADDLE = 6, MIXER_COOLANT_SADDLE = 7, MIXER_RAIL = 8;
    /** The Coolant Mixer's hood: front, back and end panels, each a right and a left. */
    public static final int MIXER_HOOD = 9;
    public static final int STEAM_PISTON = 15, STEAM_WHEEL = 16, STEAM_RAIL = 17, STEAM_BASE = 18, STEAM_POSTS = 19, STEAM_LID = 20, STEAM_LINE = 21;
    public static final int STEAM_DYNAMO_RAIL = 22, STEAM_DRUM = 23, STEAM_RIM = 24, STEAM_SHAFT = 25;
    public static final int ACID_WATER = 26, ACID_ACID = 27, ACID_TOWER = 28, ACID_TOP = 29, ACID_CHIMNEY = 30;
    public static final int BIO_TOP = 31, BIO_RING_LOW = 32, BIO_RING_HIGH = 33, BIO_TANK = 34;
    public static final int FAB_BASE = 35, FAB_BED = 36, FAB_IDLER_NEAR = 37, FAB_IDLER_FAR = 38, FAB_MAGAZINE = 39;
    public static final int OVEN_BASE = 40, OVEN_TUBE = 41, OVEN_TANK = 42;

    public MachinePartBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }
}
