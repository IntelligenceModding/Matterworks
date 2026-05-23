package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.menu.MatterSeparatorMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModFluids;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.upgrade.PowerCrystalData;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class MatterSeparatorBlockEntity extends AbstractMatterMachineBlockEntity implements MenuProvider {
    public static final int SLOT_POWER_INPUT = ENERGY_ITEM_INPUT_SLOT;
    public static final int OUTPUT_SLOT_START = 2;
    public static final int OUTPUT_SLOT_COUNT = 12;
    public static final int REFINED_BUCKET_INPUT_SLOT = 14;
    public static final int REFINED_BUCKET_OUTPUT_SLOT = 15;
    public static final int SLOT_CRYSTAL = 16;
    public static final int SLUDGE_BUCKET_INPUT_SLOT = 17;
    public static final int SLUDGE_BUCKET_OUTPUT_SLOT = 18;

    public static final int DATA_SLUDGE_AMOUNT = DATA_COUNT;
    public static final int DATA_SLUDGE_CAPACITY = DATA_COUNT + 1;
    public static final int SEPARATOR_DATA_COUNT = DATA_COUNT + 2;

    public static final int PROCESS_TIME = 100;
    public static final int ENERGY_CAPACITY = 12000;
    public static final int ENERGY_PER_TICK = 24;
    public static final int REFINED_MATTER_TANK_CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    public static final int MATTER_SLUDGE_TANK_CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    public static final int REFINED_MATTER_COST = 125;
    public static final int MATTER_SLUDGE_OUTPUT = 1000;
    public static final int DUST_OUTPUT_COUNT = 1;
    private static final int SLOT_COUNT = 19;

    private final FluidTank sludgeTank = new FluidTank(MATTER_SLUDGE_TANK_CAPACITY, ModFluids::isMatterSludge) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final ContainerData separatorData = new ContainerData() {
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
                case DATA_SLUDGE_AMOUNT -> sludgeTank.getFluidAmount();
                case DATA_SLUDGE_CAPACITY -> sludgeTank.getCapacity();
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
            return SEPARATOR_DATA_COUNT;
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
                case 1 -> itemHandler.getStackInSlot(REFINED_BUCKET_INPUT_SLOT);
                case 2 -> itemHandler.getStackInSlot(SLUDGE_BUCKET_INPUT_SLOT);
                case 3 -> itemHandler.getStackInSlot(SLOT_CRYSTAL);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return switch (slot) {
                case 0 -> itemHandler.insertItem(SLOT_POWER_INPUT, stack, simulate);
                case 1 -> itemHandler.insertItem(REFINED_BUCKET_INPUT_SLOT, stack, simulate);
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
                case 1 -> itemHandler.getSlotLimit(REFINED_BUCKET_INPUT_SLOT);
                case 2 -> itemHandler.getSlotLimit(SLUDGE_BUCKET_INPUT_SLOT);
                case 3 -> 1;
                default -> 0;
            };
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> itemHandler.isItemValid(SLOT_POWER_INPUT, stack);
                case 1 -> itemHandler.isItemValid(REFINED_BUCKET_INPUT_SLOT, stack);
                case 2 -> itemHandler.isItemValid(SLUDGE_BUCKET_INPUT_SLOT, stack);
                case 3 -> itemHandler.isItemValid(SLOT_CRYSTAL, stack);
                default -> false;
            };
        }
    };

    private final IItemHandler automationOutputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return OUTPUT_SLOT_COUNT + 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < OUTPUT_SLOT_COUNT) {
                return itemHandler.getStackInSlot(OUTPUT_SLOT_START + slot);
            }
            return switch (slot) {
                case OUTPUT_SLOT_COUNT -> itemHandler.getStackInSlot(REFINED_BUCKET_OUTPUT_SLOT);
                case OUTPUT_SLOT_COUNT + 1 -> itemHandler.getStackInSlot(SLUDGE_BUCKET_OUTPUT_SLOT);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < OUTPUT_SLOT_COUNT) {
                return itemHandler.extractItem(OUTPUT_SLOT_START + slot, amount, simulate);
            }
            return switch (slot) {
                case OUTPUT_SLOT_COUNT -> itemHandler.extractItem(REFINED_BUCKET_OUTPUT_SLOT, amount, simulate);
                case OUTPUT_SLOT_COUNT + 1 -> itemHandler.extractItem(SLUDGE_BUCKET_OUTPUT_SLOT, amount, simulate);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < OUTPUT_SLOT_COUNT) {
                return itemHandler.getSlotLimit(OUTPUT_SLOT_START + slot);
            }
            return switch (slot) {
                case OUTPUT_SLOT_COUNT -> itemHandler.getSlotLimit(REFINED_BUCKET_OUTPUT_SLOT);
                case OUTPUT_SLOT_COUNT + 1 -> itemHandler.getSlotLimit(SLUDGE_BUCKET_OUTPUT_SLOT);
                default -> 0;
            };
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    private final IFluidHandler fluidAutomationHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> fluidTank.getFluidInTank(0);
                case 1 -> sludgeTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> fluidTank.getTankCapacity(0);
                case 1 -> sludgeTank.getTankCapacity(0);
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
            return sludgeTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return sludgeTank.drain(maxDrain, action);
        }
    };

    public MatterSeparatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.MATTER_SEPARATOR.get(),
                pos,
                blockState,
                REFINED_MATTER_TANK_CAPACITY,
                ModFluids::isRefinedMatter,
                ENERGY_CAPACITY,
                SLOT_COUNT
        );
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, MatterSeparatorBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public FluidStack getSludgeFluidStack() {
        return sludgeTank.getFluid().copy();
    }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MatterSeparatorMenu(containerId, playerInventory, this, separatorData);
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
    protected IFluidHandler getBaseFluidAutomationHandler() {
        return fluidAutomationHandler;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("sludge_tank", sludgeTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        migrateLegacyInventory();
        if (tag.contains("sludge_tank")) {
            sludgeTank.readFromNBT(registries, tag.getCompound("sludge_tank"));
        }
    }

    @Override
    protected void beforeProcessingTick() {
        syncFluidTankCapacities();
        transferEnergyFromPowerInputItem();
        importRefinedMatterBucket();
        exportSludgeBucket();
    }

    protected void syncFluidTankCapacities() {
        super.syncFluidTankCapacities();
        syncTankCapacity(sludgeTank, getModifiedFluidTankCapacity(MATTER_SLUDGE_TANK_CAPACITY));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(ModBlocks.MATTER_SEPARATOR.get().getDescriptionId());
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        if (slot == SLOT_POWER_INPUT) {
            return isEnergyItem(stack);
        }
        if (slot == ENERGY_ITEM_OUTPUT_SLOT) {
            return false;
        }
        if (slot >= OUTPUT_SLOT_START && slot < OUTPUT_SLOT_START + OUTPUT_SLOT_COUNT) {
            return false;
        }
        if (slot == REFINED_BUCKET_INPUT_SLOT) {
            return stack.is(ModItems.REFINED_MATTER_BUCKET.get());
        }
        if (slot == REFINED_BUCKET_OUTPUT_SLOT) {
            return false;
        }
        if (slot == SLOT_CRYSTAL) {
            return PowerCrystalEffects.isPowerCrystal(stack);
        }
        if (slot == SLUDGE_BUCKET_INPUT_SLOT) {
            return stack.is(Items.BUCKET);
        }
        if (slot == SLUDGE_BUCKET_OUTPUT_SLOT) {
            return false;
        }
        return false;
    }

    @Override
    protected boolean canProcess() {
        return fluidTank.getFluidAmount() >= REFINED_MATTER_COST
                && level != null
                && sludgeTank.getSpace() >= MATTER_SLUDGE_OUTPUT
                && canOutputDust();
    }

    @Override
    protected void processItem() {
        fluidTank.drain(REFINED_MATTER_COST, IFluidHandler.FluidAction.EXECUTE);
        sludgeTank.fill(new FluidStack(ModFluids.MATTER_SLUDGE.get(), MATTER_SLUDGE_OUTPUT), IFluidHandler.FluidAction.EXECUTE);

        int outputSlot = findDustOutputSlot();
        if (outputSlot < 0) {
            return;
        }

        ItemStack dustStack = itemHandler.getStackInSlot(outputSlot);
        if (dustStack.isEmpty()) {
            itemHandler.setStackInSlot(outputSlot, ModItems.MATTER_DUST.get().getDefaultInstance().copyWithCount(DUST_OUTPUT_COUNT));
        } else {
            dustStack.grow(DUST_OUTPUT_COUNT);
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

    private boolean canOutputDust() {
        return findDustOutputSlot() >= 0;
    }

    private int findDustOutputSlot() {
        int emptySlot = -1;
        for (int slot = OUTPUT_SLOT_START; slot < OUTPUT_SLOT_START + OUTPUT_SLOT_COUNT; slot++) {
            ItemStack dustStack = itemHandler.getStackInSlot(slot);
            if (dustStack.isEmpty()) {
                if (emptySlot < 0) {
                    emptySlot = slot;
                }
                continue;
            }
            if (dustStack.is(ModItems.MATTER_DUST.get()) && dustStack.getCount() <= dustStack.getMaxStackSize() - DUST_OUTPUT_COUNT) {
                return slot;
            }
        }
        return emptySlot;
    }

    private void importRefinedMatterBucket() {
        ItemStack inputStack = itemHandler.getStackInSlot(REFINED_BUCKET_INPUT_SLOT);
        if (!inputStack.is(ModItems.REFINED_MATTER_BUCKET.get()) || fluidTank.getSpace() < FluidType.BUCKET_VOLUME) {
            return;
        }

        ItemStack outputStack = itemHandler.getStackInSlot(REFINED_BUCKET_OUTPUT_SLOT);
        if (!outputStack.isEmpty() && (!outputStack.is(Items.BUCKET) || outputStack.getCount() >= outputStack.getMaxStackSize())) {
            return;
        }

        fluidTank.fill(new FluidStack(ModFluids.REFINED_MATTER.get(), FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
        inputStack.shrink(1);
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(REFINED_BUCKET_OUTPUT_SLOT, new ItemStack(Items.BUCKET));
        } else {
            outputStack.grow(1);
        }
    }

    private void exportSludgeBucket() {
        ItemStack inputStack = itemHandler.getStackInSlot(SLUDGE_BUCKET_INPUT_SLOT);
        if (!inputStack.is(Items.BUCKET) || sludgeTank.getFluidAmount() < FluidType.BUCKET_VOLUME) {
            return;
        }

        ItemStack outputStack = itemHandler.getStackInSlot(SLUDGE_BUCKET_OUTPUT_SLOT);
        if (!outputStack.isEmpty() && (!outputStack.is(ModItems.MATTER_SLUDGE_BUCKET.get()) || outputStack.getCount() >= outputStack.getMaxStackSize())) {
            return;
        }

        sludgeTank.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
        inputStack.shrink(1);
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(SLUDGE_BUCKET_OUTPUT_SLOT, ModItems.MATTER_SLUDGE_BUCKET.get().getDefaultInstance());
        } else {
            outputStack.grow(1);
        }
    }

    private void migrateLegacyInventory() {
        moveLegacySlot(4, REFINED_BUCKET_INPUT_SLOT, stack -> stack.is(ModItems.REFINED_MATTER_BUCKET.get()));
        moveLegacySlot(5, REFINED_BUCKET_OUTPUT_SLOT, stack -> stack.is(Items.BUCKET));
        moveLegacySlot(6, SLOT_CRYSTAL, PowerCrystalEffects::isPowerCrystal);
        moveLegacySlot(7, SLUDGE_BUCKET_INPUT_SLOT, stack -> stack.is(Items.BUCKET) || stack.is(ModItems.MATTER_SLUDGE_BUCKET.get()));
        if (itemHandler.getStackInSlot(SLUDGE_BUCKET_INPUT_SLOT).is(ModItems.MATTER_SLUDGE_BUCKET.get())
                && itemHandler.getStackInSlot(SLUDGE_BUCKET_OUTPUT_SLOT).isEmpty()) {
            itemHandler.setStackInSlot(SLUDGE_BUCKET_OUTPUT_SLOT, itemHandler.getStackInSlot(SLUDGE_BUCKET_INPUT_SLOT).copy());
            itemHandler.setStackInSlot(SLUDGE_BUCKET_INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private void moveLegacySlot(int fromSlot, int targetSlot, java.util.function.Predicate<ItemStack> predicate) {
        ItemStack stack = itemHandler.getStackInSlot(fromSlot);
        if (stack.isEmpty() || !predicate.test(stack) || !itemHandler.getStackInSlot(targetSlot).isEmpty()) {
            return;
        }
        itemHandler.setStackInSlot(targetSlot, stack.copy());
        itemHandler.setStackInSlot(fromSlot, ItemStack.EMPTY);
    }
}
