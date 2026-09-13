package de.artemis.matterworks.common.io;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Function;
import java.util.function.Supplier;

public final class ConfiguredItemHandler implements IItemHandler {
    private final Supplier<SideAccessMode> modeSupplier;
    private final Supplier<IItemHandler> inputSupplier;
    private final Function<SideAccessMode, IItemHandler> outputSupplier;

    public ConfiguredItemHandler(Supplier<SideAccessMode> modeSupplier, Supplier<IItemHandler> inputSupplier, Supplier<IItemHandler> outputSupplier) {
        this(modeSupplier, inputSupplier, ignored -> outputSupplier.get());
    }

    public ConfiguredItemHandler(Supplier<SideAccessMode> modeSupplier, Supplier<IItemHandler> inputSupplier, Function<SideAccessMode, IItemHandler> outputSupplier) {
        this.modeSupplier = modeSupplier;
        this.inputSupplier = inputSupplier;
        this.outputSupplier = outputSupplier;
    }

    @Override
    public int getSlots() {
        return switch (getMode()) {
            case DISABLED -> 0;
            case INPUT -> getInput().getSlots();
            case OUTPUT, OUTPUT_PRIMARY, OUTPUT_SECONDARY, OUTPUT_TERTIARY -> getOutput(getMode()).getSlots();
            case BOTH -> getInput().getSlots() + getOutput().getSlots();
        };
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return switch (getMode()) {
            case DISABLED -> ItemStack.EMPTY;
            case INPUT -> getInput().getStackInSlot(slot);
            case OUTPUT, OUTPUT_PRIMARY, OUTPUT_SECONDARY, OUTPUT_TERTIARY -> getOutput(getMode()).getStackInSlot(slot);
            case BOTH -> getStackInCombinedSlot(slot);
        };
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return switch (getMode()) {
            case DISABLED, OUTPUT, OUTPUT_PRIMARY, OUTPUT_SECONDARY, OUTPUT_TERTIARY -> stack;
            case INPUT -> ItemHandlerRouting.isSlotInRange(getInput(), slot)
                    ? ItemHandlerRouting.insertIntoAnySlot(getInput(), slot, stack, simulate)
                    : stack;
            case BOTH -> insertCombined(slot, stack, simulate);
        };
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return switch (getMode()) {
            case DISABLED, INPUT -> ItemStack.EMPTY;
            case OUTPUT, OUTPUT_PRIMARY, OUTPUT_SECONDARY, OUTPUT_TERTIARY -> ItemHandlerRouting.isSlotInRange(getOutput(getMode()), slot)
                    ? ItemHandlerRouting.extractFromAnySlot(getOutput(getMode()), slot, amount, simulate)
                    : ItemStack.EMPTY;
            case BOTH -> extractCombined(slot, amount, simulate);
        };
    }

    @Override
    public int getSlotLimit(int slot) {
        return switch (getMode()) {
            case DISABLED -> 0;
            case INPUT -> getInput().getSlotLimit(slot);
            case OUTPUT, OUTPUT_PRIMARY, OUTPUT_SECONDARY, OUTPUT_TERTIARY -> getOutput(getMode()).getSlotLimit(slot);
            case BOTH -> getCombinedSlotLimit(slot);
        };
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (getMode()) {
            case DISABLED, OUTPUT, OUTPUT_PRIMARY, OUTPUT_SECONDARY, OUTPUT_TERTIARY -> false;
            case INPUT -> ItemHandlerRouting.isSlotInRange(getInput(), slot)
                    && ItemHandlerRouting.canInsertIntoAnySlot(getInput(), stack);
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
        int combinedSlots = input.getSlots() + getOutput().getSlots();
        return slot >= 0 && slot < combinedSlots
                ? ItemHandlerRouting.insertIntoAnySlot(input, slot, stack, simulate)
                : stack;
    }

    private ItemStack extractCombined(int slot, int amount, boolean simulate) {
        IItemHandler input = getInput();
        int combinedSlots = input.getSlots() + getOutput().getSlots();
        return slot >= 0 && slot < combinedSlots
                ? ItemHandlerRouting.extractFromAnySlot(getOutput(), slot - input.getSlots(), amount, simulate)
                : ItemStack.EMPTY;
    }

    private int getCombinedSlotLimit(int slot) {
        IItemHandler input = getInput();
        return slot < input.getSlots()
                ? input.getSlotLimit(slot)
                : getOutput().getSlotLimit(slot - input.getSlots());
    }

    private boolean isCombinedItemValid(int slot, ItemStack stack) {
        IItemHandler input = getInput();
        return slot >= 0
                && slot < input.getSlots() + getOutput().getSlots()
                && ItemHandlerRouting.canInsertIntoAnySlot(input, stack);
    }

    private SideAccessMode getMode() {
        return modeSupplier.get();
    }

    private IItemHandler getInput() {
        return inputSupplier.get();
    }

    private IItemHandler getOutput() {
        return getOutput(SideAccessMode.OUTPUT);
    }

    private IItemHandler getOutput(SideAccessMode mode) {
        return outputSupplier.apply(mode);
    }
}
