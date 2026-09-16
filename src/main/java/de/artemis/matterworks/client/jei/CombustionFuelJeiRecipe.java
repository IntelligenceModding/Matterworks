package de.artemis.matterworks.client.jei;

import net.minecraft.world.item.ItemStack;

public record CombustionFuelJeiRecipe(ItemStack fuel, int burnTime, int energyPerTick) {
    public int totalEnergy() {
        return burnTime * energyPerTick;
    }
}
