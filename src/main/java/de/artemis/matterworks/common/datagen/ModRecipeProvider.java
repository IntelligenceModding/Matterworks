package de.artemis.matterworks.common.datagen;

import de.artemis.matterworks.Matterworks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
    }

    @SuppressWarnings("all")
    private static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, path);
    }
}
