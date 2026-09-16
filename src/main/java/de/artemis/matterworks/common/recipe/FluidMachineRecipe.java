package de.artemis.matterworks.common.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

import java.util.List;
import java.util.function.Supplier;

public class FluidMachineRecipe implements Recipe<FluidMachineRecipe.Input> {
    private static final Codec<Integer> POSITIVE_INT = Codec.INT.validate(value -> value > 0
            ? com.mojang.serialization.DataResult.success(value)
            : com.mojang.serialization.DataResult.error(() -> "Value must be positive"));

    private final RecipeType<FluidMachineRecipe> type;
    private final RecipeSerializer<FluidMachineRecipe> serializer;
    private final FluidIngredient input;
    private final int inputAmount;
    private final ItemStack itemResult;
    private final List<FluidStack> fluidResults;
    private final int processTime;
    private final int energyPerTick;

    public FluidMachineRecipe(
            RecipeType<FluidMachineRecipe> type,
            RecipeSerializer<FluidMachineRecipe> serializer,
            FluidIngredient input,
            int inputAmount,
            ItemStack itemResult,
            List<FluidStack> fluidResults,
            int processTime,
            int energyPerTick
    ) {
        this.type = type;
        this.serializer = serializer;
        this.input = input;
        this.inputAmount = inputAmount;
        this.itemResult = itemResult;
        this.fluidResults = List.copyOf(fluidResults);
        this.processTime = processTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(Input input, Level level) {
        return input.fluid().getAmount() >= inputAmount && this.input.test(input.fluid());
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return itemResult.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return itemResult.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    @Override
    public RecipeType<?> getType() {
        return type;
    }

    public FluidIngredient input() {
        return input;
    }

    public int inputAmount() {
        return inputAmount;
    }

    public ItemStack itemResult() {
        return itemResult.copy();
    }

    public List<FluidStack> fluidResults() {
        return fluidResults.stream().map(FluidStack::copy).toList();
    }

    public int processTime() {
        return processTime;
    }

    public int energyPerTick() {
        return energyPerTick;
    }

    public FluidStack firstFluidResult() {
        return fluidResults.isEmpty() ? FluidStack.EMPTY : fluidResults.getFirst().copy();
    }

    public FluidStack fluidResult(int index) {
        return index < 0 || index >= fluidResults.size() ? FluidStack.EMPTY : fluidResults.get(index).copy();
    }

    public record Input(FluidStack fluid) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }
    }

    public static class Serializer implements RecipeSerializer<FluidMachineRecipe> {
        private final MapCodec<FluidMachineRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, FluidMachineRecipe> streamCodec;

        public Serializer(Supplier<RecipeType<FluidMachineRecipe>> type) {
            this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    FluidIngredient.CODEC_NON_EMPTY.fieldOf("ingredient").forGetter(FluidMachineRecipe::input),
                    POSITIVE_INT.fieldOf("input_amount").forGetter(FluidMachineRecipe::inputAmount),
                    ItemStack.OPTIONAL_CODEC.optionalFieldOf("item_result", ItemStack.EMPTY).forGetter(recipe -> recipe.itemResult),
                    FluidStack.CODEC.listOf().optionalFieldOf("fluid_results", List.of()).forGetter(recipe -> recipe.fluidResults),
                    POSITIVE_INT.fieldOf("process_time").forGetter(FluidMachineRecipe::processTime),
                    POSITIVE_INT.fieldOf("energy_per_tick").forGetter(FluidMachineRecipe::energyPerTick)
            ).apply(instance, (input, inputAmount, itemResult, fluidResults, processTime, energyPerTick) ->
                    new FluidMachineRecipe(type.get(), this, input, inputAmount, itemResult, fluidResults, processTime, energyPerTick)));
            this.streamCodec = ByteBufCodecs.fromCodecWithRegistries(this.codec.codec());
        }

        @Override
        public MapCodec<FluidMachineRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FluidMachineRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
