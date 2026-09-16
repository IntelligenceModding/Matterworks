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

public class CombustionGeneratorRecipeCategory implements IRecipeCategory<CombustionFuelJeiRecipe> {
    private static final int WIDTH = 114;
    private static final int HEIGHT = 40;

    private final RecipeType<CombustionFuelJeiRecipe> recipeType;
    private final IDrawable background;
    private final IDrawable icon;
    private final JeiTimedFlame flame;

    public CombustionGeneratorRecipeCategory(RecipeType<CombustionFuelJeiRecipe> recipeType, IGuiHelper guiHelper) {
        this.recipeType = recipeType;
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.COMBUSTION_GENERATOR.get());
        this.flame = new JeiTimedFlame(guiHelper.getRecipeFlameEmpty(), guiHelper.getRecipeFlameFilled());
    }

    @Override
    public RecipeType<CombustionFuelJeiRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return ModBlocks.COMBUSTION_GENERATOR.get().getName();
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
    public void setRecipe(IRecipeLayoutBuilder builder, CombustionFuelJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 0, 2)
                .setStandardSlotBackground()
                .addItemStack(recipe.fuel());
    }

    @Override
    public void draw(CombustionFuelJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        flame.draw(guiGraphics, 27, 3, recipe.burnTime());
        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(recipe.burnTime() + " ticks"), 50, 0, 0xFF555555, false);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(recipe.totalEnergy() + " FE"), 50, 14, 0xFF555555, false);
    }
}
