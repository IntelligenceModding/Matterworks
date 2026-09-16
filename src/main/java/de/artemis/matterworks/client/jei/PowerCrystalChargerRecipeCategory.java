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

public class PowerCrystalChargerRecipeCategory implements IRecipeCategory<PowerCrystalChargingJeiRecipe> {
    private static final int WIDTH = 112;
    private static final int HEIGHT = 40;

    private final RecipeType<PowerCrystalChargingJeiRecipe> recipeType;
    private final IDrawable background;
    private final IDrawable icon;
    private final JeiProgressArrow arrow;

    public PowerCrystalChargerRecipeCategory(RecipeType<PowerCrystalChargingJeiRecipe> recipeType, IGuiHelper guiHelper) {
        this.recipeType = recipeType;
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.POWER_CRYSTAL_CHARGER.get());
        this.arrow = new JeiProgressArrow(guiHelper.getRecipeArrow(), guiHelper.getRecipeArrowFilled());
    }

    @Override
    public RecipeType<PowerCrystalChargingJeiRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return ModBlocks.POWER_CRYSTAL_CHARGER.get().getName();
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
    public void setRecipe(IRecipeLayoutBuilder builder, PowerCrystalChargingJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 0, 2)
                .setStandardSlotBackground()
                .addItemStack(recipe.input());
        builder.addOutputSlot(62, 2)
                .setOutputSlotBackground()
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(PowerCrystalChargingJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 28, 2, recipe.processTime());
        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("+1% charge"), 0, 28, 0xFF555555, false);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(recipe.energyPerTick() + " FE/t"), 64, 28, 0xFF555555, false);
    }
}
