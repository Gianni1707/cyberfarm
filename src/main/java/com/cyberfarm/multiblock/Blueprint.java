package com.cyberfarm.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.entity.ResonanceCoreBlockEntity;
import com.cyberfarm.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * The multiblocks the Multiblock Projector can show, each laid out from its controller's position
 * and facing, with the same rules the controllers check.
 */
public enum Blueprint implements Structure {
    REACTOR_SMALL("reactor", TowerSize.SMALL, null),
    REACTOR_MEDIUM("reactor", TowerSize.MEDIUM, null),
    REACTOR_LARGE("reactor", TowerSize.LARGE, null),
    TURBINE_SMALL("turbine", null, TurbineSize.SMALL),
    TURBINE_MEDIUM("turbine", null, TurbineSize.MEDIUM),
    TURBINE_LARGE("turbine", null, TurbineSize.LARGE),
    METHANE_CHAMBER("methane_chamber", null, null),
    RESONANCE_CORE_TIER_2("resonance_core", 1),
    RESONANCE_CORE_TIER_3("resonance_core", 2),
    RESONANCE_CORE_TIER_4("resonance_core", 3),
    MECH_HANGAR("mech_hangar", true),
    CRYSTALLIZER("crystallizer", false);

    /** One block of the layout as the helpers below draw it up; {@link #ghosts} turns it into placed states. */
    private record Part(BlockPos pos, @Nullable Block show, Predicate<BlockState> ok) {}

    private final String name;
    private final @Nullable TowerSize tower;
    private final @Nullable TurbineSize turbine;
    /** Shell half-size of a Resonance Core tier, 0 for the other structures. */
    private final int coreRadius;
    private final boolean hangar;

    Blueprint(String name, @Nullable TowerSize tower, @Nullable TurbineSize turbine) {
        this.name = name;
        this.tower = tower;
        this.turbine = turbine;
        this.coreRadius = 0;
        this.hangar = false;
    }

    Blueprint(String name, int coreRadius) {
        this.name = name;
        this.tower = null;
        this.turbine = null;
        this.coreRadius = coreRadius;
        this.hangar = false;
    }

    Blueprint(String name, boolean hangar) {
        this.name = name;
        this.tower = null;
        this.turbine = null;
        this.coreRadius = 0;
        this.hangar = hangar;
    }

    /** Translation key of the name shown to the player, with the size when there is one. */
    public String key() {
        return "projector.cyberfarm." + name;
    }

    @Override
    public Identifier id() {
        return CyberFarm.id(name().toLowerCase(Locale.ROOT));
    }

    @Override
    public Component displayName() {
        Component name = Component.translatable(key());
        return sizeKey() == null ? name
                : Component.translatable("message.cyberfarm.projector.sized", name, Component.translatable(sizeKey()));
    }

    @Override
    public Component description() {
        return Component.translatable(key() + ".desc");
    }

    public @Nullable String sizeKey() {
        if (coreRadius > 0) {
            return "projector.cyberfarm.size.tier_" + (coreRadius + 1);
        }
        String size = tower != null ? tower.name() : turbine != null ? turbine.name() : null;
        return size != null ? "gui.cyberfarm.reactor.size." + size.toLowerCase() : null;
    }

    public static Blueprint byOrdinal(int ordinal) {
        return values()[Math.floorMod(ordinal, values().length)];
    }

    @Override
    public List<Ghost> ghosts(BlockPos controller, Direction facing) {
        return parts(controller, facing).stream()
                .map(part -> new Ghost(part.pos(), part.show() != null ? placedState(part.show(), facing) : null, part.ok()))
                .toList();
    }

    private List<Part> parts(BlockPos controller, Direction facing) {
        if (tower != null) {
            return fromCells(tower.cells(controller, facing), controller, ModBlocks.REACTOR_CONTROLLER.get(),
                    ModBlocks.REACTOR_CASING.get(), ModBlocks.REACTOR_PORT.get());
        }
        if (turbine != null) {
            return fromCells(turbine.cells(controller, facing), controller, ModBlocks.TURBINE_CONTROLLER.get(),
                    ModBlocks.TURBINE_CASING.get(), ModBlocks.TURBINE_PORT.get());
        }
        if (hangar) {
            return mechHangar(controller, facing);
        }
        if (this == CRYSTALLIZER) {
            return crystallizer(controller, facing);
        }
        return coreRadius > 0 ? resonanceCore(controller, coreRadius) : methaneChamber(controller);
    }

    /** The hangar's size: a 5x5 footprint, pillars up to the frame on top at this height. */
    public static final int HANGAR_SIZE = 5;
    public static final int HANGAR_TOP = 5;

    /**
     * A Mech Hangar, a rack the mech stands in: the console at the front of the left floor rail,
     * facing out; two floor rails of Hangar Frame along the sides, two posts at their back ends up
     * to a beam across the top, and two arms reaching forward from the beam over the mech's
     * shoulders; a 3x3 charging pad in the middle with the space over it left free. Open in front
     * and on the sides, so the mech walks in and out.
     */
    private static List<Part> mechHangar(BlockPos console, Direction facing) {
        Block frame = ModBlocks.HANGAR_FRAME.get();
        Block pad = ModBlocks.HANGAR_PAD.get();
        Direction across = facing.getCounterClockWise();
        Direction in = facing.getOpposite();
        int last = HANGAR_SIZE - 1;
        List<Part> ghosts = new ArrayList<>();
        for (int w = 0; w < HANGAR_SIZE; w++) {
            for (int d = 0; d < HANGAR_SIZE; d++) {
                BlockPos base = console.relative(across, w).relative(in, d);
                boolean rail = w == 0 || w == last;
                if (w == 0 && d == 0) {
                    ghosts.add(new Part(base, ModBlocks.MECH_HANGAR.get(), state -> state.is(ModBlocks.MECH_HANGAR.get())));
                } else if (rail) {
                    ghosts.add(frame(base, frame));
                } else if (d >= 1 && d <= 3) {
                    ghosts.add(frame(base, pad));
                    for (int y = 1; y < HANGAR_TOP; y++) {
                        ghosts.add(new Part(base.above(y), null, BlockState::isAir));
                    }
                }
                // Posts at the back of the rails, the beam across their tops, the arms forward over the pad
                if (rail && d == last) {
                    for (int y = 1; y <= HANGAR_TOP; y++) {
                        ghosts.add(frame(base.above(y), frame));
                    }
                } else if (!rail && d == last) {
                    ghosts.add(frame(base.above(HANGAR_TOP), frame));
                } else if ((w == 1 || w == last - 1) && (d == 2 || d == 3)) {
                    ghosts.add(frame(base.above(HANGAR_TOP), frame));
                }
            }
        }
        return ghosts;
    }

    /** Middle of the hangar's pad, for a console at {@code console} facing {@code facing}. */
    public static BlockPos hangarCentre(BlockPos console, Direction facing) {
        return console.relative(facing.getCounterClockWise(), 2).relative(facing.getOpposite(), 2);
    }

    /**
     * A Resonance Core tier: the shell cube over the pedestal, empty inside, and the pylon columns
     * it needs at their closest, east and west first. The core checks itself against the same rules.
     */
    private static List<Part> resonanceCore(BlockPos core, int radius) {
        Block cell = ModBlocks.RESONANCE_CELL.get();
        Block pylon = ModBlocks.STABILIZER_PYLON.get();
        List<Part> ghosts = new ArrayList<>();
        ghosts.add(new Part(core, ModBlocks.RESONANCE_CORE.get(), state -> state.is(ModBlocks.RESONANCE_CORE.get())));
        BlockPos centre = core.above(1 + radius);
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            boolean surface = Math.max(Math.abs(pos.getX() - centre.getX()), Math.max(Math.abs(pos.getY() - centre.getY()),
                    Math.abs(pos.getZ() - centre.getZ()))) == radius;
            ghosts.add(surface ? frame(pos.immutable(), cell) : new Part(pos.immutable(), null, BlockState::isAir));
        }
        int tier = radius + 1;
        Direction[] sides = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
        for (int i = 0; i < ResonanceCoreBlockEntity.requiredPylons(tier); i++) {
            BlockPos ground = core.relative(sides[i], ResonanceCoreBlockEntity.minDistance(tier));
            for (int y = 0; y < ResonanceCoreBlockEntity.requiredHeight(tier); y++) {
                ghosts.add(frame(ground.above(y), pylon));
            }
        }
        return ghosts;
    }

    /** Middle of a Crystallizer's chamber, on the ground, for a controller at {@code controller} facing {@code facing}. */
    public static BlockPos crystallizerCentre(BlockPos controller, Direction facing) {
        return controller.relative(facing.getOpposite());
    }

    /**
     * The 3x3x4 chamber behind a Crystallizer, which sits in the middle of its base's front, facing
     * out: a floor and a roof of Cyber Frame, frames up the corners, Containment Glass walls and the
     * two blocks in the middle left empty for the crystal. Any frame may be a Crystallizer Port
     * instead: the projector suggests one at the back of the floor.
     */
    private static List<Part> crystallizer(BlockPos controller, Direction facing) {
        Block frame = ModBlocks.CYBER_FRAME.get();
        Block glass = ModBlocks.CONTAINMENT_GLASS.get();
        Block crystallizer = ModBlocks.CRYSTALLIZER.get();
        Block port = ModBlocks.CRYSTALLIZER_PORT.get();
        Predicate<BlockState> shell = state -> state.is(frame) || state.is(port);
        BlockPos centre = crystallizerCentre(controller, facing);
        BlockPos suggested = centre.relative(facing.getOpposite());
        List<Part> ghosts = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean middle = dx == 0 && dz == 0;
                boolean corner = dx != 0 && dz != 0;
                BlockPos base = centre.offset(dx, 0, dz);
                ghosts.add(base.equals(controller) ? new Part(base, crystallizer, state -> state.is(crystallizer))
                        : new Part(base, base.equals(suggested) ? port : frame, shell));
                for (int dy = 1; dy <= 2; dy++) {
                    BlockPos pos = base.above(dy);
                    ghosts.add(middle ? new Part(pos, null, BlockState::isAir) : corner ? new Part(pos, frame, shell) : frame(pos, glass));
                }
                ghosts.add(new Part(base.above(3), frame, shell));
            }
        }
        return ghosts;
    }

    private static List<Part> fromCells(List<Multiblock.Cell> cells, BlockPos controller, Block controllerBlock, Block casing, Block port) {
        List<Part> ghosts = new ArrayList<>(cells.size());
        for (Multiblock.Cell cell : cells) {
            if (cell.pos().equals(controller)) {
                ghosts.add(new Part(cell.pos(), controllerBlock, state -> state.is(controllerBlock)));
            } else if (cell.part() == Multiblock.Part.SHELL) {
                ghosts.add(new Part(cell.pos(), casing, state -> state.is(casing) || state.is(port)));
            } else if (cell.part() == Multiblock.Part.EMPTY) {
                ghosts.add(new Part(cell.pos(), null, BlockState::isAir));
            }
        }
        return ghosts;
    }

    /**
     * The 3x3x4 chamber under a Methane Extractor; the extractor checks itself against it too. Any
     * Cyber Frame may be an Acid Inlet or a Methane Outlet instead, any glass of the lower wall an
     * Intake Gate: the projector suggests one of each.
     */
    private static List<Part> methaneChamber(BlockPos top) {
        Block frame = ModBlocks.CYBER_FRAME.get();
        Block glass = ModBlocks.CONTAINMENT_GLASS.get();
        Block extractor = ModBlocks.METHANE_EXTRACTOR.get();
        Predicate<BlockState> shell = state -> state.is(frame) || state.is(ModBlocks.ACID_INLET.get()) || state.is(ModBlocks.METHANE_OUTLET.get());
        Predicate<BlockState> lowerWall = state -> state.is(glass) || state.is(ModBlocks.INTAKE_GATE.get());
        List<Part> ghosts = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean centre = dx == 0 && dz == 0;
                boolean corner = dx != 0 && dz != 0;
                Block roof = dx == -1 && dz == 0 ? ModBlocks.ACID_INLET.get() : dx == 1 && dz == 0 ? ModBlocks.METHANE_OUTLET.get() : frame;
                ghosts.add(centre ? new Part(top, extractor, state -> state.is(extractor)) : new Part(top.offset(dx, 0, dz), roof, shell));
                for (int dy = 1; dy <= 2; dy++) {
                    BlockPos pos = top.offset(dx, -dy, dz);
                    Block gate = dy == 2 && dx == 0 && dz == -1 ? ModBlocks.INTAKE_GATE.get() : glass;
                    ghosts.add(centre ? new Part(pos, null, BlockState::isAir)
                            : corner ? new Part(pos, frame, shell)
                            : dy == 2 ? new Part(pos, gate, lowerWall)
                            : frame(pos, glass));
                }
                ghosts.add(new Part(top.offset(dx, -3, dz), frame, shell));
            }
        }
        return ghosts;
    }

    /** The state a ghost's block is placed with: controllers and consoles face {@code facing}, like the structure. */
    public static BlockState placedState(Block show, Direction facing) {
        BlockState state = show.defaultBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing) : state;
    }

    private static Part frame(BlockPos pos, Block frame) {
        return new Part(pos, frame, state -> state.is(frame));
    }
}
