package de.artemis.matterworks.client.jei;

import net.minecraft.world.item.ItemStack;

public record PowerCrystalChargingJeiRecipe(ItemStack input, ItemStack output, int processTime, int energyPerTick) {
}
