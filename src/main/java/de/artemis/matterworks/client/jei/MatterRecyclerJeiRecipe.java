package de.artemis.matterworks.client.jei;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public record MatterRecyclerJeiRecipe(ItemStack input, FluidStack output, int processTime, int energyPerTick) {
}
