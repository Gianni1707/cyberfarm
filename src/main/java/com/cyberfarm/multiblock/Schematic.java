package com.cyberfarm.multiblock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

/**
 * A structure saved with a Structure Block and shipped in a data pack under
 * {@code structure/schematic/}. Its north side (the lowest z) is its front: it goes towards the
 * player, centred on the block aimed at, and the rest stretches away. Air, structure voids and
 * jigsaws are left out; block entity data (chest contents...) is never copied.
 */
public record Schematic(Identifier id, Vec3i size, List<Entry> blocks) implements Structure {
    /** Bigger schematics are skipped: the projection and the network message stay small. */
    public static final int MAX_SIZE = 32;
    private static final String FOLDER = "schematic/";

    /** One block, relative to the structure's lowest corner; sorted bottom layer first. */
    public record Entry(BlockPos pos, BlockState state) {
        static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Entry::pos,
                ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), Entry::state,
                Entry::new);
    }

    public static final StreamCodec<ByteBuf, Schematic> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, Schematic::id,
            BlockPos.STREAM_CODEC.map(pos -> (Vec3i) pos, BlockPos::new), Schematic::size,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), Schematic::blocks,
            Schematic::new);

    public static boolean isSchematic(Identifier id) {
        return id.getPath().startsWith(FOLDER);
    }

    /** Reads the template's blocks through its saved form; null when it is too big or empty. */
    public static @Nullable Schematic of(Identifier id, StructureTemplate template, HolderGetter<Block> blockLookup) {
        Vec3i size = template.getSize();
        if (size.getX() > MAX_SIZE || size.getY() > MAX_SIZE || size.getZ() > MAX_SIZE) {
            return null;
        }
        CompoundTag tag = template.save(new CompoundTag());
        ListTag palette = tag.getListOrEmpty("palette");
        if (palette.isEmpty()) {
            // A template with several palettes (like shipwrecks): the first one
            palette = tag.getListOrEmpty("palettes").getListOrEmpty(0);
        }
        List<BlockState> states = new ArrayList<>(palette.size());
        for (int i = 0; i < palette.size(); i++) {
            states.add(NbtUtils.readBlockState(blockLookup, palette.getCompoundOrEmpty(i)));
        }
        List<Entry> blocks = new ArrayList<>();
        ListTag list = tag.getListOrEmpty("blocks");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag block = list.getCompoundOrEmpty(i);
            int index = block.getIntOr("state", -1);
            if (index < 0 || index >= states.size()) {
                continue;
            }
            BlockState state = states.get(index);
            if (state.isAir() || state.is(Blocks.STRUCTURE_VOID) || state.is(Blocks.JIGSAW)) {
                continue;
            }
            ListTag pos = block.getListOrEmpty("pos");
            blocks.add(new Entry(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)), state));
        }
        blocks.sort(Comparator.comparingInt(entry -> entry.pos().getY()));
        return blocks.isEmpty() ? null : new Schematic(id, size, List.copyOf(blocks));
    }

    private String key() {
        return "schematic." + id.getNamespace() + "." + id.getPath().substring(FOLDER.length()).replace('/', '.');
    }

    /** Its translated name, or the file name made readable ("watch_tower" shows as "Watch Tower"). */
    @Override
    public Component displayName() {
        String file = id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
        StringBuilder name = new StringBuilder();
        for (String word : file.split("_")) {
            if (!word.isEmpty()) {
                name.append(name.isEmpty() ? "" : " ").append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
            }
        }
        return Component.translatableWithFallback(key(), name.toString());
    }

    @Override
    public Component description() {
        return Component.translatableWithFallback(key() + ".desc", "A saved structure, %s x %s x %s blocks.",
                size.getX(), size.getY(), size.getZ());
    }

    /** The turn that brings the front (north side) towards {@code facing}. */
    private static Rotation rotation(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    @Override
    public List<Ghost> ghosts(BlockPos origin, Direction facing) {
        Rotation rotation = rotation(facing);
        // The middle of the front row lands on the origin
        BlockPos pivot = new BlockPos(size.getX() / 2, 0, 0);
        List<Ghost> ghosts = new ArrayList<>(blocks.size());
        for (Entry entry : blocks) {
            BlockState state = entry.state().rotate(rotation);
            Block block = state.getBlock();
            ghosts.add(new Ghost(origin.offset(entry.pos().subtract(pivot).rotate(rotation)), state, here -> here.is(block)));
        }
        return ghosts;
    }

    /** Without shape updates, so door and bed halves, stairs and fences go in exactly as saved. */
    @Override
    public void buildInstantly(ServerLevel level, BlockPos origin, Direction facing) {
        for (Ghost ghost : ghosts(origin, facing)) {
            if (!ghost.ok().test(level.getBlockState(ghost.pos()))) {
                level.setBlock(ghost.pos(), ghost.show(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }
}
