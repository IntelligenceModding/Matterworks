package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.matter.MatterValueManager;
import de.artemis.matterworks.common.menu.MatterAnalyzerMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.template.EncodedTemplateData;
import de.artemis.matterworks.common.template.TemplateAnalysisManager;
import de.artemis.matterworks.common.upgrade.PowerCrystalData;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class MatterAnalyzerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ENERGY_ITEM_INPUT_SLOT = 0;
    public static final int ENERGY_ITEM_OUTPUT_SLOT = 1;
    public static final int TEMPLATE_SLOT = 2;
    public static final int ITEM_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;
    public static final int REJECTED_OUTPUT_SLOT = 5;
    public static final int CRYSTAL_SLOT = 6;
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_TEMPLATE_PROGRESS = 2;
    public static final int DATA_TEMPLATE_REQUIRED = 3;
    public static final int DATA_ENERGY = 4;
    public static final int DATA_ENERGY_CAPACITY = 5;
    public static final int DATA_PROGRESS_COLOR = 6;
    public static final int DATA_COUNT = 7;

    private final ItemStackHandler itemHandler = new ItemStackHandler(7) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot == CRYSTAL_SLOT) {
                clampEnergyToCurrentCapacity();
            }
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return MatterAnalyzerBlockEntity.this.isItemValid(slot, stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return switch (slot) {
                case ENERGY_ITEM_INPUT_SLOT, ENERGY_ITEM_OUTPUT_SLOT, TEMPLATE_SLOT, CRYSTAL_SLOT -> 1;
                case ITEM_SLOT, OUTPUT_SLOT, REJECTED_OUTPUT_SLOT -> 64;
                default -> 64;
            };
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX_PROGRESS -> getCurrentProcessTime();
                case DATA_TEMPLATE_PROGRESS -> EncodedTemplateData.getAnalysisProgress(itemHandler.getStackInSlot(TEMPLATE_SLOT));
                case DATA_TEMPLATE_REQUIRED -> EncodedTemplateData.hasEncodedItem(itemHandler.getStackInSlot(TEMPLATE_SLOT))
                        ? EncodedTemplateData.getRequiredCount(itemHandler.getStackInSlot(TEMPLATE_SLOT))
                        : 0;
                case DATA_ENERGY -> energyStored;
                case DATA_ENERGY_CAPACITY -> getCurrentEnergyCapacity();
                case DATA_PROGRESS_COLOR -> getEffectiveProgressBarColor();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == DATA_PROGRESS) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private final IItemHandler inputAutomationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 4;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return switch (slot) {
                case 0 -> itemHandler.getStackInSlot(ENERGY_ITEM_INPUT_SLOT);
                case 1 -> itemHandler.getStackInSlot(TEMPLATE_SLOT);
                case 2 -> itemHandler.getStackInSlot(ITEM_SLOT);
                case 3 -> itemHandler.getStackInSlot(CRYSTAL_SLOT);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return switch (slot) {
                case 0 -> itemHandler.insertItem(ENERGY_ITEM_INPUT_SLOT, stack, simulate);
                case 1 -> itemHandler.insertItem(TEMPLATE_SLOT, stack, simulate);
                case 2 -> itemHandler.insertItem(ITEM_SLOT, stack, simulate);
                case 3 -> itemHandler.insertItem(CRYSTAL_SLOT, stack, simulate);
                default -> stack;
            };
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return switch (slot) {
                case 0, 1, 3 -> 1;
                case 2 -> 64;
                default -> 0;
            };
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> itemHandler.isItemValid(ENERGY_ITEM_INPUT_SLOT, stack);
                case 1 -> itemHandler.isItemValid(TEMPLATE_SLOT, stack);
                case 2 -> itemHandler.isItemValid(ITEM_SLOT, stack);
                case 3 -> itemHandler.isItemValid(CRYSTAL_SLOT, stack);
                default -> false;
            };
        }
    };

    private final IItemHandler outputAutomationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 3;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return switch (slot) {
                case 0 -> itemHandler.getStackInSlot(ENERGY_ITEM_OUTPUT_SLOT);
                case 1 -> itemHandler.getStackInSlot(OUTPUT_SLOT);
                case 2 -> itemHandler.getStackInSlot(REJECTED_OUTPUT_SLOT);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return switch (slot) {
                case 0 -> itemHandler.extractItem(ENERGY_ITEM_OUTPUT_SLOT, amount, simulate);
                case 1 -> itemHandler.extractItem(OUTPUT_SLOT, amount, simulate);
                case 2 -> itemHandler.extractItem(REJECTED_OUTPUT_SLOT, amount, simulate);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return switch (slot) {
                case 0 -> itemHandler.getSlotLimit(ENERGY_ITEM_OUTPUT_SLOT);
                case 1 -> itemHandler.getSlotLimit(OUTPUT_SLOT);
                case 2 -> itemHandler.getSlotLimit(REJECTED_OUTPUT_SLOT);
                default -> 0;
            };
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    private final IEnergyStorage externalEnergyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) {
                return 0;
            }

            int received = Math.min(maxReceive, getCurrentEnergyCapacity() - energyStored);
            if (received > 0 && !simulate) {
                energyStored += received;
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energyStored;
        }

        @Override
        public int getMaxEnergyStored() {
            return getCurrentEnergyCapacity();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    private int progress;
    private int energyStored;
    private ItemStack activeProcessCrystal = ItemStack.EMPTY;
    private boolean processCrystalLatched;

    public MatterAnalyzerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MATTER_ANALYZER.get(), pos, blockState);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, MatterAnalyzerBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getData() {
        return data;
    }

    public @Nullable IItemHandler getAutomationHandler(@Nullable Direction side) {
        if (side == null) {
            return itemHandler;
        }
        return side == Direction.DOWN ? outputAutomationHandler : inputAutomationHandler;
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return externalEnergyStorage;
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        transferEnergyFromInputItem();
        moveCompletedTemplateToOutput();

        if (moveRejectedInputToOutput()) {
            if (progress != 0) {
                progress = 0;
                clearLatchedProcessCrystal();
            }
            setChanged();
            return;
        }

        if (canProcess() && hasEnoughEnergy()) {
            if (progress == 0) {
                latchProcessCrystal();
            }
            energyStored -= getCurrentEnergyPerTick();
            progress++;
            if (progress >= getCurrentProcessTime()) {
                progress = 0;
                processItem();
                clearLatchedProcessCrystal();
                setChanged();
            }
        } else if (progress != 0) {
            progress = 0;
            clearLatchedProcessCrystal();
            setChanged();
        }

        moveCompletedTemplateToOutput();
    }

    public SimpleContainer createDropInventory() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i).copy());
        }
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModBlocks.MATTER_ANALYZER.get().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MatterAnalyzerMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("energy", energyStored);
        tag.putInt("progress", progress);
        if (processCrystalLatched) {
            tag.putBoolean("process_crystal_latched", true);
        }
        if (!activeProcessCrystal.isEmpty()) {
            tag.put("active_process_crystal", activeProcessCrystal.saveOptional(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("energy")) {
            energyStored = tag.getInt("energy");
            clampEnergyToCurrentCapacity();
        }
        progress = tag.getInt("progress");
        processCrystalLatched = tag.getBoolean("process_crystal_latched");
        if (tag.contains("active_process_crystal")) {
            activeProcessCrystal = ItemStack.parseOptional(registries, tag.getCompound("active_process_crystal"));
        } else {
            activeProcessCrystal = ItemStack.EMPTY;
        }
    }

    private boolean isItemValid(int slot, ItemStack stack) {
        if (slot == ENERGY_ITEM_INPUT_SLOT) {
            return EnergyItemHelper.canProvideEnergy(stack);
        }

        if (slot == ENERGY_ITEM_OUTPUT_SLOT || slot == OUTPUT_SLOT || slot == REJECTED_OUTPUT_SLOT) {
            return false;
        }

        if (slot == TEMPLATE_SLOT) {
            return stack.is(ModItems.EMPTY_TEMPLATE.get())
                    || (stack.is(ModItems.ENCODED_TEMPLATE.get()) && EncodedTemplateData.hasEncodedItem(stack));
        }

        if (slot == ITEM_SLOT) {
            return canAcceptAnalyzerInput(stack);
        }

        if (slot == CRYSTAL_SLOT) {
            return PowerCrystalEffects.isPowerCrystal(stack);
        }

        return false;
    }

    private boolean canProcess() {
        ItemStack templateStack = itemHandler.getStackInSlot(TEMPLATE_SLOT);
        ItemStack itemStack = itemHandler.getStackInSlot(ITEM_SLOT);
        if (!canEncode(itemStack) || !isTemplateReadyFor(itemStack, templateStack)) {
            return false;
        }
        return !EncodedTemplateData.isComplete(templateStack);
    }

    private void processItem() {
        ItemStack templateStack = itemHandler.getStackInSlot(TEMPLATE_SLOT);
        ItemStack itemStack = itemHandler.getStackInSlot(ITEM_SLOT);
        ItemStack crystalStack = getEffectiveCrystalStack();
        if (!canEncode(itemStack) || !isTemplateReadyFor(itemStack, templateStack)) {
            return;
        }

        if (templateStack.is(ModItems.EMPTY_TEMPLATE.get())) {
            int requiredCount = TemplateAnalysisManager.getRequiredItemCount(itemStack, level);
            templateStack = EncodedTemplateData.createEncodedTemplate(itemStack.getItem(), requiredCount);
            itemHandler.setStackInSlot(TEMPLATE_SLOT, templateStack);
        }

        EncodedTemplateData.addAnalysisProgress(templateStack, PowerCrystalEffects.getAnalyzerProgressPerProcess(crystalStack));
        itemHandler.getStackInSlot(ITEM_SLOT).shrink(1);
        moveCompletedTemplateToOutput();
    }

    private boolean isTemplateReadyFor(ItemStack itemStack, ItemStack templateStack) {
        if (templateStack.is(ModItems.EMPTY_TEMPLATE.get())) {
            return true;
        }

        if (!templateStack.is(ModItems.ENCODED_TEMPLATE.get()) || !EncodedTemplateData.hasEncodedItem(templateStack)) {
            return false;
        }

        return EncodedTemplateData.getEncodedItem(templateStack) == itemStack.getItem();
    }

    private void moveCompletedTemplateToOutput() {
        ItemStack templateStack = itemHandler.getStackInSlot(TEMPLATE_SLOT);
        if (!templateStack.is(ModItems.ENCODED_TEMPLATE.get()) || !EncodedTemplateData.isComplete(templateStack)) {
            return;
        }

        ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, templateStack.copy());
            itemHandler.setStackInSlot(TEMPLATE_SLOT, ItemStack.EMPTY);
            return;
        }

        if (ItemStack.isSameItemSameComponents(outputStack, templateStack) && outputStack.getCount() < outputStack.getMaxStackSize()) {
            outputStack.grow(1);
            itemHandler.setStackInSlot(TEMPLATE_SLOT, ItemStack.EMPTY);
        }
    }

    private boolean moveRejectedInputToOutput() {
        ItemStack inputStack = itemHandler.getStackInSlot(ITEM_SLOT);
        if (inputStack.isEmpty() || canEncode(inputStack)) {
            return false;
        }

        ItemStack rejectedOutputStack = itemHandler.getStackInSlot(REJECTED_OUTPUT_SLOT);
        if (!rejectedOutputStack.isEmpty()
                && (!ItemStack.isSameItemSameComponents(rejectedOutputStack, inputStack)
                || rejectedOutputStack.getCount() >= rejectedOutputStack.getMaxStackSize())) {
            return false;
        }

        ItemStack movedStack = inputStack.copy();
        itemHandler.setStackInSlot(ITEM_SLOT, ItemStack.EMPTY);
        if (rejectedOutputStack.isEmpty()) {
            itemHandler.setStackInSlot(REJECTED_OUTPUT_SLOT, movedStack);
        } else {
            rejectedOutputStack.grow(movedStack.getCount());
        }
        return true;
    }

    private boolean canAcceptAnalyzerInput(ItemStack stack) {
        return !stack.isEmpty()
                && !stack.is(ModItems.EMPTY_TEMPLATE.get())
                && !stack.is(ModItems.ENCODED_TEMPLATE.get())
                && !PowerCrystalEffects.isPowerCrystal(stack)
                && !EnergyItemHelper.canProvideEnergy(stack);
    }

    private boolean canEncode(ItemStack stack) {
        return !stack.isEmpty() && !MatterValueManager.isPatternEncodingBlocked(stack);
    }

    private int getCurrentProcessTime() {
        return PowerCrystalEffects.getAnalyzerProcessTime(getEffectiveCrystalStack());
    }

    public int getEffectiveProgressBarColor() {
        ItemStack crystalStack = getEffectiveCrystalStack();
        return crystalStack.isEmpty() ? 0x57C7FF : PowerCrystalEffects.getBarColor(crystalStack);
    }

    private boolean hasEnoughEnergy() {
        return energyStored >= getCurrentEnergyPerTick();
    }

    private int getCurrentEnergyPerTick() {
        return PowerCrystalEffects.getAnalyzerEnergyPerTick(getEffectiveCrystalStack());
    }

    private int getCurrentEnergyCapacity() {
        return PowerCrystalEffects.getAnalyzerEnergyCapacity(getEffectiveCrystalStack());
    }

    private void clampEnergyToCurrentCapacity() {
        energyStored = Math.min(energyStored, getCurrentEnergyCapacity());
    }

    private void drainCrystalCharge() {
    }

    private void transferEnergyFromInputItem() {
        ItemStack energyStack = itemHandler.getStackInSlot(ENERGY_ITEM_INPUT_SLOT);
        if (energyStack.isEmpty()) {
            return;
        }

        IEnergyStorage itemEnergy = EnergyItemHelper.getEnergyStorage(energyStack);
        if (itemEnergy == null) {
            return;
        }

        int missingEnergy = getCurrentEnergyCapacity() - energyStored;
        if (missingEnergy > 0) {
            int moved = EnergyItemHelper.transferEnergy(itemEnergy, externalEnergyStorage, missingEnergy);
            if (moved > 0) {
                setChanged();
            }
        }

        if (EnergyItemHelper.isDepleted(energyStack)) {
            moveEnergyItemToOutput();
        }
    }

    private void moveEnergyItemToOutput() {
        ItemStack inputStack = itemHandler.getStackInSlot(ENERGY_ITEM_INPUT_SLOT);
        if (inputStack.isEmpty()) {
            return;
        }

        ItemStack outputStack = itemHandler.getStackInSlot(ENERGY_ITEM_OUTPUT_SLOT);
        if (!outputStack.isEmpty() && (!ItemStack.isSameItemSameComponents(outputStack, inputStack) || outputStack.getCount() >= outputStack.getMaxStackSize())) {
            return;
        }

        ItemStack movedStack = inputStack.copy();
        itemHandler.setStackInSlot(ENERGY_ITEM_INPUT_SLOT, ItemStack.EMPTY);
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(ENERGY_ITEM_OUTPUT_SLOT, movedStack);
        } else {
            outputStack.grow(movedStack.getCount());
        }
        setChanged();
    }

    private ItemStack getCrystalStack() {
        return itemHandler.getSlots() > CRYSTAL_SLOT ? itemHandler.getStackInSlot(CRYSTAL_SLOT) : ItemStack.EMPTY;
    }

    private ItemStack getEffectiveCrystalStack() {
        return progress > 0 && processCrystalLatched ? activeProcessCrystal : getCrystalStack();
    }

    private void latchProcessCrystal() {
        processCrystalLatched = true;
        ItemStack crystalStack = getCrystalStack();
        if (PowerCrystalEffects.isActive(crystalStack)) {
            activeProcessCrystal = crystalStack.copy();
            PowerCrystalData.drainCharge(crystalStack, PowerCrystalEffects.CRYSTAL_CHARGE_COST);
        } else {
            activeProcessCrystal = ItemStack.EMPTY;
        }
    }

    private void clearLatchedProcessCrystal() {
        activeProcessCrystal = ItemStack.EMPTY;
        processCrystalLatched = false;
    }
}
