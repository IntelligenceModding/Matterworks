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

public class MatterRecyclerRecipeCategory implements IRecipeCategory<MatterRecyclerJeiRecipe> {
    private static final int WIDTH = 110;
    private static final int HEIGHT = 40;

    private final RecipeType<MatterRecyclerJeiRecipe> recipeType;
    private final IDrawable background;
    private final IDrawable icon;
    private final JeiProgressArrow arrow;

    public MatterRecyclerRecipeCategory(RecipeType<MatterRecyclerJeiRecipe> recipeType, IGuiHelper guiHelper) {
        this.recipeType = recipeType;
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.MATTER_RECYCLER.get());
        this.arrow = new JeiProgressArrow(guiHelper.getRecipeArrow(), guiHelper.getRecipeArrowFilled());
    }

    @Override
    public RecipeType<MatterRecyclerJeiRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return ModBlocks.MATTER_RECYCLER.get().getName();
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
    public void setRecipe(IRecipeLayoutBuilder builder, MatterRecyclerJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 0, 2)
                .setStandardSlotBackground()
                .addItemStack(recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 62, 2)
                .setStandardSlotBackground()
                .setFluidRenderer(recipe.output().getAmount(), true, 16, 16)
                .addFluidStack(recipe.output().getFluid(), recipe.output().getAmount(), recipe.output().getComponentsPatch());
    }

    @Override
    public void draw(MatterRecyclerJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 28, 2, recipe.processTime());
        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(recipe.processTime() + " ticks"), 0, 28, 0xFF555555, false);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(recipe.energyPerTick() + " FE/t"), 62, 28, 0xFF555555, false);
    }
}
