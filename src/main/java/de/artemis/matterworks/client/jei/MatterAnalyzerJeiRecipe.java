package de.artemis.matterworks.client.jei;

import net.minecraft.world.item.ItemStack;

public record MatterAnalyzerJeiRecipe(ItemStack input, ItemStack output, int requiredItems, int processTime, int energyPerTick) {
}
