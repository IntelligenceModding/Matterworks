package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.matter.MatterValueManager;
import de.artemis.matterworks.common.matter.MatterValueResult;
import de.artemis.matterworks.common.menu.MatterRecyclerMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModFluids;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.upgrade.PowerCrystalData;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class MatterRecyclerBlockEntity extends AbstractMatterMachineBlockEntity implements MenuProvider {
    public static final int SLOT_POWER_INPUT = 0;
    public static final int INPUT_SLOT_START = 1;
    public static final int INPUT_SLOT_COUNT = 24;
    public static final int SLOT_BUCKET_INPUT = 25;
    public static final int SLOT_BUCKET_OUTPUT = 26;
    public static final int SLOT_CRYSTAL = 27;
    public static final int SLOT_COUNT = 28;
    public static final int PROCESS_TIME = 80;
    public static final int TANK_CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    public static final int ENERGY_CAPACITY = 10000;
    public static final int ENERGY_PER_TICK = 20;
    private int queuedMatterOutput;
    private int activeInputSlot = -1;

    private final IFluidHandler fluidAutomationHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return fluidTank.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return fluidTank.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return fluidTank.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return fluidTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return fluidTank.drain(maxDrain, action);
        }
    };

    private final IItemHandler automationInputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 27;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return switch (slot) {
                case 0 -> itemHandler.getStackInSlot(SLOT_POWER_INPUT);
                case 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                     13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24
                        -> itemHandler.getStackInSlot(INPUT_SLOT_START + slot - 1);
                case 25 -> itemHandler.getStackInSlot(SLOT_BUCKET_INPUT);
                case 26 -> itemHandler.getStackInSlot(SLOT_CRYSTAL);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return switch (slot) {
                case 0 -> itemHandler.insertItem(SLOT_POWER_INPUT, stack, simulate);
                case 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                     13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24
                        -> itemHandler.insertItem(INPUT_SLOT_START + slot - 1, stack, simulate);
                case 25 -> itemHandler.insertItem(SLOT_BUCKET_INPUT, stack, simulate);
                case 26 -> itemHandler.insertItem(SLOT_CRYSTAL, stack, simulate);
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
                case 0, 26 -> 1;
                case 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                     13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25 -> 64;
                default -> 0;
            };
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> itemHandler.isItemValid(SLOT_POWER_INPUT, stack);
                case 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                     13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24
                        -> itemHandler.isItemValid(INPUT_SLOT_START + slot - 1, stack);
                case 25 -> itemHandler.isItemValid(SLOT_BUCKET_INPUT, stack);
                case 26 -> itemHandler.isItemValid(SLOT_CRYSTAL, stack);
                default -> false;
            };
        }
    };

    private final IItemHandler automationOutputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? itemHandler.getStackInSlot(SLOT_BUCKET_OUTPUT) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? itemHandler.extractItem(SLOT_BUCKET_OUTPUT, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 64 : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    public MatterRecyclerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MATTER_RECYCLER.get(), pos, blockState, TANK_CAPACITY, ModFluids::isRawMatter, ENERGY_CAPACITY, SLOT_COUNT);
        for (Direction side : Direction.values()) {
            initializeSideAccessMode(SideConfigType.FLUIDS, side, SideAccessMode.OUTPUT);
        }
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, MatterRecyclerBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        syncFluidTankCapacities();
        transferEnergyFromPowerInputItem();

        MatterValueResult inputEvaluation = getActiveInputEvaluation();
        queuedMatterOutput = calculateRawMatterOutput(inputEvaluation);
        boolean changed = false;

        if (canProcess(inputEvaluation) && hasEnoughEnergy()) {
            if (progress == 0) {
                latchProcessCrystal();
            }
            energyStorage.extractEnergy(getEnergyPerTick(), false);
            progress++;
            changed = true;
            if (progress >= getMaxProgress()) {
                progress = 0;
                processItem();
                clearLatchedProcessCrystal();
                changed = true;
            }
        } else if (progress != 0) {
            progress = 0;
            clearLatchedProcessCrystal();
            activeInputSlot = -1;
            changed = true;
        }

        if (changed) {
            setChanged();
        }
        afterProcessingTick();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(ModBlocks.MATTER_RECYCLER.get().getDescriptionId());
    }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MatterRecyclerMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    protected boolean supportsFluidSideConfigInput() {
        return false;
    }

    @Override
    protected boolean supportsFluidSideConfigOutput() {
        return true;
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
    protected @Nullable IFluidHandler getBaseFluidAutomationHandler() {
        return fluidAutomationHandler;
    }

    @Override
    protected void beforeProcessingTick() {
    }

    @Override
    protected boolean canProcess() {
        return canProcess(getActiveInputEvaluation());
    }

    @Override
    protected void processItem() {
        if (!isValidInputSlot(activeInputSlot)) {
            activeInputSlot = findProcessInputSlot();
        }
        if (!isValidInputSlot(activeInputSlot)) {
            return;
        }

        ItemStack inputStack = itemHandler.getStackInSlot(activeInputSlot);
        int outputMatter = queuedMatterOutput > 0 ? queuedMatterOutput : calculateRawMatterOutput(evaluateInputStack(inputStack));
        if (outputMatter < 1) {
            return;
        }

        ItemStack processedStack = inputStack.copyWithCount(1);
        inputStack.shrink(1);
        if (inputStack.isEmpty()) {
            activeInputSlot = -1;
        }
        fluidTank.fill(new FluidStack(ModFluids.RAW_MATTER.get(), outputMatter), IFluidHandler.FluidAction.EXECUTE);
        MatterValueManager.debugRecyclerResult(level, processedStack, outputMatter);
    }

    @Override
    protected int getMaxProgress() {
        return PowerCrystalEffects.getModifiedProcessTime(PROCESS_TIME, getEffectiveCrystalStack());
    }

    @Override
    protected int getEnergyPerTick() {
        return ENERGY_PER_TICK;
    }

    @Override
    protected boolean hasEnoughEnergy() {
        return energyStorage.getEnergyStored() >= getEnergyPerTick();
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

    @Override
    protected int getSlotLimit(int slot) {
        return slot == SLOT_POWER_INPUT || slot == SLOT_CRYSTAL ? 1 : 64;
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        if (slot == SLOT_POWER_INPUT) {
            return isEnergyItem(stack);
        }

        if (slot == SLOT_BUCKET_OUTPUT) {
            return false;
        }

        if (slot >= INPUT_SLOT_START && slot < INPUT_SLOT_START + INPUT_SLOT_COUNT) {
            return canAcceptRecyclerInput(stack);
        }

        if (slot == SLOT_BUCKET_INPUT) {
            return stack.is(Items.BUCKET);
        }

        if (slot == SLOT_CRYSTAL) {
            return PowerCrystalEffects.isPowerCrystal(stack);
        }

        return false;
    }

    private int getRawMatterOutput(ItemStack inputStack) {
        return calculateRawMatterOutput(evaluateInputStack(inputStack));
    }

    private int calculateRawMatterOutput(MatterValueResult evaluation) {
        return evaluation.allowed() ? evaluation.matterMillibuckets() : 0;
    }

    private boolean canAcceptRecyclerInput(ItemStack stack) {
        return level != null
                && !stack.isEmpty()
                && !PowerCrystalEffects.isPowerCrystal(stack)
                && !EnergyItemHelper.canProvideEnergy(stack)
                && evaluateInputStack(stack).allowed();
    }

    private MatterValueResult evaluateInputStack(ItemStack inputStack) {
        return level == null ? MatterValueResult.rejected("missing_level") : MatterValueManager.evaluateRecycler(inputStack, level);
    }

    private boolean canProcess(MatterValueResult evaluation) {
        return isValidInputSlot(activeInputSlot)
                && !itemHandler.getStackInSlot(activeInputSlot).isEmpty()
                && evaluation.allowed()
                && fluidTank.getSpace() >= evaluation.matterMillibuckets();
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (activeInputSlot >= 0) {
            tag.putInt("active_input_slot", activeInputSlot);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        migrateLegacyRecyclerInventory();
        activeInputSlot = tag.contains("active_input_slot") ? tag.getInt("active_input_slot") : -1;
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

    private MatterValueResult getActiveInputEvaluation() {
        if (!isValidInputSlot(activeInputSlot)) {
            activeInputSlot = findProcessInputSlot();
        }
        return isValidInputSlot(activeInputSlot)
                ? evaluateInputStack(itemHandler.getStackInSlot(activeInputSlot))
                : MatterValueResult.rejected("missing_input");
    }

    private int findProcessInputSlot() {
        for (int slot = INPUT_SLOT_START; slot < INPUT_SLOT_START + INPUT_SLOT_COUNT; slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            MatterValueResult evaluation = evaluateInputStack(stack);
            if (evaluation.allowed()) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isValidInputSlot(int slot) {
        return slot >= INPUT_SLOT_START && slot < INPUT_SLOT_START + INPUT_SLOT_COUNT;
    }

    private void migrateLegacyRecyclerInventory() {
        boolean legacyLayout = itemHandler.getStackInSlot(SLOT_CRYSTAL).isEmpty() && PowerCrystalEffects.isPowerCrystal(itemHandler.getStackInSlot(15));
        legacyLayout |= EnergyItemHelper.canProvideEnergy(itemHandler.getStackInSlot(1));
        if (!legacyLayout) {
            return;
        }

        LegacyInventoryMigration.moveIfMatches(itemHandler, 13, SLOT_CRYSTAL, PowerCrystalEffects::isPowerCrystal);
    }

    @Override
    protected void afterProcessingTick() {
        ItemStack bucketInputStack = itemHandler.getStackInSlot(SLOT_BUCKET_INPUT);
        ItemStack bucketOutputStack = itemHandler.getStackInSlot(SLOT_BUCKET_OUTPUT);
        if (!bucketInputStack.is(Items.BUCKET) || fluidTank.getFluidAmount() < FluidType.BUCKET_VOLUME) {
            return;
        }

        if (!bucketOutputStack.isEmpty() && (!bucketOutputStack.is(ModItems.RAW_MATTER_BUCKET.get()) || bucketOutputStack.getCount() >= bucketOutputStack.getMaxStackSize())) {
            return;
        }

        bucketInputStack.shrink(1);
        if (bucketOutputStack.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_BUCKET_OUTPUT, ModItems.RAW_MATTER_BUCKET.get().getDefaultInstance());
        } else {
            bucketOutputStack.grow(1);
        }
        fluidTank.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
    }
}
