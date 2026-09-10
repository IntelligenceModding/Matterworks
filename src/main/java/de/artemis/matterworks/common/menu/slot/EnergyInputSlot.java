package de.artemis.matterworks.common.menu.slot;

import de.artemis.matterworks.common.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Supplier;

public class EnergyInputSlot extends InputSlot implements GhostItemSlot {
    private final Supplier<ItemStack> ghostItemStack;

    public EnergyInputSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        this(itemHandler, index, xPosition, yPosition, () -> ModItems.MATTER_POWER_BANK.get().createChargedStack());
    }

    public EnergyInputSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, Supplier<ItemStack> ghostItemStack) {
        super(itemHandler, index, xPosition, yPosition);
        this.ghostItemStack = ghostItemStack;
    }

    @Override
    public ItemStack getGhostItemStack() {
        return ghostItemStack.get().copy();
    }
}
