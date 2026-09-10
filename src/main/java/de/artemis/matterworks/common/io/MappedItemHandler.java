package de.artemis.matterworks.common.io;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Arrays;

public final class MappedItemHandler implements IItemHandler {
    private final IItemHandler delegate;
    private final boolean allowInput;
    private final boolean allowOutput;
    private final int[] slots;

    public MappedItemHandler(IItemHandler delegate, boolean allowInput, boolean allowOutput, int... slots) {
        this.delegate = delegate;
        this.allowInput = allowInput;
        this.allowOutput = allowOutput;
        this.slots = Arrays.copyOf(slots, slots.length);
    }

    @Override
    public int getSlots() {
        return slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return isMappedSlot(slot) ? delegate.getStackInSlot(slots[slot]) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return allowInput && isMappedSlot(slot) ? delegate.insertItem(slots[slot], stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return allowOutput && isMappedSlot(slot) ? delegate.extractItem(slots[slot], amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return isMappedSlot(slot) ? delegate.getSlotLimit(slots[slot]) : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return allowInput && isMappedSlot(slot) && delegate.isItemValid(slots[slot], stack);
    }

    private boolean isMappedSlot(int slot) {
        return slot >= 0 && slot < slots.length;
    }
}
