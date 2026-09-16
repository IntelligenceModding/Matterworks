package de.artemis.matterworks.client.jei;

import de.artemis.matterworks.common.recipe.MatterFilterCopyRecipe;
import de.artemis.matterworks.common.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public class MatterFilterCopyCategoryExtension implements ICraftingCategoryExtension<MatterFilterCopyRecipe> {
    @Override
    public void setRecipe(
            RecipeHolder<MatterFilterCopyRecipe> recipeHolder,
            IRecipeLayoutBuilder builder,
            ICraftingGridHelper craftingGridHelper,
            IFocusGroup focuses
    ) {
        ItemStack copiedItemFilters = ModItems.MATTER_ITEM_FILTER.get().getDefaultInstance().copyWithCount(2);
        ItemStack copiedFluidFilters = ModItems.MATTER_FLUID_FILTER.get().getDefaultInstance().copyWithCount(2);
        List<List<ItemStack>> inputs = List.of(
                List.of(ModItems.MATTER_ITEM_FILTER.get().getDefaultInstance(), ModItems.MATTER_FLUID_FILTER.get().getDefaultInstance()),
                List.of(ModItems.MATTER_ITEM_FILTER.get().getDefaultInstance(), ModItems.MATTER_FLUID_FILTER.get().getDefaultInstance())
        );
        craftingGridHelper.createAndSetInputs(builder, inputs, 0, 0);
        craftingGridHelper.createAndSetOutputs(builder, List.of(copiedItemFilters, copiedFluidFilters));
    }
}
