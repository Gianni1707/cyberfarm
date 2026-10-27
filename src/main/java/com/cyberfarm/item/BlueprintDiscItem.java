package com.cyberfarm.item;

import java.util.function.Consumer;

import com.cyberfarm.registry.ModDataComponents;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModVillagers;
import com.cyberfarm.village.Blueprints;
import com.cyberfarm.village.VillagePlan;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
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
        stack.set(ModDataComponents.BLUEPRINT.get(), unlock.schematic());
        return stack;
    }

    public static @Nullable Identifier blueprint(ItemStack stack) {
        return stack.get(ModDataComponents.BLUEPRINT.get());
    }

    /** True for a disc whose building {@code player} has not learned yet. */
    public static boolean unlearned(ItemStack stack, Player player) {
        Identifier id = stack.getItem() instanceof BlueprintDiscItem ? blueprint(stack) : null;
        return id != null && !Blueprints.of(player).knows(id);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Identifier id = blueprint(stack);
        if (id == null) {
            return InteractionResult.PASS;
        }
        Component name = VillagePlan.unlock(id).map(VillagePlan.Unlock::displayName).orElse(Component.literal(id.toString()));
        if (Blueprints.of(player).knows(id)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.cyberfarm.blueprint.known", name));
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            player.setData(ModVillagers.BLUEPRINTS, Blueprints.of(player).with(id));
            stack.consume(1, player);
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.blueprint.learned", name));
        }
        player.playSound(SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.4F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        Identifier id = blueprint(stack);
        VillagePlan.unlock(id != null ? id : Identifier.withDefaultNamespace("none")).ifPresent(unlock ->
                tooltip.accept(unlock.displayName().copy().withStyle(ChatFormatting.AQUA)));
        tooltip.accept(Component.translatable("tooltip.cyberfarm.blueprint_disc").withStyle(ChatFormatting.GRAY));
    }
}
