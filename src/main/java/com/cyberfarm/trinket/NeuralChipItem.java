package com.cyberfarm.trinket;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.GlobalPos;
import com.cyberfarm.block.entity.DroneDockBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.cyberfarm.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Neural Chip: an implant on the back of the neck that runs up to {@link ChipProgram#SLOTS} programs, installed at the
 * Trinket Table. Zoom and Night Vision have keys; the others run on their own.
 */
public class NeuralChipItem extends TrinketItem {
    public NeuralChipItem(Properties properties) {
        super(properties, TrinketType.NECK);
    }

    public static int programMask(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CHIP_PROGRAMS.get(), 0);
    }

    public static List<ChipProgram> programs(ItemStack stack) {
        List<ChipProgram> programs = new ArrayList<>();
        for (ChipProgram program : ChipProgram.values()) {
            if ((programMask(stack) & program.bit()) != 0) {
                programs.add(program);
            }
        }
        return programs;
    }

    public static boolean has(ItemStack stack, ChipProgram program) {
        return (programMask(stack) & program.bit()) != 0;
    }

    /** Whether one more program fits, and it is not there yet. */
    public static boolean canInstall(ItemStack stack, ChipProgram program) {
        return !has(stack, program) && Integer.bitCount(programMask(stack)) < ChipProgram.SLOTS;
    }

    public static ItemStack install(ItemStack stack, ChipProgram program) {
        ItemStack copy = stack.copyWithCount(1);
        copy.set(ModDataComponents.CHIP_PROGRAMS.get(), programMask(stack) | program.bit());
        return copy;
    }

    /** Whether a program with an on/off key (Night Vision) is switched on. */
    public static boolean isOn(ItemStack stack, ChipProgram program) {
        return has(stack, program) && (stack.getOrDefault(ModDataComponents.CHIP_ACTIVE.get(), 0) & program.bit()) != 0;
    }

    public static ItemStack toggle(ItemStack stack, ChipProgram program) {
        ItemStack copy = stack.copy();
        copy.set(ModDataComponents.CHIP_ACTIVE.get(), stack.getOrDefault(ModDataComponents.CHIP_ACTIVE.get(), 0) ^ program.bit());
        return copy;
    }

    /** The chip the player wears, or empty. */
    public static ItemStack worn(Player player) {
        return Trinkets.worn(player, TrinketType.NECK, NeuralChipItem.class);
    }

    /** Whether the player wears a chip running {@code program} and, for a program that uses FE, has power for it. */
    public static boolean runs(Player player, ChipProgram program) {
        return has(worn(player), program) && (!program.needsPower() || UtilityBeltItem.energy(player) > 0);
    }

    /**
     * Right-click a Drone Dock with a chip that has the Taxi Call program: the call goes to that dock's drone from then
     * on. Before the block, so no GUI opens; without the program it does nothing and the dock opens as usual.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        if (!has(stack, ChipProgram.TAXI_CALL) || !(level.getBlockEntity(context.getClickedPos()) instanceof DroneDockBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            stack.set(ModDataComponents.CHIP_DOCK.get(), GlobalPos.of(level.dimension(), context.getClickedPos().immutable()));
            if (context.getPlayer() != null) {
                context.getPlayer().sendOverlayMessage(Component.translatable("message.cyberfarm.neural_chip.dock_set"));
            }
            level.playSound(null, context.getClickedPos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, 1.4F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        GlobalPos dock = stack.get(ModDataComponents.CHIP_DOCK.get());
        // The dock line only with the Taxi Call, the program that uses it
        if (has(stack, ChipProgram.TAXI_CALL)) {
            tooltip.accept(dock == null ? Component.translatable("tooltip.cyberfarm.neural_chip.no_dock").withStyle(ChatFormatting.DARK_GRAY)
                    : Component.translatable("tooltip.cyberfarm.neural_chip.dock", dock.pos().getX(), dock.pos().getY(), dock.pos().getZ())
                            .withStyle(ChatFormatting.GRAY));
        }
        List<ChipProgram> programs = programs(stack);
        tooltip.accept(Component.translatable("tooltip.cyberfarm.neural_chip.programs", programs.size(), ChipProgram.SLOTS)
                .withStyle(ChatFormatting.AQUA));
        for (ChipProgram program : programs) {
            tooltip.accept(Component.literal(" - ").append(Component.translatable(program.translationKey())).withStyle(ChatFormatting.GRAY));
        }
    }
}
