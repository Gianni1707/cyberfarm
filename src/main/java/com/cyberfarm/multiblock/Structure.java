package com.cyberfarm.multiblock;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Anything the Multiblock Projector shows and a Builder drone builds: the mod's multiblocks
 * ({@link Blueprint}) and the schematics from data packs ({@link Schematic}).
 */
public interface Structure {
    /**
     * One block of the layout: {@code show} is what to place there (null: leave it empty) and
     * {@code ok} whether what is there now is right.
     */
    record Ghost(BlockPos pos, @Nullable BlockState show, Predicate<BlockState> ok) {}

    /** The multiblocks first, then the schematics. */
    static List<Structure> all() {
        return Stream.concat(Arrays.stream(Blueprint.values()), Schematics.all().stream()).toList();
    }

    static Optional<Structure> byId(Identifier id) {
        return all().stream().filter(structure -> structure.id().equals(id)).findFirst();
    }

    Identifier id();

    Component displayName();

    /** One or two lines on what it is, for the projector's list. */
    Component description();

    /** The layout placed at {@code origin}, with its front towards {@code facing}. */
    List<Ghost> ghosts(BlockPos origin, Direction facing);

    /** Creative only: places every block of the structure at once and clears the spaces that must be empty. */
    default void buildInstantly(ServerLevel level, BlockPos origin, Direction facing) {
        for (Ghost ghost : ghosts(origin, facing)) {
            if (!ghost.ok().test(level.getBlockState(ghost.pos()))) {
                level.setBlock(ghost.pos(), ghost.show() != null ? ghost.show() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
