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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public class MatterRecyclerBlockEntity extends AbstractMatterMachineBlockEntity implements MenuProvider {
    public static final int PROCESS_TIME = 80;
    public static final int TANK_CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    public static final int ENERGY_CAPACITY = 10000;
    public static final int ENERGY_PER_TICK = 20;
    private int queuedMatterOutput;

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

    public MatterRecyclerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MATTER_RECYCLER.get(), pos, blockState, TANK_CAPACITY, ModFluids::isRawMatter, ENERGY_CAPACITY);
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
        transferEnergyFromInputItem();

        ItemStack inputStack = itemHandler.getStackInSlot(INPUT_SLOT);
        MatterValueResult inputEvaluation = evaluateInputStack(inputStack);

        if (moveRejectedInputToOutput(inputStack, inputEvaluation)) {
            queuedMatterOutput = 0;
            if (progress != 0) {
                progress = 0;
                clearLatchedProcessCrystal();
                setChanged();
            }
            afterProcessingTick();
            return;
        }

        queuedMatterOutput = calculateRawMatterOutput(inputEvaluation);

        if (canProcess(inputEvaluation) && hasEnoughEnergy()) {
            if (progress == 0) {
                latchProcessCrystal();
            }
            energyStorage.extractEnergy(getEnergyPerTick(), false);
            progress++;
            setChanged();
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
    protected @Nullable IFluidHandler getBaseFluidAutomationHandler() {
        return fluidAutomationHandler;
    }

    @Override
    protected void beforeProcessingTick() {
    }

    @Override
    protected boolean canProcess() {
        return canProcess(evaluateInputStack(itemHandler.getStackInSlot(INPUT_SLOT)));
    }

    @Override
    protected void processItem() {
        ItemStack inputStack = itemHandler.getStackInSlot(INPUT_SLOT);
        int outputMatter = queuedMatterOutput > 0 ? queuedMatterOutput : calculateRawMatterOutput(evaluateInputStack(inputStack));
        if (outputMatter < 1) {
            return;
        }

        ItemStack processedStack = inputStack.copyWithCount(1);
        inputStack.shrink(1);
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
        return true;
    }
    @Override
    protected void afterProcessingTick() {
        ItemStack bucketInputStack = itemHandler.getStackInSlot(FLUID_BUCKET_INPUT_SLOT);
        ItemStack bucketOutputStack = itemHandler.getStackInSlot(FLUID_BUCKET_OUTPUT_SLOT);
        if (!bucketInputStack.is(Items.BUCKET) || fluidTank.getFluidAmount() < FluidType.BUCKET_VOLUME) {
            return;
        }

        if (!bucketOutputStack.isEmpty() && (!bucketOutputStack.is(ModItems.RAW_MATTER_BUCKET.get()) || bucketOutputStack.getCount() >= bucketOutputStack.getMaxStackSize())) {
            return;
        }

        bucketInputStack.shrink(1);
        if (bucketOutputStack.isEmpty()) {
            itemHandler.setStackInSlot(FLUID_BUCKET_OUTPUT_SLOT, ModItems.RAW_MATTER_BUCKET.get().getDefaultInstance());
        } else {
            bucketOutputStack.grow(1);
        }
        fluidTank.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    protected int getSlotLimit(int slot) {
        return slot == CRYSTAL_SLOT ? 1 : super.getSlotLimit(slot);
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        if (slot == ENERGY_ITEM_INPUT_SLOT) {
            return isEnergyItem(stack);
        }

        if (slot == ENERGY_ITEM_OUTPUT_SLOT) {
            return false;
        }

        if (slot == INPUT_SLOT) {
            return canAcceptRecyclerInput(stack);
        }

        if (slot == FLUID_BUCKET_INPUT_SLOT) {
            return stack.is(Items.BUCKET);
        }

        if (slot == CRYSTAL_SLOT) {
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
        return !stack.isEmpty();
    }

    private MatterValueResult evaluateInputStack(ItemStack inputStack) {
        return level == null ? MatterValueResult.rejected("missing_level") : MatterValueManager.evaluateRecycler(inputStack, level);
    }

    private boolean canProcess(MatterValueResult evaluation) {
        return !itemHandler.getStackInSlot(INPUT_SLOT).isEmpty()
                && evaluation.allowed()
                && fluidTank.getSpace() >= evaluation.matterMillibuckets();
    }

    private boolean moveRejectedInputToOutput(ItemStack inputStack, MatterValueResult evaluation) {
        if (level == null || inputStack.isEmpty() || evaluation.allowed()) {
            return false;
        }

        ItemStack outputStack = itemHandler.getStackInSlot(INVALID_OUTPUT_SLOT);
        if (!outputStack.isEmpty() && (!ItemStack.isSameItemSameComponents(outputStack, inputStack) || outputStack.getCount() >= outputStack.getMaxStackSize())) {
            return false;
        }

        ItemStack movedStack = inputStack.copy();
        itemHandler.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(INVALID_OUTPUT_SLOT, movedStack);
        } else {
            outputStack.grow(movedStack.getCount());
        }
        MatterValueManager.debugRecyclerRejected(level, movedStack, evaluation.reason());
        setChanged();
        return true;
    }
}
