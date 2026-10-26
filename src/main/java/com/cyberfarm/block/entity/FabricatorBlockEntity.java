package com.cyberfarm.block.entity;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.fluid.TankGroup;
import com.cyberfarm.menu.FabricatorMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Fabricator: makes the circuits every machine and frame is built on. Basic Circuits from copper,
 * redstone and gold (twice what a crafting table gives), Advanced Circuits etched with Acid,
 * Elite Circuits from uranium and diamond, etched with more acid.
 */
public class FabricatorBlockEntity extends MachineBlockEntity {
    public static final TagKey<Item> URANIUM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/uranium"));
    public static final int INPUTS = 3;
    public static final int OUTPUT = 3;
    public static final int TANK_CAPACITY = 8_000;
    public static final int ENERGY_PER_TICK = 40;
    private static final int CAPACITY = 20_000;
    private static final int MAX_INPUT = 1_000;

    /** One ingredient: what matches, how many, and what a recipe viewer shows. */
    public record Input(Predicate<ItemStack> test, int count, Supplier<ItemStack> shown) {
        static Input of(Item item, int count) {
            return new Input(stack -> stack.is(item), count, () -> new ItemStack(item, count));
        }

        static Input of(Supplier<? extends Item> item, int count) {
            return new Input(stack -> stack.is(item.get()), count, () -> new ItemStack(item.get(), count));
        }
    }

    public record Recipe(List<Input> inputs, int acid, Supplier<? extends Item> result, int count, int time) {
        public ItemStack output() {
            return new ItemStack(result.get(), count);
        }
    }

    public static final List<Recipe> RECIPES = List.of(
            new Recipe(List.of(Input.of(Items.COPPER_INGOT, 1), Input.of(Items.REDSTONE, 2), Input.of(Items.GOLD_NUGGET, 1)),
                    0, ModItems.BASIC_CIRCUIT, 2, 60),
            new Recipe(List.of(Input.of(ModItems.BASIC_CIRCUIT, 2), Input.of(Items.GOLD_INGOT, 1)),
                    250, ModItems.ADVANCED_CIRCUIT, 1, 100),
            new Recipe(List.of(Input.of(ModItems.ADVANCED_CIRCUIT, 1), Input.of(Items.DIAMOND, 1),
                    new Input(stack -> stack.is(URANIUM), 1, () -> new ItemStack(ModItems.URANIUM_INGOT.get()))),
                    500, ModItems.ELITE_CIRCUIT, 1, 160));

    private final GasTank acid = new GasTank(TANK_CAPACITY, resource -> resource.is(ModFluids.ACID.get().getFluidType()), this::setChanged);
    private final TankGroup fluids = TankGroup.of(new GasTank[] {acid});
    private int progress;
    private int recipeTime = 1;

    public FabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FABRICATOR.get(), pos, state, INPUTS + 1, CAPACITY, MAX_INPUT, 0);
    }

    public TankGroup getFluids() {
        return fluids;
    }

    @Override
    protected float progress() {
        return progress / (float) recipeTime;
    }

    /** The recipe the input slots hold, each ingredient in a slot of its own, or null. */
    private @Nullable Recipe current() {
        for (Recipe recipe : RECIPES) {
            if (slotsFor(recipe) != null && acid.amount() >= recipe.acid()) {
                return recipe;
            }
        }
        return null;
    }

    /** For each ingredient of {@code recipe}, the input slot that holds enough of it; null if any is missing. */
    private int @Nullable [] slotsFor(Recipe recipe) {
        int[] slots = new int[recipe.inputs().size()];
        boolean[] used = new boolean[INPUTS];
        for (int i = 0; i < slots.length; i++) {
            Input input = recipe.inputs().get(i);
            slots[i] = -1;
            for (int slot = 0; slot < INPUTS && slots[i] < 0; slot++) {
                ItemStack stack = getStack(slot);
                if (!used[slot] && input.test().test(stack) && stack.getCount() >= input.count()) {
                    slots[i] = slot;
                    used[slot] = true;
                }
            }
            if (slots[i] < 0) {
                return null;
            }
        }
        return slots;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        Recipe recipe = current();
        boolean canRun = recipe != null && hasRoomFor(OUTPUT, recipe.result().get())
                && getStack(OUTPUT).getCount() + recipe.count() <= recipe.output().getMaxStackSize();
        if (canRun && energy.consume(ENERGY_PER_TICK)) {
            recipeTime = recipe.time();
            if (++progress >= recipe.time()) {
                progress = 0;
                int[] slots = slotsFor(recipe);
                for (int i = 0; i < slots.length; i++) {
                    ItemStack stack = getStack(slots[i]);
                    setStack(slots[i], stack.copyWithCount(stack.getCount() - recipe.inputs().get(i).count()));
                }
                acid.drain(recipe.acid());
                ItemStack out = getStack(OUTPUT);
                setStack(OUTPUT, out.isEmpty() ? recipe.output() : out.copyWithCount(out.getCount() + recipe.count()));
                level.playSound(null, pos, ModSounds.ASSEMBLER_WORK.get(), SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            setChanged();
        } else if (!canRun && progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    /** Whether {@code stack} is an ingredient of any recipe. */
    public static boolean isIngredient(ItemStack stack) {
        return RECIPES.stream().anyMatch(recipe -> recipe.inputs().stream().anyMatch(input -> input.test().test(stack)));
    }

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return index < INPUTS && isIngredient(resource.toStack());
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> progress;
            case 1 -> recipeTime;
            case 2 -> acid.amount();
            case 3 -> acid.fluidCode();
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 4;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        acid.serialize(output.child("acid"));
        output.putInt("progress", progress);
        output.putInt("recipe_time", recipeTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        acid.deserialize(input.childOrEmpty("acid"));
        progress = input.getIntOr("progress", 0);
        recipeTime = Math.max(1, input.getIntOr("recipe_time", 1));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FabricatorMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
