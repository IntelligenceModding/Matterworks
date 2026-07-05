package de.artemis.matterworks.common.blockentity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.Predicate;

public final class LegacyInventoryMigration {
    private LegacyInventoryMigration() {
    }

    public static void moveIfMatches(ItemStackHandler itemHandler, int fromSlot, int targetSlot, Predicate<ItemStack> predicate) {
        ItemStack stack = itemHandler.getStackInSlot(fromSlot);
        if (stack.isEmpty() || !predicate.test(stack) || !itemHandler.getStackInSlot(targetSlot).isEmpty()) {
            return;
        }

        itemHandler.setStackInSlot(targetSlot, stack.copy());
        itemHandler.setStackInSlot(fromSlot, ItemStack.EMPTY);
    }
}
