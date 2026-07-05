package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.fluid.FluidItemHelper;
import de.artemis.matterworks.common.io.ConfiguredFluidHandler;
import de.artemis.matterworks.common.io.ConfiguredItemHandler;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.io.SideConfigurableBlockEntity;
import de.artemis.matterworks.common.io.SideConfigurationData;
import de.artemis.matterworks.common.menu.FluidTankMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import de.artemis.matterworks.common.world.PylonChunkLoading;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class FluidTankBlockEntity extends MatterPylonBlockEntity implements SideConfigurableBlockEntity {
    public static final int CRYSTAL_SLOT = 0;
    public static final int DRAIN_SLOT = 1;
    public static final int FILL_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    public static final int DATA_FLUID_AMOUNT = 0;
    public static final int DATA_FLUID_CAPACITY = 1;
    public static final int DATA_COUNT = 2;
    public static final int FLUID_CAPACITY = 32 * FluidType.BUCKET_VOLUME;
    public static final int MAX_SLOT_TRANSFER_PER_TICK = FluidType.BUCKET_VOLUME;
    public static final int MAX_SIDE_TRANSFER_PER_TICK = FluidType.BUCKET_VOLUME;
    private static final int HISTORY_SAMPLE_INTERVAL = 4;
    private static final int HISTORY_LENGTH = 120;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (getSlotRole(slot)) {
                case CRYSTAL -> PowerCrystalEffects.isPowerCrystal(stack);
                case DRAIN -> canDrainFromItem(stack);
                case FILL -> FluidItemHelper.isFluidItem(stack);
                case NONE -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private final FluidTank fluidTank = new FluidTank(FLUID_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            syncVisualState();
        }
    };
    private final SideConfigurationData sideConfiguration = new SideConfigurationData(SideAccessMode.BOTH, SideAccessMode.BOTH, SideAccessMode.DISABLED);
    private final IItemHandler inputAutomationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return isValidSlot(slot) ? itemHandler.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isValidSlot(slot) ? itemHandler.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return isValidSlot(slot) ? 1 : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidSlot(slot) && itemHandler.isItemValid(slot, stack);
        }
    };
    private final IItemHandler outputAutomationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return isValidSlot(slot) ? itemHandler.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isValidSlot(slot) ? itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return isValidSlot(slot) ? 1 : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
    private final IItemHandler[] configuredItemHandlers = createConfiguredItemHandlers();
    private final IFluidHandler[] configuredFluidHandlers = createConfiguredFluidHandlers();
    private final TransferHistorySample[] transferHistory = createEmptyHistory();
    private int historySize;
    private int currentInputRate;
    private int currentOutputRate;
    private int currentTransitRate;
    private int lastTickFluidAmount = Integer.MIN_VALUE;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FLUID_AMOUNT -> fluidTank.getFluidAmount();
                case DATA_FLUID_CAPACITY -> fluidTank.getCapacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FluidTankBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.FLUID_TANK.get(), pos, blockState);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, FluidTankBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getData() {
        return data;
    }

    public IFluidHandler getFluidStorage(@Nullable Direction side) {
        return side == null ? fluidTank : configuredFluidHandlers[side.ordinal()];
    }

    public IItemHandler getAutomationHandler(@Nullable Direction side) {
        return side == null ? itemHandler : configuredItemHandlers[side.ordinal()];
    }

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    public int getFluidCapacity() {
        return fluidTank.getCapacity();
    }

    public FluidStack getFluidStack() {
        return fluidTank.getFluid().copy();
    }

    public ItemStack getCrystalStack() {
        return itemHandler.getStackInSlot(CRYSTAL_SLOT).copy();
    }

    @Override
    protected void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        syncFluidCapacity();
        int externalNetRate = sampleExternalFluidDelta();
        int inputRate = Math.max(0, externalNetRate);
        int outputRate = Math.max(0, -externalNetRate);

        inputRate += transferFluidFromInputItem();
        outputRate += transferFluidToOutputItem();
        outputRate += pushFluidToNeighbors();
        super.serverTick();
        int transitRate = getTransferRoleAmount(CHANNEL_FLUIDS, TransferDisplayRole.TRANSIT);
        inputRate += getTransferRoleAmount(CHANNEL_FLUIDS, TransferDisplayRole.SINK);
        outputRate += getTransferRoleAmount(CHANNEL_FLUIDS, TransferDisplayRole.SOURCE);
        refreshTransferTelemetry(inputRate, outputRate, transitRate);
        lastTickFluidAmount = fluidTank.getFluidAmount();
    }

    public SimpleContainer createDropInventory() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            inventory.setItem(slot, itemHandler.getStackInSlot(slot).copy());
        }
        return inventory;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(ModBlocks.FLUID_TANK.get().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluidTankMenu(containerId, playerInventory, this, data);
    }

    @Override
    public boolean openPrimaryMenu(Player player, boolean remoteAccess) {
        player.openMenu(
                new SimpleMenuProvider((containerId, inventory, menuPlayer) -> new FluidTankMenu(containerId, inventory, this, data, remoteAccess), getDisplayName()),
                worldPosition
        );
        return true;
    }

    @Override
    protected boolean supportsChannel(int channel) {
        return channel == CHANNEL_FLUIDS;
    }

    @Override
    protected boolean canLinkTo(MatterPylonBlockEntity other) {
        return other.getType() == ModBlockEntities.MATTER_PYLON.get()
                || other.getType() == ModBlockEntities.MATTER_NETWORK_CONTROLLER.get()
                || other.getType() == ModBlockEntities.FLUID_TANK.get();
    }

    @Override
    protected @Nullable IFluidHandler getAttachedFluidHandler() {
        return fluidTank;
    }

    @Override
    protected Component getMatterNetworkMenuTitle() {
        return Component.translatable("screen.matterworks.matter_network.fluid_tank");
    }

    @Override
    protected void refreshChunkLoadingTickets(ServerLevel serverLevel) {
        PylonChunkLoading.forceNodeTickets(serverLevel, worldPosition);
    }

    @Override
    protected void releaseChunkLoadingTickets(ServerLevel serverLevel) {
        PylonChunkLoading.releaseNodeTickets(serverLevel, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.put("fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        sideConfiguration.writeToTag(tag);
        writeTransferTelemetryTag(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            loadInventory(tag.getCompound("inventory"), registries);
        }
        if (tag.contains("fluid")) {
            fluidTank.readFromNBT(registries, tag.getCompound("fluid"));
        }
        sideConfiguration.readFromTag(tag, this::sanitizeSideAccessMode);
        readTransferTelemetryTag(tag);
        syncFluidCapacity();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        sideConfiguration.writeToTag(tag);
        writeTransferTelemetryTag(tag);
        return tag;
    }

    @Override
    public boolean supportsSideConfigType(SideConfigType type) {
        return type == SideConfigType.ITEMS || type == SideConfigType.FLUIDS;
    }

    @Override
    public boolean supportsSideConfigInput(SideConfigType type) {
        return supportsSideConfigType(type);
    }

    @Override
    public boolean supportsSideConfigOutput(SideConfigType type) {
        return supportsSideConfigType(type);
    }

    @Override
    public SideAccessMode getSideAccessMode(SideConfigType type, Direction side) {
        return sideConfiguration.get(type, side);
    }

    @Override
    public void setSideAccessMode(SideConfigType type, Direction side, SideAccessMode mode) {
        if (!supportsSideConfigType(type)) {
            return;
        }
        if (sideConfiguration.set(type, side, sanitizeSideAccessMode(type, side, mode))) {
            setChanged();
            syncVisualState();
        }
    }

    public int getCurrentInputRate() {
        return currentInputRate;
    }

    public int getCurrentOutputRate() {
        return currentOutputRate;
    }

    public int getCurrentTransitRate() {
        return currentTransitRate;
    }

    public int getCurrentTotalTransferRate() {
        return currentInputRate + currentOutputRate + currentTransitRate;
    }

    public int getHistorySize() {
        return historySize;
    }

    public int getHistoryCapacity() {
        return HISTORY_LENGTH;
    }

    public TransferHistorySample getHistorySample(int index) {
        if (index < 0 || index >= historySize) {
            return TransferHistorySample.EMPTY;
        }
        return transferHistory[index];
    }

    private int transferFluidFromInputItem() {
        ItemStack stack = itemHandler.getStackInSlot(DRAIN_SLOT);
        IFluidHandlerItem itemFluid = FluidItemHelper.getFluidHandler(stack);
        if (itemFluid == null) {
            return 0;
        }

        FluidStack simulatedDrain = itemFluid.drain(MAX_SLOT_TRANSFER_PER_TICK, IFluidHandler.FluidAction.SIMULATE);
        if (simulatedDrain.isEmpty()) {
            return 0;
        }

        int moved = FluidItemHelper.transferFluid(itemFluid, fluidTank, simulatedDrain, MAX_SLOT_TRANSFER_PER_TICK);
        if (moved > 0) {
            itemHandler.setStackInSlot(DRAIN_SLOT, itemFluid.getContainer());
            setChanged();
        }
        return moved;
    }

    private int transferFluidToOutputItem() {
        ItemStack stack = itemHandler.getStackInSlot(FILL_SLOT);
        IFluidHandlerItem itemFluid = FluidItemHelper.getFluidHandler(stack);
        if (itemFluid == null || fluidTank.getFluidAmount() <= 0) {
            return 0;
        }

        FluidStack available = fluidTank.getFluid().copyWithAmount(Math.min(fluidTank.getFluidAmount(), MAX_SLOT_TRANSFER_PER_TICK));
        int moved = FluidItemHelper.transferFluid(fluidTank, itemFluid, available, MAX_SLOT_TRANSFER_PER_TICK);
        if (moved > 0) {
            itemHandler.setStackInSlot(FILL_SLOT, itemFluid.getContainer());
            setChanged();
        }
        return moved;
    }

    private int pushFluidToNeighbors() {
        int movedTotal = 0;
        for (Direction direction : Direction.values()) {
            if (fluidTank.getFluidAmount() <= 0) {
                break;
            }
            if (!getSideAccessMode(SideConfigType.FLUIDS, direction).allowsOutput()) {
                continue;
            }

            BlockPos targetPos = worldPosition.relative(direction);
            IFluidHandler targetHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, direction.getOpposite());
            if (targetHandler == null) {
                continue;
            }

            FluidStack available = fluidTank.getFluid().copyWithAmount(Math.min(fluidTank.getFluidAmount(), MAX_SIDE_TRANSFER_PER_TICK));
            int moved = FluidItemHelper.transferFluid(fluidTank, targetHandler, available, MAX_SIDE_TRANSFER_PER_TICK);
            if (moved > 0) {
                movedTotal += moved;
                setChanged();
            }
        }
        return movedTotal;
    }

    private int sampleExternalFluidDelta() {
        int fluidAmount = fluidTank.getFluidAmount();
        if (lastTickFluidAmount == Integer.MIN_VALUE) {
            lastTickFluidAmount = fluidAmount;
            return 0;
        }
        return fluidAmount - lastTickFluidAmount;
    }

    private void refreshTransferTelemetry(int inputRate, int outputRate, int transitRate) {
        currentInputRate = inputRate;
        currentOutputRate = outputRate;
        currentTransitRate = transitRate;

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean changed = false;
        if (serverLevel.getGameTime() % HISTORY_SAMPLE_INTERVAL == 0L) {
            TransferHistorySample sample = new TransferHistorySample(inputRate, outputRate, transitRate);
            if (historySize < HISTORY_LENGTH) {
                transferHistory[historySize] = sample;
                historySize++;
            } else {
                System.arraycopy(transferHistory, 1, transferHistory, 0, HISTORY_LENGTH - 1);
                transferHistory[HISTORY_LENGTH - 1] = sample;
            }
            changed = true;
        }

        if (changed) {
            setChanged();
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    private void writeTransferTelemetryTag(CompoundTag tag) {
        tag.putInt("current_input_rate", currentInputRate);
        tag.putInt("current_output_rate", currentOutputRate);
        tag.putInt("current_transit_rate", currentTransitRate);
        tag.putInt("history_size", historySize);

        ListTag historyTag = new ListTag();
        for (int index = 0; index < historySize; index++) {
            TransferHistorySample sample = transferHistory[index];
            CompoundTag sampleTag = new CompoundTag();
            sampleTag.putInt("input", sample.inputRate());
            sampleTag.putInt("output", sample.outputRate());
            sampleTag.putInt("transit", sample.transitRate());
            historyTag.add(sampleTag);
        }
        tag.put("transfer_history", historyTag);
    }

    private void readTransferTelemetryTag(CompoundTag tag) {
        currentInputRate = tag.getInt("current_input_rate");
        currentOutputRate = tag.getInt("current_output_rate");
        currentTransitRate = tag.getInt("current_transit_rate");

        clearHistory();
        ListTag historyTag = tag.getList("transfer_history", Tag.TAG_COMPOUND);
        int loadedSize = Math.min(HISTORY_LENGTH, historyTag.size());
        for (int index = 0; index < loadedSize; index++) {
            CompoundTag sampleTag = historyTag.getCompound(index);
            transferHistory[index] = new TransferHistorySample(
                    sampleTag.getInt("input"),
                    sampleTag.getInt("output"),
                    sampleTag.getInt("transit")
            );
        }
        historySize = Math.min(HISTORY_LENGTH, Math.max(tag.getInt("history_size"), loadedSize));
    }

    private void clearHistory() {
        for (int index = 0; index < HISTORY_LENGTH; index++) {
            transferHistory[index] = TransferHistorySample.EMPTY;
        }
        historySize = 0;
    }

    private void loadInventory(CompoundTag inventoryTag, HolderLookup.Provider registries) {
        int serializedSize = inventoryTag.contains("Size", Tag.TAG_INT) ? inventoryTag.getInt("Size") : SLOT_COUNT;
        ItemStackHandler serializedHandler = new ItemStackHandler(Math.max(1, serializedSize));
        serializedHandler.deserializeNBT(registries, inventoryTag);

        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }

        if (serializedSize <= 2) {
            copyIfPresent(serializedHandler, 0, DRAIN_SLOT);
            copyIfPresent(serializedHandler, 1, FILL_SLOT);
            return;
        }

        int slotsToCopy = Math.min(serializedHandler.getSlots(), itemHandler.getSlots());
        for (int slot = 0; slot < slotsToCopy; slot++) {
            itemHandler.setStackInSlot(slot, serializedHandler.getStackInSlot(slot));
        }
    }

    private void copyIfPresent(ItemStackHandler sourceHandler, int sourceSlot, int targetSlot) {
        if (sourceSlot < 0 || sourceSlot >= sourceHandler.getSlots() || targetSlot < 0 || targetSlot >= itemHandler.getSlots()) {
            return;
        }
        itemHandler.setStackInSlot(targetSlot, sourceHandler.getStackInSlot(sourceSlot));
    }

    private void syncFluidCapacity() {
        int targetCapacity = PowerCrystalEffects.getModifiedFluidCapacity(FLUID_CAPACITY, itemHandler.getStackInSlot(CRYSTAL_SLOT));
        if (fluidTank.getCapacity() == targetCapacity) {
            return;
        }

        fluidTank.setCapacity(targetCapacity);
        if (fluidTank.getFluidAmount() > targetCapacity) {
            fluidTank.setFluid(fluidTank.getFluid().copyWithAmount(targetCapacity));
        }
        setChanged();
        syncVisualState();
    }

    private static TransferHistorySample[] createEmptyHistory() {
        TransferHistorySample[] history = new TransferHistorySample[HISTORY_LENGTH];
        for (int index = 0; index < HISTORY_LENGTH; index++) {
            history[index] = TransferHistorySample.EMPTY;
        }
        return history;
    }

    private static boolean canDrainFromItem(ItemStack stack) {
        return !stack.isEmpty() && FluidItemHelper.canProvideFluid(stack);
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static SlotRole getSlotRole(int slot) {
        return switch (slot) {
            case CRYSTAL_SLOT -> SlotRole.CRYSTAL;
            case DRAIN_SLOT -> SlotRole.DRAIN;
            case FILL_SLOT -> SlotRole.FILL;
            default -> SlotRole.NONE;
        };
    }

    private SideAccessMode sanitizeSideAccessMode(SideConfigType type, Direction side, SideAccessMode requestedMode) {
        return supportsSideConfigType(type) ? requestedMode : SideAccessMode.DISABLED;
    }

    private IItemHandler[] createConfiguredItemHandlers() {
        IItemHandler[] handlers = new IItemHandler[Direction.values().length];
        for (Direction side : Direction.values()) {
            handlers[side.ordinal()] = new ConfiguredItemHandler(
                    () -> getSideAccessMode(SideConfigType.ITEMS, side),
                    () -> inputAutomationHandler,
                    () -> outputAutomationHandler
            );
        }
        return handlers;
    }

    private IFluidHandler[] createConfiguredFluidHandlers() {
        IFluidHandler[] handlers = new IFluidHandler[Direction.values().length];
        for (Direction side : Direction.values()) {
            handlers[side.ordinal()] = new ConfiguredFluidHandler(
                    () -> getSideAccessMode(SideConfigType.FLUIDS, side),
                    () -> fluidTank
            );
        }
        return handlers;
    }

    public record TransferHistorySample(int inputRate, int outputRate, int transitRate) {
        public static final TransferHistorySample EMPTY = new TransferHistorySample(0, 0, 0);

        public int totalRate() {
            return inputRate + outputRate + transitRate;
        }

        public int netRate() {
            return inputRate - outputRate;
        }
    }

    private enum SlotRole {
        CRYSTAL,
        DRAIN,
        FILL,
        NONE
    }
}
