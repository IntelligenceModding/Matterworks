package de.artemis.matterworks.client.jei;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public record MatterConstructorJeiRecipe(
        ItemStack template,
        FluidStack refinedMatter,
        ItemStack output,
        FluidStack sludge,
        int processTime,
        int energyPerTick
) {
}
