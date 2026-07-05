package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.menu.MatterBatteryCoreMenu;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockDefinition;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockHelper;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockLayout;
import de.artemis.matterworks.common.multiblock.MultiblockPartEntity;
import de.artemis.matterworks.common.multiblock.MultiblockPartState;
import de.artemis.matterworks.common.multiblock.MultiblockRole;
import de.artemis.matterworks.common.multiblock.MultiblockStructure;
import de.artemis.matterworks.common.multiblock.MultiblockStructureRegistry;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.transport.PylonMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MatterBatteryPortBlockEntity extends MatterPylonBlockEntity implements MultiblockPartEntity {
    public static final int CAPACITY_PER_CELL = 500_000;
    public static final int BASE_TRANSFER_RATE = 1_000_000;
    public static final int HISTORY_SIZE = 48;
    public static final int HISTORY_SAMPLE_TICKS = 10;
    public static final int DATA_FORMED = 0;
    public static final int DATA_ENERGY = 1;
    public static final int DATA_CAPACITY = 2;
    public static final int DATA_TRANSFER = 3;
    public static final int DATA_CELLS = 4;
    public static final int DATA_COUNT = 5;

    private final MultiblockPartState multiblockPartState = new MultiblockPartState();
    private final BatteryEnergyStorage batteryEnergyStorage = new BatteryEnergyStorage();
    private final IEnergyStorage networkEnergyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            MatterBatteryCoreBlockEntity controller = getController();
            if (controller == null || !getMode().allowsInput()) {
                return 0;
            }
            advanceTelemetryWindows();
            int accepted = controller.getEnergyStorage().receiveEnergy(Math.min(maxReceive, getRemainingInputBudget()), simulate);
            if (accepted > 0 && !simulate) {
                inputUsedThisTick += accepted;
                sampleInputAccum += accepted;
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            MatterBatteryCoreBlockEntity controller = getController();
            if (controller == null || !getMode().allowsOutput()) {
                return 0;
            }
            advanceTelemetryWindows();
            int extracted = controller.getEnergyStorage().extractEnergy(Math.min(maxExtract, getRemainingOutputBudget()), simulate);
            if (extracted > 0 && !simulate) {
                outputUsedThisTick += extracted;
                sampleOutputAccum += extracted;
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            MatterBatteryCoreBlockEntity controller = getController();
            return controller == null ? 0 : controller.getDisplayedEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            MatterBatteryCoreBlockEntity controller = getController();
            return controller == null ? 0 : controller.getDisplayedEnergyCapacity();
        }

        @Override
        public boolean canExtract() {
            MatterBatteryCoreBlockEntity controller = getController();
            return controller != null && getMode().allowsOutput() && controller.getEnergyStorage().canExtract();
        }

        @Override
        public boolean canReceive() {
            MatterBatteryCoreBlockEntity controller = getController();
            return controller != null && getMode().allowsInput() && controller.getEnergyStorage().canReceive();
        }
    };
    private final ContainerData batteryData = new ContainerData() {
        @Override
        public int get(int index) {
            MatterBatteryCoreBlockEntity controller = getController();
            if (controller == null) {
                return 0;
            }
            return switch (index) {
                case DATA_FORMED -> controller.isFormed() ? 1 : 0;
                case DATA_ENERGY -> controller.getDisplayedEnergyStored();
                case DATA_CAPACITY -> controller.getDisplayedEnergyCapacity();
                case DATA_TRANSFER -> controller.getTransferRate();
                case DATA_CELLS -> controller.getCellCount();
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

    private int maxTransfer = BASE_TRANSFER_RATE;
    private long transferWindowTick = Long.MIN_VALUE;
    private long telemetrySampleTick = Long.MIN_VALUE;
    private int inputUsedThisTick;
    private int outputUsedThisTick;
    private int sampleInputAccum;
    private int sampleOutputAccum;
    private int lastInputRate;
    private int lastOutputRate;

    private int storedEnergy;
    private int capacity;
    private int cellCount;
    private long lastRecoveryAttemptTick = Long.MIN_VALUE;
    private long batteryTransferWindowTick = Long.MIN_VALUE;
    private long lastHistorySampleTick = Long.MIN_VALUE;
    private int receivedThisTick;
    private int extractedThisTick;
    private int sampleReceivedAccum;
    private int sampleExtractedAccum;
    private int historyCursor;
    private int historySamples;
    private final int[] energyHistory = new int[HISTORY_SIZE];
    private final int[] inputHistory = new int[HISTORY_SIZE];
    private final int[] outputHistory = new int[HISTORY_SIZE];
    private List<PortOverview> portOverview = List.of();

    public MatterBatteryPortBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MULTIBLOCK_PORT.get(), pos, blockState);
        setMode(CHANNEL_ENERGY, PylonMode.IMPORT_EXPORT);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MatterBatteryPortBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel && isBatteryController()) {
            lastRecoveryAttemptTick = serverLevel.getGameTime() - 20L;
        }
    }

    @Override
    public MultiblockPartState getMultiblockPartState() {
        return multiblockPartState;
    }

    public @Nullable IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        Direction outwardSide = getOutwardSide();
        if (outwardSide == null) {
            return null;
        }
        return side == null || side == outwardSide ? networkEnergyStorage : null;
    }

    public void handleNetworkLinkUse(Player player) {
        handleLinkUse(player);
    }

    public ContainerData getData() {
        return batteryData;
    }

    public boolean isFormedBatteryController() {
        return false;
    }

    public boolean isBatteryController() {
        return false;
    }

    public int getDisplayedEnergyStored() {
        MatterBatteryCoreBlockEntity controller = getController();
        return controller == null ? 0 : controller.getDisplayedEnergyStored();
    }

    public int getDisplayedEnergyCapacity() {
        MatterBatteryCoreBlockEntity controller = getController();
        return controller == null ? 0 : controller.getDisplayedEnergyCapacity();
    }

    public int getTransferRate() {
        MatterBatteryCoreBlockEntity controller = getController();
        return controller == null ? 0 : controller.getTransferRate();
    }

    public int[] getOrderedEnergyHistory() {
        MatterBatteryCoreBlockEntity controller = getController();
        return controller == null ? new int[0] : controller.getOrderedEnergyHistory();
    }

    public int[] getOrderedInputHistory() {
        MatterBatteryCoreBlockEntity controller = getController();
        return controller == null ? new int[0] : controller.getOrderedInputHistory();
    }

    public int[] getOrderedOutputHistory() {
        MatterBatteryCoreBlockEntity controller = getController();
        return controller == null ? new int[0] : controller.getOrderedOutputHistory();
    }

    public int getLatestInputRate() {
        return lastInputRate;
    }

    public int getLatestOutputRate() {
        return lastOutputRate;
    }

    public List<MatterBatteryCoreBlockEntity.PortOverview> getPortOverview() {
        MatterBatteryCoreBlockEntity controller = getController();
        return controller == null ? List.of() : controller.getPortOverview();
    }

    public boolean isMenuStillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    public void configure(SideAccessMode newMode, int newMaxTransfer) {
        setMode(CHANNEL_ENERGY, toPylonMode(newMode));
        maxTransfer = Math.max(0, Math.min(newMaxTransfer, BASE_TRANSFER_RATE));
        setChanged();
        syncVisualState();
    }

    public void configurePort(BlockPos portPos, SideAccessMode mode, int newMaxTransfer) {
        MatterBatteryCoreBlockEntity controller = getController();
        if (!(level instanceof ServerLevel serverLevel) || controller == null || !controller.isFormed()) {
            return;
        }
        controller.configurePort(portPos, mode, newMaxTransfer);
    }

    public SideAccessMode getMode() {
        return fromPylonMode(getMode(CHANNEL_ENERGY));
    }

    public int getNetworkId() {
        return getPylonId(CHANNEL_ENERGY);
    }

    public int getMaxTransfer() {
        return maxTransfer;
    }

    public int getLastInputRate() {
        return lastInputRate;
    }

    public int getLastOutputRate() {
        return lastOutputRate;
    }

    @Override
    public boolean openPrimaryMenu(Player player, boolean remoteAccess) {
        return level instanceof ServerLevel serverLevel
                && MatterBatteryMultiblockHelper.tryOpenBatteryMenu(serverLevel, worldPosition, player);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        MatterBatteryCoreBlockEntity controller = getController();
        if (controller == null) {
            throw new IllegalStateException("Battery port menu requested without a battery core controller");
        }
        return new MatterBatteryCoreMenu(containerId, playerInventory, controller, controller.getData());
    }

    @Override
    public BlockPos getControllerTrackedPos() {
        MatterBatteryCoreBlockEntity controller = getController();
        return controller == null ? super.getControllerTrackedPos() : controller.getBlockPos();
    }

    @Override
    public String getControllerTrackedDisplayName() {
        MatterBatteryCoreBlockEntity controller = getController();
        return controller == null ? super.getControllerTrackedDisplayName() : controller.getDisplayName().getString();
    }

    @Override
    public void onMultiblockAssembled(MultiblockStructure structure, MultiblockRole role) {
    }

    @Override
    public void onMultiblockDisassembled(MultiblockStructure structure) {
    }

    @Override
    protected void serverTick() {
        advanceTelemetryWindows();
        super.serverTick();

    }

    @Override
    protected boolean supportsChannel(int channel) {
        return channel == CHANNEL_ENERGY;
    }

    @Override
    public boolean supportsUpgradeCrystals() {
        return false;
    }

    @Override
    protected boolean canLinkTo(MatterPylonBlockEntity other) {
        return other.getType() == ModBlockEntities.MATTER_PYLON.get()
                || other.getType() == ModBlockEntities.MATTER_NETWORK_CONTROLLER.get()
                || other.getType() == ModBlockEntities.MATTER_NETWORK_MONITOR.get()
                || other.getType() == ModBlockEntities.MATTER_ENERGY_CELL.get()
                || other.getType() == ModBlockEntities.MULTIBLOCK_PORT.get();
    }

    @Override
    protected @Nullable IEnergyStorage getAttachedEnergyStorage(boolean importer) {
        if (importer && !networkEnergyStorage.canReceive()) {
            return null;
        }
        if (!importer && !networkEnergyStorage.canExtract()) {
            return null;
        }
        return networkEnergyStorage;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(ModBlocks.MULTIBLOCK_PORT.get().getDescriptionId());
    }

    @Override
    protected Component getMatterNetworkMenuTitle() {
        return Component.literal("Battery Port Network");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        multiblockPartState.writeToTag(tag);
        tag.putInt("max_transfer", maxTransfer);
        tag.putInt("last_input_rate", lastInputRate);
        tag.putInt("last_output_rate", lastOutputRate);
        tag.putInt("stored_energy", storedEnergy);
        tag.putInt("capacity", capacity);
        tag.putInt("cell_count", cellCount);
        tag.putInt("history_cursor", historyCursor);
        tag.putInt("history_samples", historySamples);
        tag.putIntArray("energy_history", energyHistory);
        tag.putIntArray("input_history", inputHistory);
        tag.putIntArray("output_history", outputHistory);
        writePortOverview(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        multiblockPartState.readFromTag(tag);
        maxTransfer = Math.max(0, tag.getInt("max_transfer"));
        if (maxTransfer == 0 && !tag.contains("max_transfer")) {
            maxTransfer = BASE_TRANSFER_RATE;
        }
        lastInputRate = Math.max(0, tag.getInt("last_input_rate"));
        lastOutputRate = Math.max(0, tag.getInt("last_output_rate"));
        storedEnergy = Math.max(0, tag.getInt("stored_energy"));
        capacity = Math.max(0, tag.getInt("capacity"));
        cellCount = Math.max(0, tag.getInt("cell_count"));
        historyCursor = Math.max(0, Math.min(tag.getInt("history_cursor"), HISTORY_SIZE - 1));
        historySamples = Math.max(0, Math.min(tag.getInt("history_samples"), HISTORY_SIZE));
        copyIntoHistory(tag.getIntArray("energy_history"), energyHistory);
        copyIntoHistory(tag.getIntArray("input_history"), inputHistory);
        copyIntoHistory(tag.getIntArray("output_history"), outputHistory);
        readPortOverview(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        multiblockPartState.writeToTag(tag);
        tag.putInt("max_transfer", maxTransfer);
        tag.putInt("last_input_rate", lastInputRate);
        tag.putInt("last_output_rate", lastOutputRate);
        tag.putInt("stored_energy", storedEnergy);
        tag.putInt("capacity", capacity);
        tag.putInt("cell_count", cellCount);
        tag.putInt("history_cursor", historyCursor);
        tag.putInt("history_samples", historySamples);
        tag.putIntArray("energy_history", energyHistory);
        tag.putIntArray("input_history", inputHistory);
        tag.putIntArray("output_history", outputHistory);
        writePortOverview(tag);
        return tag;
    }

    public void refreshStructureStats(ServerLevel serverLevel, MultiblockStructure structure) {
        if (!worldPosition.equals(structure.controllerPos())) {
            return;
        }
        recalculateStats(serverLevel, structure);
        rebuildPortOverview(serverLevel, structure);
        syncVisualState();
    }

    private void recalculateStats(ServerLevel serverLevel, MultiblockStructure structure) {
        int cells = 0;
        for (BlockPos memberPos : structure.members().keySet()) {
            BlockState memberState = serverLevel.getBlockState(memberPos);
            if (memberState.is(ModBlocks.MATTER_CAPACITOR_CELL.get())) {
                cells++;
            }
        }

        cellCount = cells;
        capacity = cellCount * CAPACITY_PER_CELL;
        if (storedEnergy > capacity) {
            storedEnergy = capacity;
        }
        setChanged();
    }

    private void clearStructureStats() {
        capacity = 0;
        cellCount = 0;
        portOverview = List.of();
        historyCursor = 0;
        historySamples = 0;
        sampleReceivedAccum = 0;
        sampleExtractedAccum = 0;
        lastHistorySampleTick = Long.MIN_VALUE;
        java.util.Arrays.fill(energyHistory, 0);
        java.util.Arrays.fill(inputHistory, 0);
        java.util.Arrays.fill(outputHistory, 0);
        setChanged();
    }

    private int receiveBatteryEnergy(int maxReceive, boolean simulate) {
        if (!isFormedBatteryController() || capacity <= 0 || getTransferRate() <= 0 || maxReceive <= 0) {
            return 0;
        }
        syncBatteryTransferWindow();
        int availableTransfer = Math.max(0, getTransferRate() - receivedThisTick);
        int accepted = Math.min(Math.min(maxReceive, availableTransfer), capacity - storedEnergy);
        if (accepted > 0 && !simulate) {
            storedEnergy += accepted;
            receivedThisTick += accepted;
            sampleReceivedAccum += accepted;
            setChanged();
        }
        return accepted;
    }

    private int extractBatteryEnergy(int maxExtract, boolean simulate) {
        if (!isFormedBatteryController() || getTransferRate() <= 0 || maxExtract <= 0 || storedEnergy <= 0) {
            return 0;
        }
        syncBatteryTransferWindow();
        int availableTransfer = Math.max(0, getTransferRate() - extractedThisTick);
        int extracted = Math.min(Math.min(maxExtract, availableTransfer), storedEnergy);
        if (extracted > 0 && !simulate) {
            storedEnergy -= extracted;
            extractedThisTick += extracted;
            sampleExtractedAccum += extracted;
            setChanged();
        }
        return extracted;
    }

    private void syncBatteryTransferWindow() {
        if (level == null) {
            return;
        }
        long currentTick = level.getGameTime();
        if (currentTick != batteryTransferWindowTick) {
            batteryTransferWindowTick = currentTick;
            receivedThisTick = 0;
            extractedThisTick = 0;
        }
    }

    private void sampleBatteryTelemetry(ServerLevel serverLevel, MultiblockStructure structure) {
        long currentTick = serverLevel.getGameTime();
        int elapsed = lastHistorySampleTick == Long.MIN_VALUE ? HISTORY_SAMPLE_TICKS : (int) Math.max(1L, currentTick - lastHistorySampleTick);
        pushHistorySample(storedEnergy, sampleReceivedAccum / elapsed, sampleExtractedAccum / elapsed);
        sampleReceivedAccum = 0;
        sampleExtractedAccum = 0;
        lastHistorySampleTick = currentTick;
        rebuildPortOverview(serverLevel, structure);
        setChanged();
        syncVisualState();
    }

    private void resetBatteryTelemetry(long currentTick) {
        historyCursor = 0;
        historySamples = 0;
        sampleReceivedAccum = 0;
        sampleExtractedAccum = 0;
        lastHistorySampleTick = currentTick;
        java.util.Arrays.fill(energyHistory, 0);
        java.util.Arrays.fill(inputHistory, 0);
        java.util.Arrays.fill(outputHistory, 0);
        pushHistorySample(storedEnergy, 0, 0);
    }

    private void pushHistorySample(int energyValue, int inputValue, int outputValue) {
        energyHistory[historyCursor] = energyValue;
        inputHistory[historyCursor] = inputValue;
        outputHistory[historyCursor] = outputValue;
        historyCursor = (historyCursor + 1) % HISTORY_SIZE;
        if (historySamples < HISTORY_SIZE) {
            historySamples++;
        }
    }

    private void rebuildPortOverview(ServerLevel serverLevel, MultiblockStructure structure) {
        List<PortOverview> entries = new ArrayList<>();
        for (BlockPos memberPos : structure.members().keySet()) {
            if (!serverLevel.getBlockState(memberPos).is(ModBlocks.MULTIBLOCK_PORT.get())) {
                continue;
            }
            BlockEntity blockEntity = serverLevel.getBlockEntity(memberPos);
            if (!(blockEntity instanceof MatterBatteryPortBlockEntity portBlockEntity)) {
                continue;
            }
            Direction outwardSide = MatterBatteryMultiblockHelper.getOutwardSide(
                    portBlockEntity.getMultiblockPartState().getLocalPos(),
                    portBlockEntity.getMultiblockPartState().getFront(),
                    portBlockEntity.getMultiblockPartState().getWidth(),
                    portBlockEntity.getMultiblockPartState().getHeight(),
                    portBlockEntity.getMultiblockPartState().getDepth()
            );
            entries.add(new PortOverview(
                    memberPos.immutable(),
                    portBlockEntity.getDisplayName().getString(),
                    portBlockEntity.getNetworkId(),
                    portBlockEntity.getMode(),
                    portBlockEntity.getMaxTransfer(),
                    portBlockEntity.getLastInputRate(),
                    portBlockEntity.getLastOutputRate(),
                    outwardSide
            ));
        }
        entries.sort(Comparator.comparingLong(entry -> entry.pos().asLong()));
        portOverview = List.copyOf(entries);
    }

    private void writePortOverview(CompoundTag tag) {
        ListTag portsTag = new ListTag();
        for (PortOverview overview : portOverview) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("x", overview.pos().getX());
            entryTag.putInt("y", overview.pos().getY());
            entryTag.putInt("z", overview.pos().getZ());
            entryTag.putString("display_name", overview.displayName());
            entryTag.putInt("network_id", overview.networkId());
            entryTag.putString("mode", overview.mode().name());
            entryTag.putInt("max_transfer", overview.maxTransfer());
            entryTag.putInt("input_rate", overview.inputRate());
            entryTag.putInt("output_rate", overview.outputRate());
            entryTag.putString("outward_side", overview.outwardSide().getName());
            portsTag.add(entryTag);
        }
        tag.put("ports", portsTag);
    }

    private void readPortOverview(CompoundTag tag) {
        if (!tag.contains("ports", Tag.TAG_LIST)) {
            portOverview = List.of();
            return;
        }

        List<PortOverview> entries = new ArrayList<>();
        ListTag portsTag = tag.getList("ports", Tag.TAG_COMPOUND);
        for (int index = 0; index < portsTag.size(); index++) {
            CompoundTag entryTag = portsTag.getCompound(index);
            SideAccessMode mode;
            try {
                mode = SideAccessMode.valueOf(entryTag.getString("mode"));
            } catch (IllegalArgumentException ignored) {
                mode = SideAccessMode.BOTH;
            }
            Direction outwardSide = Direction.byName(entryTag.getString("outward_side"));
            if (outwardSide == null) {
                outwardSide = Direction.NORTH;
            }
            entries.add(new PortOverview(
                    new BlockPos(entryTag.getInt("x"), entryTag.getInt("y"), entryTag.getInt("z")),
                    entryTag.getString("display_name"),
                    Math.max(1, entryTag.getInt("network_id")),
                    mode,
                    Math.max(0, entryTag.getInt("max_transfer")),
                    Math.max(0, entryTag.getInt("input_rate")),
                    Math.max(0, entryTag.getInt("output_rate")),
                    outwardSide
            ));
        }
        portOverview = List.copyOf(entries);
    }

    private void copyIntoHistory(int[] source, int[] target) {
        java.util.Arrays.fill(target, 0);
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
    }

    private int[] copyOrderedHistory(int[] source) {
        int[] ordered = new int[historySamples];
        if (historySamples <= 0) {
            return ordered;
        }
        int start = historySamples == HISTORY_SIZE ? historyCursor : 0;
        for (int index = 0; index < historySamples; index++) {
            ordered[index] = source[(start + index) % HISTORY_SIZE];
        }
        return ordered;
    }

    private int getLatestHistoryIndex() {
        return (historyCursor - 1 + HISTORY_SIZE) % HISTORY_SIZE;
    }

    private void advanceTelemetryWindows() {
        if (level == null) {
            return;
        }

        long currentTick = level.getGameTime();
        if (currentTick != transferWindowTick) {
            transferWindowTick = currentTick;
            inputUsedThisTick = 0;
            outputUsedThisTick = 0;
        }

        if (telemetrySampleTick == Long.MIN_VALUE) {
            telemetrySampleTick = currentTick;
            return;
        }

        long elapsed = currentTick - telemetrySampleTick;
        if (elapsed >= HISTORY_SAMPLE_TICKS) {
            int sampleTicks = (int) Math.max(1L, elapsed);
            lastInputRate = sampleInputAccum / sampleTicks;
            lastOutputRate = sampleOutputAccum / sampleTicks;
            sampleInputAccum = 0;
            sampleOutputAccum = 0;
            telemetrySampleTick = currentTick;
            setChanged();
        }
    }

    private int getRemainingInputBudget() {
        return Math.max(0, maxTransfer - inputUsedThisTick);
    }

    private int getRemainingOutputBudget() {
        return Math.max(0, maxTransfer - outputUsedThisTick);
    }

    private @Nullable MatterBatteryCoreBlockEntity getController() {
        if (!multiblockPartState.isFormed() || level == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(multiblockPartState.getControllerPos());
        return blockEntity instanceof MatterBatteryCoreBlockEntity controller ? controller : null;
    }

    private @Nullable Direction getOutwardSide() {
        if (!multiblockPartState.isFormed()) {
            return null;
        }
        return MatterBatteryMultiblockHelper.getOutwardSide(multiblockPartState.getLocalPos(), multiblockPartState.getFront(), multiblockPartState.getWidth(), multiblockPartState.getHeight(), multiblockPartState.getDepth());
    }

    private static SideAccessMode fromPylonMode(PylonMode mode) {
        return switch (mode) {
            case DISABLED -> SideAccessMode.DISABLED;
            case EXPORT -> SideAccessMode.OUTPUT;
            case IMPORT -> SideAccessMode.INPUT;
            case IMPORT_EXPORT -> SideAccessMode.BOTH;
        };
    }

    private static PylonMode toPylonMode(SideAccessMode mode) {
        if (mode == null) {
            return PylonMode.IMPORT_EXPORT;
        }
        return switch (mode) {
            case DISABLED -> PylonMode.DISABLED;
            case INPUT -> PylonMode.IMPORT;
            case OUTPUT -> PylonMode.EXPORT;
            case BOTH -> PylonMode.IMPORT_EXPORT;
        };
    }

    public record PortOverview(BlockPos pos, String displayName, int networkId, SideAccessMode mode, int maxTransfer, int inputRate, int outputRate, Direction outwardSide) {
    }

    public final class BatteryEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return receiveBatteryEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return extractBatteryEnergy(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return getDisplayedEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return getDisplayedEnergyCapacity();
        }

        @Override
        public boolean canExtract() {
            return isFormedBatteryController() && getTransferRate() > 0 && storedEnergy > 0;
        }

        @Override
        public boolean canReceive() {
            return isFormedBatteryController() && getTransferRate() > 0 && storedEnergy < capacity;
        }
    }
}
