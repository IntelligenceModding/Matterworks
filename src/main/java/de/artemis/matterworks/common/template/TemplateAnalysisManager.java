package de.artemis.matterworks.common.template;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TemplateAnalysisManager {
    private static final int MIN_REQUIRED_COUNT = 1;
    private static final int MAX_REQUIRED_COUNT = 64;

    private TemplateAnalysisManager() {
    }

    public static int getRequiredItemCount(ItemStack stack, Level level) {
        if (stack.isEmpty() || level == null) {
            return MIN_REQUIRED_COUNT;
        }

        RecipeManager recipeManager = level.getRecipeManager();
        HolderLookup.Provider registries = level.registryAccess();
        Map<Item, Integer> memo = new HashMap<>();
        Set<Item> visiting = new HashSet<>();
        return computeRequiredCount(stack.getItem(), recipeManager, registries, memo, visiting);
    }

    private static int computeRequiredCount(
            Item item,
            RecipeManager recipeManager,
            HolderLookup.Provider registries,
            Map<Item, Integer> memo,
            Set<Item> visiting
    ) {
        Integer cached = memo.get(item);
        if (cached != null) {
            return cached;
        }

        if (!visiting.add(item)) {
            return MIN_REQUIRED_COUNT;
        }

        int bestRequiredCount = Integer.MAX_VALUE;
        for (RecipeHolder<CraftingRecipe> holder : recipeManager.getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = holder.value();
            ItemStack resultStack = recipe.getResultItem(registries);
            if (resultStack.isEmpty() || resultStack.getItem() != item) {
                continue;
            }

            int outputCount = Math.max(1, resultStack.getCount());
            int ingredientComplexity = 0;
            int ingredientCount = 0;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) {
                    continue;
                }

                ingredientCount++;
                int cheapestIngredient = Integer.MAX_VALUE;
                for (ItemStack option : ingredient.getItems()) {
                    if (option.isEmpty()) {
                        continue;
                    }

                    cheapestIngredient = Math.min(
                            cheapestIngredient,
                            computeRequiredCount(option.getItem(), recipeManager, registries, memo, visiting)
                    );
                }

                ingredientComplexity += cheapestIngredient == Integer.MAX_VALUE ? MIN_REQUIRED_COUNT : cheapestIngredient;
            }

            if (ingredientCount == 0) {
                bestRequiredCount = Math.min(bestRequiredCount, MIN_REQUIRED_COUNT);
                continue;
            }

            int recipeRequiredCount = 1 + (int) Math.ceil(ingredientComplexity / (double) outputCount / 4.0D);
            bestRequiredCount = Math.min(bestRequiredCount, recipeRequiredCount);
        }

        visiting.remove(item);

        int resolved = bestRequiredCount == Integer.MAX_VALUE ? MIN_REQUIRED_COUNT : bestRequiredCount;
        resolved = Math.max(MIN_REQUIRED_COUNT, Math.min(MAX_REQUIRED_COUNT, resolved));
        memo.put(item, resolved);
        return resolved;
    }
}
