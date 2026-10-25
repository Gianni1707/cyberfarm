package com.cyberfarm.item;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.multiblock.Blueprint;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A structure picked in the Multiblock Projector's list, for the projector held in {@code hand}. */
public record ProjectorSelectPayload(InteractionHand hand, int blueprint) implements CustomPacketPayload {
    public static final Type<ProjectorSelectPayload> TYPE = new Type<>(CyberFarm.id("projector_select"));
    public static final StreamCodec<ByteBuf, ProjectorSelectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL.map(main -> main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, hand -> hand == InteractionHand.MAIN_HAND),
            ProjectorSelectPayload::hand,
            ByteBufCodecs.VAR_INT, ProjectorSelectPayload::blueprint,
            ProjectorSelectPayload::new);

    @Override
    public Type<ProjectorSelectPayload> type() {
        return TYPE;
    }

    public static void handle(ProjectorSelectPayload payload, IPayloadContext context) {
        ItemStack stack = context.player().getItemInHand(payload.hand());
        if (stack.getItem() instanceof ProjectorItem) {
            ProjectorItem.select(stack, Blueprint.byOrdinal(payload.blueprint()));
        }
    }
}
