package de.artemis.matterworks.common.recipe;

import de.artemis.matterworks.common.filter.MatterFilterData;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.registry.ModRecipeSerializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class MatterFilterCopyRecipe extends CustomRecipe {
    public MatterFilterCopyRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack configured = ItemStack.EMPTY;
        ItemStack empty = ItemStack.EMPTY;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() != ModItems.MATTER_ITEM_FILTER.get() && stack.getItem() != ModItems.MATTER_FLUID_FILTER.get()) {
                return false;
            }
            boolean configuredStack = stack.getItem() == ModItems.MATTER_ITEM_FILTER.get()
                    ? MatterFilterData.hasItemEntries(stack)
                    : MatterFilterData.hasFluidEntries(stack);
            if (configuredStack) {
                if (!configured.isEmpty()) {
                    return false;
                }
                configured = stack;
            } else {
                if (!empty.isEmpty()) {
                    return false;
                }
                empty = stack;
            }
        }

        return !configured.isEmpty() && !empty.isEmpty() && ItemStack.isSameItemSameComponents(configured.copyWithCount(1), empty.copyWithCount(1));
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && (stack.getItem() == ModItems.MATTER_ITEM_FILTER.get() || stack.getItem() == ModItems.MATTER_FLUID_FILTER.get())) {
                boolean configuredStack = stack.getItem() == ModItems.MATTER_ITEM_FILTER.get()
                        ? MatterFilterData.hasItemEntries(stack)
                        : MatterFilterData.hasFluidEntries(stack);
                if (!configuredStack) {
                    continue;
                }
                ItemStack result = stack.copy();
                result.setCount(2);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.MATTER_FILTER_COPY.get();
    }
}
