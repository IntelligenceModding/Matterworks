package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.recipe.FluidMachineRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Matterworks.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<FluidMachineRecipe>> GRAVITIC_CONDENSING =
            register("gravitic_condensing");
    public static final DeferredHolder<RecipeType<?>, RecipeType<FluidMachineRecipe>> MATTER_SEPARATING =
            register("matter_separating");
    public static final DeferredHolder<RecipeType<?>, RecipeType<FluidMachineRecipe>> MATTER_STABILIZING =
            register("matter_stabilizing");

    private ModRecipeTypes() {
    }

    private static DeferredHolder<RecipeType<?>, RecipeType<FluidMachineRecipe>> register(String name) {
        return RECIPE_TYPES.register(name, () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
    }
}
