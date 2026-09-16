package de.artemis.matterworks.client.jei;

import de.artemis.matterworks.common.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
import org.jetbrains.annotations.Nullable;

public class MatterConstructorRecipeCategory implements IRecipeCategory<MatterConstructorJeiRecipe> {
    private static final int WIDTH = 128;
    private static final int HEIGHT = 40;

    private final RecipeType<MatterConstructorJeiRecipe> recipeType;
    private final IDrawable background;
    private final IDrawable icon;
    private final JeiProgressArrow arrow;

    public MatterConstructorRecipeCategory(RecipeType<MatterConstructorJeiRecipe> recipeType, IGuiHelper guiHelper) {
        this.recipeType = recipeType;
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.MATTER_CONSTRUCTOR.get());
        this.arrow = new JeiProgressArrow(guiHelper.getRecipeArrow(), guiHelper.getRecipeArrowFilled());
    }

    @Override
    public RecipeType<MatterConstructorJeiRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return ModBlocks.MATTER_CONSTRUCTOR.get().getName();
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
    public void setRecipe(IRecipeLayoutBuilder builder, MatterConstructorJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 0, 2)
                .setStandardSlotBackground()
                .addItemStack(recipe.template());
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 2)
                .setStandardSlotBackground()
                .setFluidRenderer(recipe.refinedMatter().getAmount(), true, 16, 16)
                .addFluidStack(recipe.refinedMatter().getFluid(), recipe.refinedMatter().getAmount(), recipe.refinedMatter().getComponentsPatch());
        builder.addOutputSlot(82, 2)
                .setOutputSlotBackground()
                .addItemStack(recipe.output());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 108, 2)
                .setStandardSlotBackground()
                .setFluidRenderer(recipe.sludge().getAmount(), true, 16, 16)
                .addFluidStack(recipe.sludge().getFluid(), recipe.sludge().getAmount(), recipe.sludge().getComponentsPatch());
    }

    @Override
    public void draw(MatterConstructorJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 48, 2, recipe.processTime());
        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(recipe.processTime() + " ticks"), 0, 28, 0xFF555555, false);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(recipe.energyPerTick() + " FE/t"), 76, 28, 0xFF555555, false);
    }
}
