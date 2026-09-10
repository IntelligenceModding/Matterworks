package de.artemis.matterworks.common.menu.slot;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.Supplier;

public class GhostHintSlot extends SlotItemHandler implements GhostItemSlot {
    private final Supplier<ItemStack> ghostItemStack;

    public GhostHintSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, Supplier<ItemStack> ghostItemStack) {
        super(itemHandler, index, xPosition, yPosition);
        this.ghostItemStack = ghostItemStack;
    }

    @Override
    public ItemStack getGhostItemStack() {
        return ghostItemStack.get().copy();
    }
}
