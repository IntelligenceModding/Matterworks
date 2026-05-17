package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.recipe.MatterFilterCopyRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Matterworks.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<MatterFilterCopyRecipe>> MATTER_FILTER_COPY =
            RECIPE_SERIALIZERS.register("matter_filter_copy", () -> new SimpleCraftingRecipeSerializer<>(MatterFilterCopyRecipe::new));

    private ModRecipeSerializers() {
    }

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
