package com.cyberfarm.trinket;

import java.util.Comparator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The Pocket Drone's errand: out to the nearest drop, then back to the player with it, into the inventory. The server
 * runs it and keeps it on the player (synced); the client draws the drone along the same path from the same numbers
 * (PocketDroneView), so both agree on where it is without sending its position.
 *
 * @param phase    {@link #IDLE}, {@link #OUT} (flying to {@code target}) or {@link #BACK} (flying home with {@code carried})
 * @param target   where the drop lay when the drone left (out) or where it took it (back)
 * @param item     the drop's entity id while going out
 * @param since    game time the phase began
 * @param duration ticks the phase lasts
 */
public record DroneFetch(int phase, Vec3 target, int item, long since, int duration, ItemStack carried) {
    public static final int IDLE = 0, OUT = 1, BACK = 2;
    public static final DroneFetch NONE = new DroneFetch(IDLE, Vec3.ZERO, -1, 0, 0, ItemStack.EMPTY);
    /** Blocks a tick the drone flies, and how long a leg may take at most and at least. */
    static final double SPEED = 0.45;
    static final int MIN_TICKS = 5, MAX_TICKS = 40;
    /** How often an idle drone looks round, and how far it reaches (the drops a step away the player picks up anyway). */
    private static final int LOOK_EVERY = 4;
    private static final double REACH = TrinketTicker.POCKET_DRONE_RANGE, NEAR = 1.2;
    /** Ticks a drop the player threw themself lies there before the drone fetches it too. */
    static final int OWN_DROP_WAIT = 100;

    static final MapCodec<DroneFetch> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            com.mojang.serialization.Codec.INT.fieldOf("phase").forGetter(DroneFetch::phase),
            Vec3.CODEC.fieldOf("target").forGetter(DroneFetch::target),
            com.mojang.serialization.Codec.INT.fieldOf("item").forGetter(DroneFetch::item),
            com.mojang.serialization.Codec.LONG.fieldOf("since").forGetter(DroneFetch::since),
            com.mojang.serialization.Codec.INT.fieldOf("duration").forGetter(DroneFetch::duration),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("carried", ItemStack.EMPTY).forGetter(DroneFetch::carried)).apply(i, DroneFetch::new));
    static final StreamCodec<RegistryFriendlyByteBuf, DroneFetch> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DroneFetch::phase,
            Vec3.STREAM_CODEC, DroneFetch::target,
            ByteBufCodecs.VAR_INT, DroneFetch::item,
            ByteBufCodecs.VAR_LONG, DroneFetch::since,
            ByteBufCodecs.VAR_INT, DroneFetch::duration,
            ItemStack.OPTIONAL_STREAM_CODEC, DroneFetch::carried,
            DroneFetch::new);

    /**
     * Where the drone waits: above and behind the player's right shoulder, out of the way of a first-person view,
     * bobbing a little. {@code partial} 1 on the server.
     */
    public static Vec3 anchor(Player player, float partial, float time) {
        float yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 at = player.getPosition(partial);
        double rightX = -Mth.cos(yaw), rightZ = -Mth.sin(yaw), backX = Mth.sin(yaw), backZ = -Mth.cos(yaw);
        return new Vec3(at.x + rightX * 0.6 + backX * 0.35, at.y + player.getBbHeight() * 1.12 + 0.06 * Mth.sin(time * 0.15F),
                at.z + rightZ * 0.6 + backZ * 0.35);
    }

    /** Ticks a leg of {@code length} blocks takes. */
    static int ticksFor(double length) {
        return Mth.clamp(Mth.ceil(length / SPEED), MIN_TICKS, MAX_TICKS);
    }

    /** How far through the leg, 0 to 1, {@code elapsed} ticks after {@link #since}. */
    public float progress(float elapsed) {
        return duration <= 0 ? 1.0F : Mth.clamp(elapsed / duration, 0.0F, 1.0F);
    }

    /** One server tick of the errand; {@code flying} whether the drone is out and charged. */
    static void tick(ServerPlayer player, ServerLevel level, boolean flying) {
        DroneFetch fetch = player.getData(Trinkets.FETCH);
        long now = level.getGameTime();
        if (!flying) {
            if (fetch.phase != IDLE) {
                // Called back mid-errand: whatever it holds goes straight to the player
                deliver(player, level, fetch.carried);
                player.setData(Trinkets.FETCH, NONE);
            }
            return;
        }
        Vec3 home = anchor(player, 1.0F, now);
        switch (fetch.phase) {
            case IDLE -> {
                if (player.tickCount % LOOK_EVERY == 0) {
                    ItemEntity drop = nearestDrop(player, level);
                    if (drop != null) {
                        player.setData(Trinkets.FETCH, new DroneFetch(OUT, drop.position(), drop.getId(), now,
                                ticksFor(home.distanceTo(drop.position())), ItemStack.EMPTY));
                    }
                }
            }
            case OUT -> {
                if (now - fetch.since < fetch.duration) {
                    return;
                }
                ItemStack taken = ItemStack.EMPTY;
                Vec3 from = fetch.target;
                if (level.getEntity(fetch.item) instanceof ItemEntity drop && drop.isAlive() && drop.position().distanceToSqr(fetch.target) < 4.0) {
                    taken = drop.getItem().copy();
                    from = drop.position();
                    drop.discard();
                    level.playSound(null, from.x, from.y, from.z, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.15F, 1.6F);
                }
                // Home with it, or empty-handed if someone else got there first
                player.setData(Trinkets.FETCH, new DroneFetch(BACK, from, -1, now, ticksFor(home.distanceTo(from)), taken));
            }
            case BACK -> {
                if (now - fetch.since >= fetch.duration) {
                    deliver(player, level, fetch.carried);
                    player.setData(Trinkets.FETCH, NONE);
                }
            }
            default -> player.setData(Trinkets.FETCH, NONE);
        }
    }

    /** The closest drop in reach the player could take and has room for, not one at their feet; one they threw only after a wait. */
    private static ItemEntity nearestDrop(ServerPlayer player, ServerLevel level) {
        return level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(REACH), drop -> drop.isAlive()
                        && !drop.hasPickUpDelay() && (drop.getOwner() != player || drop.tickCount >= OWN_DROP_WAIT) && drop.distanceTo(player) > NEAR && fits(player, drop.getItem()))
                .stream().min(Comparator.comparingDouble(drop -> drop.distanceToSqr(player))).orElse(null);
    }

    private static boolean fits(Player player, ItemStack stack) {
        return player.getInventory().getFreeSlot() >= 0 || player.getInventory().getSlotWithRemainingSpace(stack) >= 0;
    }

    /** Into the inventory; what does not fit is dropped at the player's feet as theirs, so the drone leaves it be. */
    private static void deliver(ServerPlayer player, ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack left = stack.copy();
        player.getInventory().add(left);
        if (!left.isEmpty()) {
            player.drop(left, false);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 1.4F + 2.0F);
    }
}
