package de.artemis.matterworks.client.jei;

import de.artemis.matterworks.common.recipe.FluidMachineRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FluidMachineRecipeCategory implements IRecipeCategory<RecipeHolder<FluidMachineRecipe>> {
    private static final int WIDTH = 128;
    private static final int HEIGHT = 40;

    private final RecipeType<RecipeHolder<FluidMachineRecipe>> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;
    private final JeiProgressArrow arrow;

    public FluidMachineRecipeCategory(
            RecipeType<RecipeHolder<FluidMachineRecipe>> recipeType,
            DeferredBlock<? extends Block> machineBlock,
            IGuiHelper guiHelper
    ) {
        this.recipeType = recipeType;
        this.title = machineBlock.get().getName();
        this.icon = guiHelper.createDrawableItemLike(machineBlock.get());
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.arrow = new JeiProgressArrow(guiHelper.getRecipeArrow(), guiHelper.getRecipeArrowFilled());
    }

    @Override
    public RecipeType<RecipeHolder<FluidMachineRecipe>> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    @Deprecated
    public @Nullable IDrawable getBackground() {
        return background;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<FluidMachineRecipe> holder, IFocusGroup focuses) {
        FluidMachineRecipe recipe = holder.value();
        IRecipeSlotBuilder inputSlot = builder.addSlot(RecipeIngredientRole.INPUT, 0, 2)
                .setStandardSlotBackground()
                .setFluidRenderer(recipe.inputAmount(), true, 16, 16);
        for (FluidStack stack : recipe.input().getStacks()) {
            inputSlot.addFluidStack(stack.getFluid(), recipe.inputAmount(), stack.getComponentsPatch());
        }

        int outputX = 62;
        ItemStack itemResult = recipe.itemResult();
        if (!itemResult.isEmpty()) {
            builder.addOutputSlot(outputX, 2)
                    .setOutputSlotBackground()
                    .addItemStack(itemResult);
            outputX += 28;
        }

        List<FluidStack> fluidResults = recipe.fluidResults();
        for (FluidStack result : fluidResults) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, outputX, 2)
                    .setStandardSlotBackground()
                    .setFluidRenderer(result.getAmount(), true, 16, 16)
                    .addFluidStack(result.getFluid(), result.getAmount(), result.getComponentsPatch());
            outputX += 24;
        }
    }

    @Override
    public void draw(RecipeHolder<FluidMachineRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        FluidMachineRecipe recipe = holder.value();
        arrow.draw(guiGraphics, 28, 2, recipe.processTime());
        Component time = Component.literal(recipe.processTime() + " ticks");
        Component energy = Component.literal(recipe.energyPerTick() + " FE/t");
        guiGraphics.drawString(Minecraft.getInstance().font, time, 0, 28, 0xFF555555, false);
        guiGraphics.drawString(Minecraft.getInstance().font, energy, 74, 28, 0xFF555555, false);
    }
}
