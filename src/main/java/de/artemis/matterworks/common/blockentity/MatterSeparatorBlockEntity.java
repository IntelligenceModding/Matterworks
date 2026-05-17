package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.menu.MatterSeparatorMenu;
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

public class MatterSeparatorBlockEntity extends AbstractMatterMachineBlockEntity implements MenuProvider {
    public static final int DUST_OUTPUT_SLOT = INPUT_SLOT;
    public static final int SLUDGE_BUCKET_INPUT_SLOT = INVALID_OUTPUT_SLOT;
    public static final int REFINED_BUCKET_INPUT_SLOT = FLUID_BUCKET_INPUT_SLOT;
    public static final int REFINED_BUCKET_OUTPUT_SLOT = FLUID_BUCKET_OUTPUT_SLOT;
    public static final int SLUDGE_BUCKET_OUTPUT_SLOT = 7;

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
    private static final int SLOT_COUNT = 8;

    private int sludgeOverflowBuffer;

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
                case 0 -> itemHandler.getStackInSlot(ENERGY_ITEM_INPUT_SLOT);
                case 1 -> itemHandler.getStackInSlot(REFINED_BUCKET_INPUT_SLOT);
                case 2 -> itemHandler.getStackInSlot(SLUDGE_BUCKET_INPUT_SLOT);
                case 3 -> itemHandler.getStackInSlot(CRYSTAL_SLOT);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return switch (slot) {
                case 0 -> itemHandler.insertItem(ENERGY_ITEM_INPUT_SLOT, stack, simulate);
                case 1 -> itemHandler.insertItem(REFINED_BUCKET_INPUT_SLOT, stack, simulate);
                case 2 -> itemHandler.insertItem(SLUDGE_BUCKET_INPUT_SLOT, stack, simulate);
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
                case 0, 1, 2, 3 -> 1;
                default -> 0;
            };
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> itemHandler.isItemValid(ENERGY_ITEM_INPUT_SLOT, stack);
                case 1 -> itemHandler.isItemValid(REFINED_BUCKET_INPUT_SLOT, stack);
                case 2 -> itemHandler.isItemValid(SLUDGE_BUCKET_INPUT_SLOT, stack);
                case 3 -> itemHandler.isItemValid(CRYSTAL_SLOT, stack);
                default -> false;
            };
        }
    };

    private final IItemHandler automationOutputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 4;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return switch (slot) {
                case 0 -> itemHandler.getStackInSlot(ENERGY_ITEM_OUTPUT_SLOT);
                case 1 -> itemHandler.getStackInSlot(DUST_OUTPUT_SLOT);
                case 2 -> itemHandler.getStackInSlot(REFINED_BUCKET_OUTPUT_SLOT);
                case 3 -> itemHandler.getStackInSlot(SLUDGE_BUCKET_OUTPUT_SLOT);
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
                case 1 -> itemHandler.extractItem(DUST_OUTPUT_SLOT, amount, simulate);
                case 2 -> itemHandler.extractItem(REFINED_BUCKET_OUTPUT_SLOT, amount, simulate);
                case 3 -> itemHandler.extractItem(SLUDGE_BUCKET_OUTPUT_SLOT, amount, simulate);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return switch (slot) {
                case 0 -> itemHandler.getSlotLimit(ENERGY_ITEM_OUTPUT_SLOT);
                case 1 -> itemHandler.getSlotLimit(DUST_OUTPUT_SLOT);
                case 2 -> itemHandler.getSlotLimit(REFINED_BUCKET_OUTPUT_SLOT);
                case 3 -> itemHandler.getSlotLimit(SLUDGE_BUCKET_OUTPUT_SLOT);
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

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MatterSeparatorMenu(containerId, playerInventory, this, separatorData);
    }

    @Override
    public @Nullable IItemHandler getAutomationHandler(@Nullable Direction side) {
        if (side == null) {
            return itemHandler;
        }
        return side == Direction.DOWN ? automationOutputHandler : automationInputHandler;
    }

    @Override
    public @Nullable IFluidHandler getFluidAutomationHandler(@Nullable Direction side) {
        return fluidAutomationHandler;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("sludge_tank", sludgeTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("sludge_overflow_buffer", sludgeOverflowBuffer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("sludge_tank")) {
            sludgeTank.readFromNBT(registries, tag.getCompound("sludge_tank"));
        }
        sludgeOverflowBuffer = tag.getInt("sludge_overflow_buffer");
    }

    @Override
    protected void beforeProcessingTick() {
        super.beforeProcessingTick();
        importRefinedMatterBucket();
        exportSludgeBucket();
    }

    @Override
    protected void afterProcessingTick() {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && sludgeOverflowBuffer > 0) {
            sludgeOverflowBuffer = MatterSludgeOverflowHelper.spillBufferedOverflow(serverLevel, worldPosition, sludgeOverflowBuffer);
        }
    }

    @Override
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
        if (slot == ENERGY_ITEM_INPUT_SLOT) {
            return isEnergyItem(stack);
        }
        if (slot == ENERGY_ITEM_OUTPUT_SLOT || slot == REFINED_BUCKET_OUTPUT_SLOT || slot == DUST_OUTPUT_SLOT || slot == SLUDGE_BUCKET_OUTPUT_SLOT) {
            return false;
        }
        if (slot == REFINED_BUCKET_INPUT_SLOT) {
        return stack.is(ModItems.REFINED_MATTER_BUCKET.get());
        }
        if (slot == SLUDGE_BUCKET_INPUT_SLOT) {
            return stack.is(Items.BUCKET);
        }
        if (slot == CRYSTAL_SLOT) {
            return PowerCrystalEffects.isPowerCrystal(stack);
        }
        return false;
    }

    @Override
    protected boolean canProcess() {
        return fluidTank.getFluidAmount() >= REFINED_MATTER_COST
                && level != null
                && MatterSludgeOverflowHelper.canAcceptOutput(level, worldPosition, sludgeTank, MATTER_SLUDGE_OUTPUT, sludgeOverflowBuffer)
                && canOutputDust();
    }

    @Override
    protected void processItem() {
        fluidTank.drain(REFINED_MATTER_COST, IFluidHandler.FluidAction.EXECUTE);
        sludgeOverflowBuffer = MatterSludgeOverflowHelper.fillTankAndBufferOverflow(sludgeTank, MATTER_SLUDGE_OUTPUT, sludgeOverflowBuffer);
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && sludgeOverflowBuffer > 0) {
            sludgeOverflowBuffer = MatterSludgeOverflowHelper.spillBufferedOverflow(serverLevel, worldPosition, sludgeOverflowBuffer);
        }

        ItemStack dustStack = itemHandler.getStackInSlot(DUST_OUTPUT_SLOT);
        if (dustStack.isEmpty()) {
            itemHandler.setStackInSlot(DUST_OUTPUT_SLOT, ModItems.MATTER_DUST.get().getDefaultInstance().copyWithCount(DUST_OUTPUT_COUNT));
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

    private boolean canOutputDust() {
        ItemStack dustStack = itemHandler.getStackInSlot(DUST_OUTPUT_SLOT);
        return dustStack.isEmpty()
                || (dustStack.is(ModItems.MATTER_DUST.get()) && dustStack.getCount() <= dustStack.getMaxStackSize() - DUST_OUTPUT_COUNT);
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
}
