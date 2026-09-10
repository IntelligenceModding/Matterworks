package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.io.MappedItemHandler;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.menu.GraviticCondenserMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModFluids;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.upgrade.PowerCrystalData;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class GraviticCondenserBlockEntity extends AbstractMatterMachineBlockEntity implements MenuProvider {
    public static final int SLOT_POWER_INPUT = ENERGY_ITEM_INPUT_SLOT;
    public static final int OUTPUT_SLOT = 2;
    public static final int RAW_BUCKET_INPUT_SLOT = 3;
    public static final int RAW_BUCKET_OUTPUT_SLOT = 4;
    public static final int SLOT_CRYSTAL = 5;
    public static final int SLUDGE_BUCKET_INPUT_SLOT = 6;
    public static final int SLUDGE_BUCKET_OUTPUT_SLOT = 7;

    public static final int DATA_UNSTABLE_AMOUNT = DATA_COUNT;
    public static final int DATA_UNSTABLE_CAPACITY = DATA_COUNT + 1;
    public static final int CONDENSER_DATA_COUNT = DATA_COUNT + 2;

    public static final int PROCESS_TIME = 200;
    public static final int ENERGY_CAPACITY = 24000;
    public static final int ENERGY_PER_TICK = 96;
    public static final int RAW_MATTER_TANK_CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    public static final int UNSTABLE_MATTER_TANK_CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    public static final int RAW_MATTER_COST = 8000;
    public static final int UNSTABLE_MATTER_OUTPUT = 100;
    private static final int SLOT_COUNT = 8;

    private final FluidTank unstableTank = new FluidTank(UNSTABLE_MATTER_TANK_CAPACITY, ModFluids::isUnstableMatter) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final ContainerData condenserData = new ContainerData() {
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
                case DATA_UNSTABLE_AMOUNT -> unstableTank.getFluidAmount();
                case DATA_UNSTABLE_CAPACITY -> unstableTank.getCapacity();
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
            return CONDENSER_DATA_COUNT;
        }
    };

    private final IItemHandler automationInputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 4;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return switch (slot) {
                case 0 -> itemHandler.getStackInSlot(SLOT_POWER_INPUT);
                case 1 -> itemHandler.getStackInSlot(RAW_BUCKET_INPUT_SLOT);
                case 2 -> itemHandler.getStackInSlot(SLUDGE_BUCKET_INPUT_SLOT);
                case 3 -> itemHandler.getStackInSlot(SLOT_CRYSTAL);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return switch (slot) {
                case 0 -> itemHandler.insertItem(SLOT_POWER_INPUT, stack, simulate);
                case 1 -> itemHandler.insertItem(RAW_BUCKET_INPUT_SLOT, stack, simulate);
                case 2 -> itemHandler.insertItem(SLUDGE_BUCKET_INPUT_SLOT, stack, simulate);
                case 3 -> itemHandler.insertItem(SLOT_CRYSTAL, stack, simulate);
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
                case 0 -> 1;
                case 1 -> itemHandler.getSlotLimit(RAW_BUCKET_INPUT_SLOT);
                case 2 -> itemHandler.getSlotLimit(SLUDGE_BUCKET_INPUT_SLOT);
                case 3 -> 1;
                default -> 0;
            };
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> itemHandler.isItemValid(SLOT_POWER_INPUT, stack);
                case 1 -> itemHandler.isItemValid(RAW_BUCKET_INPUT_SLOT, stack);
                case 2 -> itemHandler.isItemValid(SLUDGE_BUCKET_INPUT_SLOT, stack);
                case 3 -> itemHandler.isItemValid(SLOT_CRYSTAL, stack);
                default -> false;
            };
        }
    };

    private final IItemHandler automationOutputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 3;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return switch (slot) {
                case 0 -> itemHandler.getStackInSlot(OUTPUT_SLOT);
                case 1 -> itemHandler.getStackInSlot(RAW_BUCKET_OUTPUT_SLOT);
                case 2 -> itemHandler.getStackInSlot(SLUDGE_BUCKET_OUTPUT_SLOT);
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
                case 0 -> itemHandler.extractItem(OUTPUT_SLOT, amount, simulate);
                case 1 -> itemHandler.extractItem(RAW_BUCKET_OUTPUT_SLOT, amount, simulate);
                case 2 -> itemHandler.extractItem(SLUDGE_BUCKET_OUTPUT_SLOT, amount, simulate);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return switch (slot) {
                case 0 -> itemHandler.getSlotLimit(OUTPUT_SLOT);
                case 1 -> itemHandler.getSlotLimit(RAW_BUCKET_OUTPUT_SLOT);
                case 2 -> itemHandler.getSlotLimit(SLUDGE_BUCKET_OUTPUT_SLOT);
                default -> 0;
            };
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
    private final IItemHandler singularityOutputHandler = new MappedItemHandler(itemHandler, false, true, OUTPUT_SLOT);
    private final IItemHandler emptyBucketOutputHandler = new MappedItemHandler(itemHandler, false, true, RAW_BUCKET_OUTPUT_SLOT);
    private final IItemHandler unstableBucketOutputHandler = new MappedItemHandler(itemHandler, false, true, SLUDGE_BUCKET_OUTPUT_SLOT);

    private final IFluidHandler fluidAutomationHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> fluidTank.getFluidInTank(0);
                case 1 -> unstableTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> fluidTank.getTankCapacity(0);
                case 1 -> unstableTank.getTankCapacity(0);
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && fluidTank.isFluidValid(0, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return fluidTank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return unstableTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return unstableTank.drain(maxDrain, action);
        }
    };

    public GraviticCondenserBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.GRAVITIC_CONDENSER.get(),
                pos,
                blockState,
                RAW_MATTER_TANK_CAPACITY,
                ModFluids::isRawMatter,
                ENERGY_CAPACITY,
                SLOT_COUNT
        );
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, GraviticCondenserBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public FluidStack getUnstableFluidStack() {
        return unstableTank.getFluid().copy();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GraviticCondenserMenu(containerId, playerInventory, this, condenserData);
    }

    @Override
    protected IItemHandler getInputAutomationHandler() {
        return automationInputHandler;
    }

    @Override
    protected IItemHandler getOutputAutomationHandler() {
        return automationOutputHandler;
    }

    @Override
    protected IItemHandler getOutputAutomationHandler(SideAccessMode mode) {
        return switch (mode) {
            case OUTPUT_PRIMARY -> singularityOutputHandler;
            case OUTPUT_SECONDARY -> emptyBucketOutputHandler;
            case OUTPUT_TERTIARY -> unstableBucketOutputHandler;
            default -> automationOutputHandler;
        };
    }

    @Override
    protected IFluidHandler getBaseFluidAutomationHandler() {
        return fluidAutomationHandler;
    }

    @Override
    protected boolean supportsTargetedSideOutput(SideConfigType type, SideAccessMode mode) {
        return type == SideConfigType.ITEMS
                && (mode == SideAccessMode.OUTPUT_PRIMARY
                || mode == SideAccessMode.OUTPUT_SECONDARY
                || mode == SideAccessMode.OUTPUT_TERTIARY);
    }

    @Override
    public String getSideAccessModeLabel(SideConfigType type, net.minecraft.core.Direction side, SideAccessMode mode) {
        if (type == SideConfigType.ITEMS) {
            return switch (mode) {
                case OUTPUT_PRIMARY -> "Out (Matter Singularity)";
                case OUTPUT_SECONDARY -> "Out (Empty Bucket)";
                case OUTPUT_TERTIARY -> "Out (Unstable Bucket)";
                default -> super.getSideAccessModeLabel(type, side, mode);
            };
        }
        return super.getSideAccessModeLabel(type, side, mode);
    }

    @Override
    public String getSideAccessModeShortLabel(SideConfigType type, net.minecraft.core.Direction side, SideAccessMode mode) {
        if (type == SideConfigType.ITEMS) {
            return switch (mode) {
                case OUTPUT_PRIMARY -> "Sing";
                case OUTPUT_SECONDARY -> "Emp";
                case OUTPUT_TERTIARY -> "UnB";
                default -> super.getSideAccessModeShortLabel(type, side, mode);
            };
        }
        return super.getSideAccessModeShortLabel(type, side, mode);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("unstable_tank", unstableTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("unstable_tank")) {
            unstableTank.readFromNBT(registries, tag.getCompound("unstable_tank"));
        }
    }

    @Override
    protected void beforeProcessingTick() {
        syncFluidTankCapacities();
        transferEnergyFromPowerInputItem();
        importRawMatterBucket();
        exportSludgeBucket();
    }

    @Override
    protected void syncFluidTankCapacities() {
        super.syncFluidTankCapacities();
        syncTankCapacity(unstableTank, getModifiedFluidTankCapacity(UNSTABLE_MATTER_TANK_CAPACITY));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(ModBlocks.GRAVITIC_CONDENSER.get().getDescriptionId());
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        if (slot == SLOT_POWER_INPUT) {
            return isEnergyItem(stack);
        }
        if (slot == OUTPUT_SLOT || slot == RAW_BUCKET_OUTPUT_SLOT || slot == SLUDGE_BUCKET_OUTPUT_SLOT || slot == ENERGY_ITEM_OUTPUT_SLOT) {
            return false;
        }
        if (slot == RAW_BUCKET_INPUT_SLOT) {
            return stack.is(ModItems.RAW_MATTER_BUCKET.get());
        }
        if (slot == SLOT_CRYSTAL) {
            return PowerCrystalEffects.isPowerCrystal(stack);
        }
        if (slot == SLUDGE_BUCKET_INPUT_SLOT) {
            return stack.is(Items.BUCKET);
        }
        return false;
    }

    @Override
    protected boolean canProcess() {
        return fluidTank.getFluidAmount() >= RAW_MATTER_COST
                && level != null
                && unstableTank.getSpace() >= UNSTABLE_MATTER_OUTPUT
                && canOutputSingularity();
    }

    @Override
    protected void processItem() {
        fluidTank.drain(RAW_MATTER_COST, IFluidHandler.FluidAction.EXECUTE);
        unstableTank.fill(new FluidStack(ModFluids.UNSTABLE_MATTER.get(), UNSTABLE_MATTER_OUTPUT), IFluidHandler.FluidAction.EXECUTE);

        ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, ModItems.MATTER_SINGULARITY.get().getDefaultInstance());
        } else {
            outputStack.grow(1);
        }
    }

    @Override
    protected int getMaxProgress() {
        return PowerCrystalEffects.getModifiedProcessTime(PROCESS_TIME, getEffectiveCrystalStack());
    }

    @Override
    protected int getEnergyPerTick() {
        return PowerCrystalEffects.getConstructorEnergyPerTick(ENERGY_PER_TICK, getEffectiveCrystalStack());
    }

    @Override
    protected ItemStack getEffectiveCrystalStack() {
        return progress > 0 && processCrystalLatched ? activeProcessCrystal : itemHandler.getStackInSlot(SLOT_CRYSTAL);
    }

    @Override
    protected void latchProcessCrystal() {
        processCrystalLatched = true;
        ItemStack crystalStack = itemHandler.getStackInSlot(SLOT_CRYSTAL);
        if (PowerCrystalEffects.isActive(crystalStack)) {
            activeProcessCrystal = crystalStack.copy();
            PowerCrystalData.drainCharge(crystalStack, PowerCrystalEffects.CRYSTAL_CHARGE_COST);
        } else {
            activeProcessCrystal = ItemStack.EMPTY;
        }
    }

    private void transferEnergyFromPowerInputItem() {
        ItemStack energyStack = itemHandler.getStackInSlot(SLOT_POWER_INPUT);
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
    }

    private boolean canOutputSingularity() {
        ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);
        return outputStack.isEmpty()
                || (outputStack.is(ModItems.MATTER_SINGULARITY.get()) && outputStack.getCount() < outputStack.getMaxStackSize());
    }

    private void importRawMatterBucket() {
        ItemStack inputStack = itemHandler.getStackInSlot(RAW_BUCKET_INPUT_SLOT);
        if (!inputStack.is(ModItems.RAW_MATTER_BUCKET.get()) || fluidTank.getSpace() < FluidType.BUCKET_VOLUME) {
            return;
        }

        ItemStack outputStack = itemHandler.getStackInSlot(RAW_BUCKET_OUTPUT_SLOT);
        if (!outputStack.isEmpty() && (!outputStack.is(Items.BUCKET) || outputStack.getCount() >= outputStack.getMaxStackSize())) {
            return;
        }

        fluidTank.fill(new FluidStack(ModFluids.RAW_MATTER.get(), FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
        inputStack.shrink(1);
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(RAW_BUCKET_OUTPUT_SLOT, new ItemStack(Items.BUCKET));
        } else {
            outputStack.grow(1);
        }
    }

    private void exportSludgeBucket() {
        ItemStack inputStack = itemHandler.getStackInSlot(SLUDGE_BUCKET_INPUT_SLOT);
        if (!inputStack.is(Items.BUCKET) || unstableTank.getFluidAmount() < FluidType.BUCKET_VOLUME) {
            return;
        }

        ItemStack outputStack = itemHandler.getStackInSlot(SLUDGE_BUCKET_OUTPUT_SLOT);
        if (!outputStack.isEmpty() && (!outputStack.is(ModItems.UNSTABLE_MATTER_BUCKET.get()) || outputStack.getCount() >= outputStack.getMaxStackSize())) {
            return;
        }

        unstableTank.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
        inputStack.shrink(1);
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(SLUDGE_BUCKET_OUTPUT_SLOT, ModItems.UNSTABLE_MATTER_BUCKET.get().getDefaultInstance());
        } else {
            outputStack.grow(1);
        }
    }
}
