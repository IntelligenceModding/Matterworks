package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.menu.MatterStabilizerMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModFluids;
import de.artemis.matterworks.common.registry.ModItems;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class MatterStabilizerBlockEntity extends AbstractMatterMachineBlockEntity implements MenuProvider {
    public static final int RAW_BUCKET_INPUT_SLOT = FLUID_BUCKET_INPUT_SLOT;
    public static final int RAW_BUCKET_OUTPUT_SLOT = FLUID_BUCKET_OUTPUT_SLOT;
    public static final int UNSTABLE_BUCKET_INPUT_SLOT = 7;
    public static final int UNSTABLE_BUCKET_OUTPUT_SLOT = 8;
    public static final int REFINED_BUCKET_INPUT_SLOT = 9;
    public static final int REFINED_BUCKET_OUTPUT_SLOT = 10;

    public static final int DATA_RAW_FLUID_AMOUNT = DATA_COUNT;
    public static final int DATA_RAW_FLUID_CAPACITY = DATA_COUNT + 1;
    public static final int DATA_UNSTABLE_FLUID_AMOUNT = DATA_COUNT + 2;
    public static final int DATA_UNSTABLE_FLUID_CAPACITY = DATA_COUNT + 3;
    public static final int STABILIZER_DATA_COUNT = DATA_COUNT + 4;

    public static final int PROCESS_TIME = 120;
    public static final int RAW_MATTER_TANK_CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    public static final int REFINED_MATTER_TANK_CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    public static final int UNSTABLE_MATTER_TANK_CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    public static final int ENERGY_CAPACITY = 10000;
    public static final int ENERGY_PER_TICK = 30;
    public static final int RAW_MATTER_COST = 100;
    public static final int REFINED_MATTER_OUTPUT = 95;
    public static final int UNSTABLE_MATTER_OUTPUT = 5;
    private static final int SLOT_COUNT = 11;

    private final FluidTank rawMatterTank = new FluidTank(RAW_MATTER_TANK_CAPACITY, ModFluids::isRawMatter) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final FluidTank unstableMatterTank = new FluidTank(UNSTABLE_MATTER_TANK_CAPACITY, ModFluids::isUnstableMatter) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final ContainerData stabilizerData = new ContainerData() {
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
                case DATA_RAW_FLUID_AMOUNT -> rawMatterTank.getFluidAmount();
                case DATA_RAW_FLUID_CAPACITY -> rawMatterTank.getCapacity();
                case DATA_UNSTABLE_FLUID_AMOUNT -> unstableMatterTank.getFluidAmount();
                case DATA_UNSTABLE_FLUID_CAPACITY -> unstableMatterTank.getCapacity();
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
            return STABILIZER_DATA_COUNT;
        }
    };

    private final IFluidHandler fluidAutomationHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 3;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> rawMatterTank.getFluidInTank(0);
                case 1 -> fluidTank.getFluidInTank(0);
                case 2 -> unstableMatterTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> rawMatterTank.getTankCapacity(0);
                case 1 -> fluidTank.getTankCapacity(0);
                case 2 -> unstableMatterTank.getTankCapacity(0);
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && rawMatterTank.isFluidValid(0, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return rawMatterTank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack drained = fluidTank.drain(resource, action);
            return drained.isEmpty() ? unstableMatterTank.drain(resource, action) : drained;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack drained = fluidTank.drain(maxDrain, action);
            return drained.isEmpty() ? unstableMatterTank.drain(maxDrain, action) : drained;
        }
    };

    public MatterStabilizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.MATTER_STABILIZER.get(),
                pos,
                blockState,
                REFINED_MATTER_TANK_CAPACITY,
                ModFluids::isRefinedMatter,
                ENERGY_CAPACITY,
                SLOT_COUNT
        );
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, MatterStabilizerBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MatterStabilizerMenu(containerId, playerInventory, this, stabilizerData);
    }

    @Override
    public @Nullable IFluidHandler getFluidAutomationHandler(@Nullable Direction side) {
        return fluidAutomationHandler;
    }

    @Override
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
        tag.put("raw_input_tank", rawMatterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("unstable_output_tank", unstableMatterTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        migrateInventoryFromSavedState();

        if (tag.contains("raw_input_tank")) {
            rawMatterTank.readFromNBT(registries, tag.getCompound("raw_input_tank"));
        } else if (tag.contains("raw_tank")) {
            rawMatterTank.readFromNBT(registries, tag.getCompound("raw_tank"));
        }

        if (tag.contains("unstable_output_tank")) {
            unstableMatterTank.readFromNBT(registries, tag.getCompound("unstable_output_tank"));
        } else if (tag.contains("unstable_tank")) {
            unstableMatterTank.readFromNBT(registries, tag.getCompound("unstable_tank"));
        }
    }

    private void migrateInventoryFromSavedState() {
        if (itemHandler.getSlots() >= SLOT_COUNT) {
            return;
        }

        ItemStack oldRawInput = itemHandler.getSlots() > RAW_BUCKET_INPUT_SLOT ? itemHandler.getStackInSlot(RAW_BUCKET_INPUT_SLOT).copy() : ItemStack.EMPTY;
        ItemStack oldLegacyExchange = itemHandler.getSlots() > RAW_BUCKET_OUTPUT_SLOT ? itemHandler.getStackInSlot(RAW_BUCKET_OUTPUT_SLOT).copy() : ItemStack.EMPTY;
        ItemStack oldCrystal = itemHandler.getSlots() > CRYSTAL_SLOT ? itemHandler.getStackInSlot(CRYSTAL_SLOT).copy() : ItemStack.EMPTY;

        itemHandler.setSize(SLOT_COUNT);

        if (oldRawInput.is(ModItems.RAW_MATTER_BUCKET.get())) {
            itemHandler.setStackInSlot(RAW_BUCKET_INPUT_SLOT, oldRawInput);
        } else if (oldRawInput.is(Items.BUCKET)) {
            itemHandler.setStackInSlot(RAW_BUCKET_OUTPUT_SLOT, oldRawInput);
        }

        if (oldLegacyExchange.is(ModItems.UNSTABLE_MATTER_BUCKET.get())) {
            itemHandler.setStackInSlot(UNSTABLE_BUCKET_OUTPUT_SLOT, oldLegacyExchange);
        } else if (oldLegacyExchange.is(ModItems.REFINED_MATTER_BUCKET.get())) {
            itemHandler.setStackInSlot(REFINED_BUCKET_OUTPUT_SLOT, oldLegacyExchange);
        } else if (oldLegacyExchange.is(Items.BUCKET)) {
            itemHandler.setStackInSlot(REFINED_BUCKET_INPUT_SLOT, oldLegacyExchange);
        }

        itemHandler.setStackInSlot(CRYSTAL_SLOT, oldCrystal);
    }

    @Override
    protected void beforeProcessingTick() {
        super.beforeProcessingTick();
        importRawMatterBucket();
        exportUnstableMatterBucket();
        exportRefinedMatterBucket();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(ModBlocks.MATTER_STABILIZER.get().getDescriptionId());
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        if (slot == ENERGY_ITEM_INPUT_SLOT) {
            return isEnergyItem(stack);
        }
        if (slot == ENERGY_ITEM_OUTPUT_SLOT || slot == RAW_BUCKET_OUTPUT_SLOT || slot == UNSTABLE_BUCKET_OUTPUT_SLOT || slot == REFINED_BUCKET_OUTPUT_SLOT) {
            return false;
        }
        if (slot == RAW_BUCKET_INPUT_SLOT) {
            return stack.is(ModItems.RAW_MATTER_BUCKET.get());
        }
        if (slot == UNSTABLE_BUCKET_INPUT_SLOT || slot == REFINED_BUCKET_INPUT_SLOT) {
            return stack.is(Items.BUCKET);
        }
        if (slot == CRYSTAL_SLOT) {
            return PowerCrystalEffects.isPowerCrystal(stack);
        }
        return false;
    }

    @Override
    protected boolean canProcess() {
        return rawMatterTank.getFluidAmount() >= RAW_MATTER_COST
                && fluidTank.getSpace() >= REFINED_MATTER_OUTPUT
                && unstableMatterTank.getSpace() >= UNSTABLE_MATTER_OUTPUT;
    }

    @Override
    protected void processItem() {
        rawMatterTank.drain(RAW_MATTER_COST, IFluidHandler.FluidAction.EXECUTE);
        fluidTank.fill(new FluidStack(ModFluids.REFINED_MATTER.get(), REFINED_MATTER_OUTPUT), IFluidHandler.FluidAction.EXECUTE);
        unstableMatterTank.fill(new FluidStack(ModFluids.UNSTABLE_MATTER.get(), UNSTABLE_MATTER_OUTPUT), IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    protected void processFailed() {
        rawMatterTank.drain(RAW_MATTER_COST, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    protected int getMaxProgress() {
        return PowerCrystalEffects.getModifiedProcessTime(PROCESS_TIME, getEffectiveCrystalStack());
    }

    @Override
    protected int getEnergyPerTick() {
        return PowerCrystalEffects.getConstructorEnergyPerTick(ENERGY_PER_TICK, getEffectiveCrystalStack());
    }

    private void importRawMatterBucket() {
        ItemStack inputStack = itemHandler.getStackInSlot(RAW_BUCKET_INPUT_SLOT);
        if (!inputStack.is(ModItems.RAW_MATTER_BUCKET.get()) || rawMatterTank.getSpace() < FluidType.BUCKET_VOLUME) {
            return;
        }

        ItemStack outputStack = itemHandler.getStackInSlot(RAW_BUCKET_OUTPUT_SLOT);
        if (!outputStack.isEmpty() && (!outputStack.is(Items.BUCKET) || outputStack.getCount() >= outputStack.getMaxStackSize())) {
            return;
        }

        rawMatterTank.fill(new FluidStack(ModFluids.RAW_MATTER.get(), FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
        inputStack.shrink(1);
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(RAW_BUCKET_OUTPUT_SLOT, new ItemStack(Items.BUCKET));
        } else {
            outputStack.grow(1);
        }
    }

    private void exportUnstableMatterBucket() {
        ItemStack inputStack = itemHandler.getStackInSlot(UNSTABLE_BUCKET_INPUT_SLOT);
        if (!inputStack.is(Items.BUCKET) || unstableMatterTank.getFluidAmount() < FluidType.BUCKET_VOLUME) {
            return;
        }

        ItemStack outputStack = itemHandler.getStackInSlot(UNSTABLE_BUCKET_OUTPUT_SLOT);
        if (!outputStack.isEmpty() && (!outputStack.is(ModItems.UNSTABLE_MATTER_BUCKET.get()) || outputStack.getCount() >= outputStack.getMaxStackSize())) {
            return;
        }

        unstableMatterTank.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
        inputStack.shrink(1);
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(UNSTABLE_BUCKET_OUTPUT_SLOT, ModItems.UNSTABLE_MATTER_BUCKET.get().getDefaultInstance());
        } else {
            outputStack.grow(1);
        }
    }

    private void exportRefinedMatterBucket() {
        ItemStack inputStack = itemHandler.getStackInSlot(REFINED_BUCKET_INPUT_SLOT);
        if (!inputStack.is(Items.BUCKET) || fluidTank.getFluidAmount() < FluidType.BUCKET_VOLUME) {
            return;
        }

        ItemStack outputStack = itemHandler.getStackInSlot(REFINED_BUCKET_OUTPUT_SLOT);
        if (!outputStack.isEmpty() && (!outputStack.is(ModItems.REFINED_MATTER_BUCKET.get()) || outputStack.getCount() >= outputStack.getMaxStackSize())) {
            return;
        }

        fluidTank.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
        inputStack.shrink(1);
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(REFINED_BUCKET_OUTPUT_SLOT, ModItems.REFINED_MATTER_BUCKET.get().getDefaultInstance());
        } else {
            outputStack.grow(1);
        }
    }
}
