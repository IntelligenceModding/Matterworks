package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.io.ConfiguredEnergyStorage;
import de.artemis.matterworks.common.io.ConfiguredItemHandler;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.io.SideConfigurableBlockEntity;
import de.artemis.matterworks.common.io.SideConfigurationData;
import de.artemis.matterworks.common.menu.EnergyCellMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import de.artemis.matterworks.common.world.PylonChunkLoading;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class EnergyCellBlockEntity extends MatterPylonBlockEntity implements SideConfigurableBlockEntity {
    public static final int SLOT_CRYSTAL = 0;
    public static final int CHARGE_SLOT_START = 1;
    public static final int CHARGE_SLOT_COUNT = 6;
    public static final int SLOT_POWER_BANK = CHARGE_SLOT_START + CHARGE_SLOT_COUNT;
    public static final int SLOT_COUNT = SLOT_POWER_BANK + 1;
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_CAPACITY = 1;
    public static final int DATA_COUNT = 2;
    public static final int ENERGY_CAPACITY = 200_000;
    public static final int MAX_SLOT_TRANSFER_PER_TICK = 2_000;
    public static final int MAX_SIDE_TRANSFER_PER_TICK = 2_000;
    public static final int MAX_INTERNAL_TRANSFER_PER_TICK = 8_000;
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
                case CHARGE -> isChargeItem(stack);
                case POWER_BANK -> isPowerBankInputItem(stack);
                case NONE -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private final CellEnergyStorage energyStorage = new CellEnergyStorage(ENERGY_CAPACITY, MAX_INTERNAL_TRANSFER_PER_TICK, MAX_INTERNAL_TRANSFER_PER_TICK);
    private final SideConfigurationData sideConfiguration = new SideConfigurationData(SideAccessMode.BOTH, SideAccessMode.DISABLED, SideAccessMode.BOTH);
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
    private final IEnergyStorage[] configuredEnergyHandlers = createConfiguredEnergyHandlers();
    private final TransferHistorySample[] transferHistory = createEmptyHistory();
    private int historySize;
    private int currentInputRate;
    private int currentOutputRate;
    private int currentTransitRate;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY -> energyStorage.getEnergyStored();
                case DATA_ENERGY_CAPACITY -> energyStorage.getMaxEnergyStored();
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

    public EnergyCellBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MATTER_ENERGY_CELL.get(), pos, blockState);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, EnergyCellBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getData() {
        return data;
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return side == null ? energyStorage : configuredEnergyHandlers[side.ordinal()];
    }

    public IItemHandler getAutomationHandler(@Nullable Direction side) {
        return side == null ? itemHandler : configuredItemHandlers[side.ordinal()];
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    protected void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        int inputRate = transferEnergyFromInputItems();
        int outputRate = transferEnergyToOutputItems();
        outputRate += pushEnergyToNeighbors();
        super.serverTick();
        int transitRate = getTransferRoleAmount(CHANNEL_ENERGY, TransferDisplayRole.TRANSIT);
        inputRate += getTransferRoleAmount(CHANNEL_ENERGY, TransferDisplayRole.SINK);
        outputRate += getTransferRoleAmount(CHANNEL_ENERGY, TransferDisplayRole.SOURCE);
        refreshTransferTelemetry(inputRate, outputRate, transitRate);
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
        return Component.translatable(ModBlocks.MATTER_ENERGY_CELL.get().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EnergyCellMenu(containerId, playerInventory, this, data);
    }

    @Override
    public boolean openPrimaryMenu(Player player, boolean remoteAccess) {
        player.openMenu(
                new SimpleMenuProvider((containerId, inventory, menuPlayer) -> new EnergyCellMenu(containerId, inventory, this, data, remoteAccess), getDisplayName()),
                worldPosition
        );
        return true;
    }

    @Override
    protected boolean supportsChannel(int channel) {
        return channel == CHANNEL_ENERGY;
    }

    @Override
    protected boolean canLinkTo(MatterPylonBlockEntity other) {
        return other.getType() == ModBlockEntities.MATTER_PYLON.get()
                || other.getType() == ModBlockEntities.MATTER_NETWORK_CONTROLLER.get()
                || other.getType() == ModBlockEntities.MATTER_ENERGY_CELL.get()
                || other.getType() == ModBlockEntities.MULTIBLOCK_PORT.get();
    }

    @Override
    protected @Nullable IEnergyStorage getAttachedEnergyStorage(boolean importer) {
        if (importer && !energyStorage.canReceive()) {
            return null;
        }
        if (!importer && !energyStorage.canExtract()) {
            return null;
        }
        return energyStorage;
    }

    @Override
    protected Component getMatterNetworkMenuTitle() {
        return Component.translatable("screen.matterworks.matter_network.energy_cell");
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
        tag.putInt("energy", energyStorage.getEnergyStored());
        sideConfiguration.writeToTag(tag);
        writeTransferTelemetryTag(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            loadInventory(tag.getCompound("inventory"), registries);
        }
        energyStorage.setStoredEnergy(tag.getInt("energy"));
        sideConfiguration.readFromTag(tag, this::sanitizeSideAccessMode);
        readTransferTelemetryTag(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        sideConfiguration.writeToTag(tag);
        writeTransferTelemetryTag(tag);
        return tag;
    }

    @Override
    public boolean supportsSideConfigType(SideConfigType type) {
        return type == SideConfigType.ITEMS || type == SideConfigType.ENERGY;
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

    private int transferEnergyFromInputItems() {
        ItemStack stack = itemHandler.getStackInSlot(SLOT_POWER_BANK);
        if (!isPowerBankInputItem(stack)) {
            return 0;
        }

        IEnergyStorage itemEnergy = EnergyItemHelper.getEnergyStorage(stack);
        if (itemEnergy == null) {
            return 0;
        }

        int moved = EnergyItemHelper.transferEnergy(itemEnergy, energyStorage, MAX_SLOT_TRANSFER_PER_TICK);
        if (moved > 0) {
            setChanged();
        }
        return moved;
    }

    private int transferEnergyToOutputItems() {
        int movedTotal = 0;
        for (int slot = CHARGE_SLOT_START; slot < CHARGE_SLOT_START + CHARGE_SLOT_COUNT; slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (!isChargeItem(stack)) {
                continue;
            }

            IEnergyStorage itemEnergy = EnergyItemHelper.getEnergyStorage(stack);
            if (itemEnergy == null) {
                continue;
            }

            int moved = EnergyItemHelper.transferEnergy(energyStorage, itemEnergy, MAX_SLOT_TRANSFER_PER_TICK);
            if (moved > 0) {
                movedTotal += moved;
                setChanged();
            }
        }
        return movedTotal;
    }

    private int pushEnergyToNeighbors() {
        int movedTotal = 0;
        for (Direction direction : Direction.values()) {
            int storedEnergy = energyStorage.getEnergyStored();
            if (storedEnergy <= 0) {
                break;
            }
            if (!getSideAccessMode(SideConfigType.ENERGY, direction).allowsOutput()) {
                continue;
            }

            BlockPos targetPos = worldPosition.relative(direction);
            IEnergyStorage targetStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, direction.getOpposite());
            if (targetStorage == null || !targetStorage.canReceive()) {
                continue;
            }

            if (targetStorage.getEnergyStored() >= targetStorage.getMaxEnergyStored()) {
                continue;
            }

            if (targetStorage.getEnergyStored() >= storedEnergy) {
                continue;
            }

            int offered = Math.min(MAX_SIDE_TRANSFER_PER_TICK, storedEnergy);
            int accepted = targetStorage.receiveEnergy(offered, false);
            if (accepted > 0) {
                energyStorage.extractEnergy(accepted, false);
                movedTotal += accepted;
                setChanged();
            }
        }
        return movedTotal;
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
            copyIfPresent(serializedHandler, 0, SLOT_POWER_BANK);
            copyIfPresent(serializedHandler, 1, CHARGE_SLOT_START);
            return;
        }

        if (serializedSize == 8) {
            migrateLegacyEightSlotInventory(serializedHandler);
            return;
        }

        int slotsToCopy = Math.min(serializedHandler.getSlots(), itemHandler.getSlots());
        for (int slot = 0; slot < slotsToCopy; slot++) {
            itemHandler.setStackInSlot(slot, serializedHandler.getStackInSlot(slot));
        }
    }

    private void migrateLegacyEightSlotInventory(ItemStackHandler sourceHandler) {
        int nextChargeSlot = CHARGE_SLOT_START;

        for (int sourceSlot = 4; sourceSlot < 8 && nextChargeSlot < CHARGE_SLOT_START + CHARGE_SLOT_COUNT; sourceSlot++) {
            ItemStack stack = sourceHandler.getStackInSlot(sourceSlot);
            if (isChargeItem(stack)) {
                itemHandler.setStackInSlot(nextChargeSlot++, stack.copy());
            }
        }

        for (int sourceSlot = 0; sourceSlot < 4; sourceSlot++) {
            ItemStack stack = sourceHandler.getStackInSlot(sourceSlot);
            if (stack.isEmpty()) {
                continue;
            }

            if (itemHandler.getStackInSlot(SLOT_POWER_BANK).isEmpty() && isPowerBankInputItem(stack)) {
                itemHandler.setStackInSlot(SLOT_POWER_BANK, stack.copy());
                continue;
            }

            if (nextChargeSlot < CHARGE_SLOT_START + CHARGE_SLOT_COUNT && isChargeItem(stack)) {
                itemHandler.setStackInSlot(nextChargeSlot++, stack.copy());
            }
        }
    }

    private void copyIfPresent(ItemStackHandler sourceHandler, int sourceSlot, int targetSlot) {
        if (sourceSlot < 0 || sourceSlot >= sourceHandler.getSlots() || targetSlot < 0 || targetSlot >= itemHandler.getSlots()) {
            return;
        }
        itemHandler.setStackInSlot(targetSlot, sourceHandler.getStackInSlot(sourceSlot));
    }

    private static TransferHistorySample[] createEmptyHistory() {
        TransferHistorySample[] history = new TransferHistorySample[HISTORY_LENGTH];
        for (int index = 0; index < HISTORY_LENGTH; index++) {
            history[index] = TransferHistorySample.EMPTY;
        }
        return history;
    }

    private static boolean isChargeItem(ItemStack stack) {
        return isUsableEnergyItem(stack) && EnergyItemHelper.canReceiveEnergy(stack);
    }

    private static boolean isPowerBankInputItem(ItemStack stack) {
        return isUsableEnergyItem(stack) && EnergyItemHelper.canProvideEnergy(stack);
    }

    private static boolean isUsableEnergyItem(ItemStack stack) {
        return !stack.isEmpty()
                && EnergyItemHelper.getEnergyStorage(stack) != null
                && !PowerCrystalEffects.isPowerCrystal(stack);
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static SlotRole getSlotRole(int slot) {
        if (slot == SLOT_CRYSTAL) {
            return SlotRole.CRYSTAL;
        }
        if (slot >= CHARGE_SLOT_START && slot < CHARGE_SLOT_START + CHARGE_SLOT_COUNT) {
            return SlotRole.CHARGE;
        }
        if (slot == SLOT_POWER_BANK) {
            return SlotRole.POWER_BANK;
        }
        return SlotRole.NONE;
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

    private IEnergyStorage[] createConfiguredEnergyHandlers() {
        IEnergyStorage[] handlers = new IEnergyStorage[Direction.values().length];
        for (Direction side : Direction.values()) {
            handlers[side.ordinal()] = new ConfiguredEnergyStorage(
                    () -> getSideAccessMode(SideConfigType.ENERGY, side),
                    () -> energyStorage
            );
        }
        return handlers;
    }

    private final class CellEnergyStorage extends EnergyStorage {
        private CellEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
            }
            return extracted;
        }

        private void setStoredEnergy(int amount) {
            this.energy = Mth.clamp(amount, 0, this.capacity);
            setChanged();
        }
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
        CHARGE,
        POWER_BANK,
        NONE
    }
}
