package de.artemis.matterworks.common.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public abstract class AbstractBaseMenu extends AbstractContainerMenu {
    protected AbstractBaseMenu(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    protected void addPlayerInventorySlots(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
    }

    protected void addPlayerHotbarSlots(Inventory inventory, int left, int top) {
        for (int slot = 0; slot < 9; slot++) {
            this.addSlot(new Slot(inventory, slot, left + slot * 18, top));
        }
    }

    protected boolean moveToContainerSlot(ItemStack sourceStack, int machineSlotCount, int containerSlot) {
        int menuSlotIndex = findSlotIndexForContainerSlot(machineSlotCount, containerSlot);
        return menuSlotIndex >= 0 && this.moveItemStackTo(sourceStack, menuSlotIndex, menuSlotIndex + 1, false);
    }

    protected boolean moveToContainerSlotRange(ItemStack sourceStack, int machineSlotCount, int containerSlotStart, int containerSlotCount) {
        boolean moved = false;
        int containerSlotEnd = containerSlotStart + containerSlotCount;
        for (int slotIndex = 0; slotIndex < machineSlotCount && !sourceStack.isEmpty(); slotIndex++) {
            Slot slot = this.slots.get(slotIndex);
            if (!(slot instanceof SlotItemHandler slotItemHandler)) {
                continue;
            }
            int containerSlot = slotItemHandler.getContainerSlot();
            if (containerSlot < containerSlotStart || containerSlot >= containerSlotEnd) {
                continue;
            }
            moved = this.moveItemStackTo(sourceStack, slotIndex, slotIndex + 1, false) || moved;
        }
        return moved;
    }

    protected int findSlotIndexForContainerSlot(int machineSlotCount, int containerSlot) {
        for (int slotIndex = 0; slotIndex < machineSlotCount; slotIndex++) {
            Slot slot = this.slots.get(slotIndex);
            if (slot instanceof SlotItemHandler slotItemHandler && slotItemHandler.getContainerSlot() == containerSlot) {
                return slotIndex;
            }
        }
        return -1;
    }
}
