package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.energy.EnergyItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import de.artemis.matterworks.common.upgrade.PowerCrystalData;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;

public abstract class AbstractMatterMachineBlockEntity extends BlockEntity {
    public static final int ENERGY_ITEM_INPUT_SLOT = 0;
    public static final int ENERGY_ITEM_OUTPUT_SLOT = 1;
    public static final int INPUT_SLOT = 2;
    public static final int INVALID_OUTPUT_SLOT = 3;
    public static final int FLUID_BUCKET_INPUT_SLOT = 4;
    public static final int FLUID_BUCKET_OUTPUT_SLOT = 5;
    public static final int CRYSTAL_SLOT = 6;
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_FLUID_AMOUNT = 2;
    public static final int DATA_FLUID_CAPACITY = 3;
    public static final int DATA_ENERGY = 4;
    public static final int DATA_ENERGY_CAPACITY = 5;
    public static final int DATA_PROGRESS_COLOR = 6;
    public static final int DATA_COUNT = 7;

    protected final ItemStackHandler itemHandler;

    protected final FluidTank fluidTank;
    protected final EnergyStorage energyStorage;
    private final int baseFluidTankCapacity;
    private final IEnergyStorage externalEnergyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return energyStorage.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energyStorage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyStorage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return energyStorage.canReceive();
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX_PROGRESS -> getMaxProgress();
                case DATA_FLUID_AMOUNT -> fluidTank.getFluidAmount();
                case DATA_FLUID_CAPACITY -> fluidTank.getCapacity();
                case DATA_ENERGY -> energyStorage.getEnergyStored();
                case DATA_ENERGY_CAPACITY -> energyStorage.getMaxEnergyStored();
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
                case 1 -> itemHandler.getStackInSlot(INPUT_SLOT);
                case 2 -> itemHandler.getStackInSlot(FLUID_BUCKET_INPUT_SLOT);
                case 3 -> itemHandler.getStackInSlot(CRYSTAL_SLOT);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return switch (slot) {
                case 0 -> itemHandler.insertItem(ENERGY_ITEM_INPUT_SLOT, stack, simulate);
                case 1 -> itemHandler.insertItem(INPUT_SLOT, stack, simulate);
                case 2 -> itemHandler.insertItem(FLUID_BUCKET_INPUT_SLOT, stack, simulate);
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
                case 0 -> itemHandler.getSlotLimit(ENERGY_ITEM_INPUT_SLOT);
                case 1 -> itemHandler.getSlotLimit(INPUT_SLOT);
                case 2 -> itemHandler.getSlotLimit(FLUID_BUCKET_INPUT_SLOT);
                case 3 -> itemHandler.getSlotLimit(CRYSTAL_SLOT);
                default -> 0;
            };
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> itemHandler.isItemValid(ENERGY_ITEM_INPUT_SLOT, stack);
                case 1 -> itemHandler.isItemValid(INPUT_SLOT, stack);
                case 2 -> itemHandler.isItemValid(FLUID_BUCKET_INPUT_SLOT, stack);
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
                case 1 -> itemHandler.getStackInSlot(INVALID_OUTPUT_SLOT);
                case 2 -> itemHandler.getStackInSlot(FLUID_BUCKET_OUTPUT_SLOT);
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
                case 1 -> itemHandler.extractItem(INVALID_OUTPUT_SLOT, amount, simulate);
                case 2 -> itemHandler.extractItem(FLUID_BUCKET_OUTPUT_SLOT, amount, simulate);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return switch (slot) {
                case 0 -> itemHandler.getSlotLimit(ENERGY_ITEM_OUTPUT_SLOT);
                case 1 -> itemHandler.getSlotLimit(INVALID_OUTPUT_SLOT);
                case 2 -> itemHandler.getSlotLimit(FLUID_BUCKET_OUTPUT_SLOT);
                default -> 0;
            };
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
    protected int progress;
    protected ItemStack activeProcessCrystal = ItemStack.EMPTY;
    protected boolean processCrystalLatched;

    protected AbstractMatterMachineBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState blockState,
            int tankCapacity,
            Predicate<FluidStack> fluidValidator,
            int energyCapacity
    ) {
        this(type, pos, blockState, tankCapacity, fluidValidator, energyCapacity, 7);
    }

    protected AbstractMatterMachineBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState blockState,
            int tankCapacity,
            Predicate<FluidStack> fluidValidator,
            int energyCapacity,
            int slotCount
    ) {
        super(type, pos, blockState);
        this.baseFluidTankCapacity = tankCapacity;
        this.itemHandler = new ItemStackHandler(slotCount) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return AbstractMatterMachineBlockEntity.this.isItemValid(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return AbstractMatterMachineBlockEntity.this.getSlotLimit(slot);
            }
        };
        this.fluidTank = new FluidTank(tankCapacity, fluidValidator) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        this.energyStorage = new EnergyStorage(energyCapacity, energyCapacity, energyCapacity) {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                int received = super.receiveEnergy(maxReceive, simulate);
                if (received > 0 && !simulate) {
                    setChanged();
                }
                return received;
            }
        };
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getData() {
        return data;
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public ItemStack getDisplayedCrystalStack() {
        return getEffectiveCrystalStack();
    }

    public int getEffectiveProgressBarColor() {
        ItemStack crystalStack = getEffectiveCrystalStack();
        return crystalStack.isEmpty() ? 0xB67CFF : PowerCrystalEffects.getBarColor(crystalStack);
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return externalEnergyStorage;
    }

    public @Nullable IItemHandler getAutomationHandler(@Nullable Direction side) {
        if (side == null) {
            return itemHandler;
        }
        return side == Direction.DOWN ? outputAutomationHandler : inputAutomationHandler;
    }

    public @Nullable IFluidHandler getFluidAutomationHandler(@Nullable Direction side) {
        return fluidTank;
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        beforeProcessingTick();

        if (canProcess() && hasEnoughEnergy()) {
            if (progress == 0) {
                latchProcessCrystal();
            }
            energyStorage.extractEnergy(getEnergyPerTick(), false);
            progress++;
            if (progress >= getMaxProgress()) {
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

        afterProcessingTick();
    }

    public SimpleContainer createDropInventory() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i).copy());
        }
        return inventory;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.put("tank", fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("energy", energyStorage.getEnergyStored());
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
            migrateLegacyInventory();
        }
        if (tag.contains("tank")) {
            fluidTank.readFromNBT(registries, tag.getCompound("tank"));
        }
        if (tag.contains("energy")) {
            energyStorage.receiveEnergy(tag.getInt("energy"), false);
        }
        progress = tag.getInt("progress");
        processCrystalLatched = tag.getBoolean("process_crystal_latched");
        if (tag.contains("active_process_crystal")) {
            activeProcessCrystal = ItemStack.parseOptional(registries, tag.getCompound("active_process_crystal"));
        } else {
            activeProcessCrystal = ItemStack.EMPTY;
        }
        syncFluidTankCapacities();
    }

    private void migrateLegacyInventory() {
        if (itemHandler.getSlots() >= 7) {
            return;
        }

        ItemStack oldInput = itemHandler.getSlots() > 0 ? itemHandler.getStackInSlot(0).copy() : ItemStack.EMPTY;
        ItemStack oldBucketInput = itemHandler.getSlots() > 1 ? itemHandler.getStackInSlot(1).copy() : ItemStack.EMPTY;
        ItemStack oldBucketOutput = itemHandler.getSlots() > 2 ? itemHandler.getStackInSlot(2).copy() : ItemStack.EMPTY;
        ItemStack oldCrystal = itemHandler.getSlots() > 3 ? itemHandler.getStackInSlot(3).copy() : ItemStack.EMPTY;

        itemHandler.setSize(7);
        itemHandler.setStackInSlot(INPUT_SLOT, oldInput);
        itemHandler.setStackInSlot(FLUID_BUCKET_INPUT_SLOT, oldBucketInput);
        itemHandler.setStackInSlot(FLUID_BUCKET_OUTPUT_SLOT, oldBucketOutput);
        itemHandler.setStackInSlot(CRYSTAL_SLOT, oldCrystal);
    }

    protected void beforeProcessingTick() {
        syncFluidTankCapacities();
        transferEnergyFromInputItem();
    }

    protected void afterProcessingTick() {
    }

    protected boolean hasEnoughEnergy() {
        return energyStorage.getEnergyStored() >= getEnergyPerTick();
    }

    protected void drainCrystalCharge() {
    }

    protected ItemStack getEffectiveCrystalStack() {
        return progress > 0 && processCrystalLatched ? activeProcessCrystal : itemHandler.getStackInSlot(CRYSTAL_SLOT);
    }

    protected void latchProcessCrystal() {
        processCrystalLatched = true;
        ItemStack crystalStack = itemHandler.getStackInSlot(CRYSTAL_SLOT);
        if (PowerCrystalEffects.isActive(crystalStack)) {
            activeProcessCrystal = crystalStack.copy();
            PowerCrystalData.drainCharge(crystalStack, PowerCrystalEffects.CRYSTAL_CHARGE_COST);
        } else {
            activeProcessCrystal = ItemStack.EMPTY;
        }
    }

    protected void clearLatchedProcessCrystal() {
        activeProcessCrystal = ItemStack.EMPTY;
        processCrystalLatched = false;
    }

    protected void syncFluidTankCapacities() {
        syncTankCapacity(fluidTank, getModifiedFluidTankCapacity(baseFluidTankCapacity));
    }

    protected int getModifiedFluidTankCapacity(int baseCapacity) {
        return PowerCrystalEffects.getModifiedFluidCapacity(baseCapacity, getEffectiveCrystalStack());
    }

    protected void syncTankCapacity(FluidTank tank, int capacity) {
        int clampedCapacity = Math.max(0, capacity);
        if (tank.getCapacity() != clampedCapacity) {
            tank.setCapacity(clampedCapacity);
            if (tank.getFluidAmount() > clampedCapacity) {
                tank.setFluid(tank.getFluid().copyWithAmount(clampedCapacity));
            }
            setChanged();
        }
    }

    protected int getSlotLimit(int slot) {
        if (slot == ENERGY_ITEM_INPUT_SLOT || slot == ENERGY_ITEM_OUTPUT_SLOT || slot == CRYSTAL_SLOT) {
            return 1;
        }
        return 64;
    }

    protected boolean isEnergyItem(ItemStack stack) {
        return EnergyItemHelper.canProvideEnergy(stack);
    }

    protected void transferEnergyFromInputItem() {
        ItemStack energyStack = itemHandler.getStackInSlot(ENERGY_ITEM_INPUT_SLOT);
        if (energyStack.isEmpty()) {
            return;
        }

        IEnergyStorage itemEnergy = EnergyItemHelper.getEnergyStorage(energyStack);
        if (itemEnergy == null) {
            return;
        }

        int missingEnergy = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        if (missingEnergy > 0) {
            int moved = EnergyItemHelper.transferEnergy(itemEnergy, energyStorage, missingEnergy);
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

    protected abstract Component getDefaultName();

    protected abstract boolean isItemValid(int slot, ItemStack stack);

    protected abstract boolean canProcess();

    protected abstract void processItem();

    protected abstract int getMaxProgress();

    protected abstract int getEnergyPerTick();
}
