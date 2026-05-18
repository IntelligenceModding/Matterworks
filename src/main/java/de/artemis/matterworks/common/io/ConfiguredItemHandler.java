package de.artemis.matterworks.common.io;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Supplier;

public final class ConfiguredItemHandler implements IItemHandler {
    private final Supplier<SideAccessMode> modeSupplier;
    private final Supplier<IItemHandler> inputSupplier;
    private final Supplier<IItemHandler> outputSupplier;

    public ConfiguredItemHandler(Supplier<SideAccessMode> modeSupplier, Supplier<IItemHandler> inputSupplier, Supplier<IItemHandler> outputSupplier) {
        this.modeSupplier = modeSupplier;
        this.inputSupplier = inputSupplier;
        this.outputSupplier = outputSupplier;
    }

    @Override
    public int getSlots() {
        return switch (getMode()) {
            case DISABLED -> 0;
            case INPUT -> getInput().getSlots();
            case OUTPUT -> getOutput().getSlots();
            case BOTH -> getInput().getSlots() + getOutput().getSlots();
        };
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return switch (getMode()) {
            case DISABLED -> ItemStack.EMPTY;
            case INPUT -> getInput().getStackInSlot(slot);
            case OUTPUT -> getOutput().getStackInSlot(slot);
            case BOTH -> getStackInCombinedSlot(slot);
        };
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return switch (getMode()) {
            case DISABLED, OUTPUT -> stack;
            case INPUT -> getInput().insertItem(slot, stack, simulate);
            case BOTH -> insertCombined(slot, stack, simulate);
        };
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return switch (getMode()) {
            case DISABLED, INPUT -> ItemStack.EMPTY;
            case OUTPUT -> getOutput().extractItem(slot, amount, simulate);
            case BOTH -> extractCombined(slot, amount, simulate);
        };
    }

    @Override
    public int getSlotLimit(int slot) {
        return switch (getMode()) {
            case DISABLED -> 0;
            case INPUT -> getInput().getSlotLimit(slot);
            case OUTPUT -> getOutput().getSlotLimit(slot);
            case BOTH -> getCombinedSlotLimit(slot);
        };
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (getMode()) {
            case DISABLED, OUTPUT -> false;
            case INPUT -> getInput().isItemValid(slot, stack);
            case BOTH -> isCombinedItemValid(slot, stack);
        };
    }

    private ItemStack getStackInCombinedSlot(int slot) {
        IItemHandler input = getInput();
        return slot < input.getSlots()
                ? input.getStackInSlot(slot)
                : getOutput().getStackInSlot(slot - input.getSlots());
    }

    private ItemStack insertCombined(int slot, ItemStack stack, boolean simulate) {
        IItemHandler input = getInput();
        return slot < input.getSlots() ? input.insertItem(slot, stack, simulate) : stack;
    }

    private ItemStack extractCombined(int slot, int amount, boolean simulate) {
        IItemHandler input = getInput();
        return slot < input.getSlots() ? ItemStack.EMPTY : getOutput().extractItem(slot - input.getSlots(), amount, simulate);
    }

    private int getCombinedSlotLimit(int slot) {
        IItemHandler input = getInput();
        return slot < input.getSlots()
                ? input.getSlotLimit(slot)
                : getOutput().getSlotLimit(slot - input.getSlots());
    }

    private boolean isCombinedItemValid(int slot, ItemStack stack) {
        IItemHandler input = getInput();
        return slot < input.getSlots() && input.isItemValid(slot, stack);
    }

    private SideAccessMode getMode() {
        return modeSupplier.get();
    }

    private IItemHandler getInput() {
        return inputSupplier.get();
    }

    private IItemHandler getOutput() {
        return outputSupplier.get();
    }
}
