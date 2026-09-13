package de.artemis.matterworks.common.io;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class ItemHandlerRouting {
    private ItemHandlerRouting() {
    }

    public static ItemStack insertIntoAnySlot(IItemHandler handler, int preferredSlot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remainder = stack;
        if (isSlotInRange(handler, preferredSlot)) {
            remainder = handler.insertItem(preferredSlot, remainder, simulate);
            if (remainder.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (slot == preferredSlot) {
                continue;
            }
            remainder = handler.insertItem(slot, remainder, simulate);
            if (remainder.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remainder;
    }

    public static ItemStack extractFromAnySlot(IItemHandler handler, int preferredSlot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        if (isSlotInRange(handler, preferredSlot)) {
            ItemStack extracted = handler.extractItem(preferredSlot, amount, simulate);
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (slot == preferredSlot) {
                continue;
            }
            ItemStack extracted = handler.extractItem(slot, amount, simulate);
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean canInsertIntoAnySlot(IItemHandler handler, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.isItemValid(slot, stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSlotInRange(IItemHandler handler, int slot) {
        return slot >= 0 && slot < handler.getSlots();
    }
}
