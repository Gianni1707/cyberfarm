package com.cyberfarm.item;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.InteractionResultHolder;

import java.util.List;

import java.util.function.Consumer;

import com.cyberfarm.registry.ModDataComponents;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModVillagers;
import com.cyberfarm.village.Blueprints;
import com.cyberfarm.village.VillagePlan;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Blueprint Disc: the plan of one tech village building at one tier, from the Drone Mechanist.
 * Right-click it to learn it: the disc is used up and the building shows in your Multiblock
 * Projector from then on. Each player learns from their own discs.
 */
public class BlueprintDiscItem extends Item {
    public BlueprintDiscItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    public static ItemStack of(VillagePlan.Unlock unlock) {
        ItemStack stack = new ItemStack(ModItems.BLUEPRINT_DISC.get());
        ModDataComponents.BLUEPRINT.set(stack, unlock.schematic());
        return stack;
    }

    public static @Nullable ResourceLocation blueprint(ItemStack stack) {
        return ModDataComponents.BLUEPRINT.get(stack);
    }

    /** True for a disc whose building {@code player} has not learned yet. */
    public static boolean unlearned(ItemStack stack, Player player) {
        ResourceLocation id = stack.getItem() instanceof BlueprintDiscItem ? blueprint(stack) : null;
        return id != null && !Blueprints.of(player).knows(id);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResult result = useItem(level, player, hand);
        return new InteractionResultHolder<>(result, player.getItemInHand(hand));
    }

    private InteractionResult useItem(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceLocation id = blueprint(stack);
        if (id == null) {
            return InteractionResult.PASS;
        }
        Component name = VillagePlan.unlock(id).map(VillagePlan.Unlock::displayName).orElse(Component.literal(id.toString()));
        if (Blueprints.of(player).knows(id)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.cyberfarm.blueprint.known", name), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            Blueprints.set((ServerPlayer) player, Blueprints.of(player).with(id));
            if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
            player.displayClientMessage(Component.translatable("message.cyberfarm.blueprint.learned", name), true);
        }
        player.playSound(SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.4F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        Consumer<Component> tooltip = lines::add;
        ResourceLocation id = blueprint(stack);
        VillagePlan.unlock(id != null ? id : new ResourceLocation("none")).ifPresent(unlock ->
                tooltip.accept(unlock.displayName().copy().withStyle(ChatFormatting.AQUA)));
        tooltip.accept(Component.translatable("tooltip.cyberfarm.blueprint_disc").withStyle(ChatFormatting.GRAY));
    }
}
