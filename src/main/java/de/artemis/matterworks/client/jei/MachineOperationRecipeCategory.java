package de.artemis.matterworks.client.jei;

import de.artemis.matterworks.common.registry.ModBlocks;
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
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class MachineOperationRecipeCategory implements IRecipeCategory<MachineOperationJeiRecipe> {
    private static final int WIDTH = 154;
    private static final int HEIGHT = 68;
    private static final int SLOT_SPACING = 20;

    private final RecipeType<MachineOperationJeiRecipe> recipeType;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public MachineOperationRecipeCategory(RecipeType<MachineOperationJeiRecipe> recipeType, IGuiHelper guiHelper) {
        this.recipeType = recipeType;
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.MATTER_PYLON.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<MachineOperationJeiRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Machine Operations");
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
    public void setRecipe(IRecipeLayoutBuilder builder, MachineOperationJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 0, 15)
                .setStandardSlotBackground()
                .addItemStack(recipe.machine());

        int inputY = firstSlotY(recipe.itemInputs().size() + recipe.fluidInputs().size());
        for (ItemStack stack : recipe.itemInputs()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 30, inputY)
                    .setStandardSlotBackground()
                    .addItemStack(stack);
            inputY += SLOT_SPACING;
        }
        for (FluidStack stack : recipe.fluidInputs()) {
            addFluidSlot(builder, RecipeIngredientRole.INPUT, stack, 30, inputY);
            inputY += SLOT_SPACING;
        }

        int outputY = firstSlotY(recipe.itemOutputs().size() + recipe.fluidOutputs().size());
        for (ItemStack stack : recipe.itemOutputs()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 104, outputY)
                    .setOutputSlotBackground()
                    .addItemStack(stack);
            outputY += SLOT_SPACING;
        }
        for (FluidStack stack : recipe.fluidOutputs()) {
            addFluidSlot(builder, RecipeIngredientRole.OUTPUT, stack, 104, outputY);
            outputY += SLOT_SPACING;
        }
    }

    @Override
    public void draw(MachineOperationJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 70, 17);
        for (int index = 0; index < recipe.notes().size() && index < 2; index++) {
            guiGraphics.drawString(Minecraft.getInstance().font, recipe.notes().get(index), 0, 46 + index * 10, 0xFF555555, false);
        }
    }

    private static void addFluidSlot(
            IRecipeLayoutBuilder builder,
            RecipeIngredientRole role,
            FluidStack stack,
            int x,
            int y
    ) {
        IRecipeSlotBuilder slot = builder.addSlot(role, x, y)
                .setStandardSlotBackground()
                .setFluidRenderer(stack.getAmount(), true, 16, 16);
        slot.addFluidStack(stack.getFluid(), stack.getAmount(), stack.getComponentsPatch());
    }

    private static int firstSlotY(int slotCount) {
        return Math.max(5, 17 - Math.max(0, slotCount - 1) * SLOT_SPACING / 2);
    }
}
