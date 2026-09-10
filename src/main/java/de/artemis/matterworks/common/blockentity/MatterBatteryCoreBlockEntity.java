package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.client.render.MatterBatteryFormationOverlayState;
import de.artemis.matterworks.common.block.MatterBatteryCoreBlock;
import de.artemis.matterworks.common.block.MatterCapacitorCellBlock;
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
import de.artemis.matterworks.common.network.ShowMatterBatteryFormationPayload;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class MatterBatteryCoreBlockEntity extends BlockEntity implements MenuProvider, CustomNamedBlockEntity, MultiblockPartEntity {
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
    private static final long RECOVERY_RETRY_TICKS = 5L;

    private final MultiblockPartState multiblockPartState = new MultiblockPartState();
    private final BatteryEnergyStorage energyStorage = new BatteryEnergyStorage();
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FORMED -> isFormed() ? 1 : 0;
                case DATA_ENERGY -> getDisplayedEnergyStored();
                case DATA_CAPACITY -> getDisplayedEnergyCapacity();
                case DATA_TRANSFER -> getTransferRate();
                case DATA_CELLS -> cellCount;
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

    private String customName = "";
    private boolean hasStoredBlueprint;
    private BlockPos storedOriginPos = BlockPos.ZERO;
    private Direction storedFront = Direction.NORTH;
    private int storedWidth;
    private int storedHeight;
    private int storedDepth;
    private int storedEnergy;
    private int capacity;
    private int maxTransfer;
    private int cellCount;
    private long lastRecoveryAttemptTick = Long.MIN_VALUE;
    private long transferWindowTick = Long.MIN_VALUE;
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

    public MatterBatteryCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MATTER_BATTERY_CORE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MatterBatteryCoreBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel && (multiblockPartState.isFormed() || hasStoredBlueprint)) {
            lastRecoveryAttemptTick = serverLevel.getGameTime() - RECOVERY_RETRY_TICKS;
        }
    }

    public boolean isFormed() {
        return multiblockPartState.isFormed();
    }

    public int getDisplayedEnergyStored() {
        return isFormed() ? storedEnergy : 0;
    }

    public boolean hasStoredBlueprint() {
        return hasStoredBlueprint && MatterBatteryMultiblockLayout.isValidSize(storedWidth, storedHeight, storedDepth);
    }

    public int getDisplayedEnergyCapacity() {
        return isFormed() ? capacity : 0;
    }

    public int getTransferRate() {
        return isFormed() ? maxTransfer : 0;
    }

    public int getCellCount() {
        return cellCount;
    }

    public BatteryEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public ContainerData getData() {
        return data;
    }

    public int[] getOrderedEnergyHistory() {
        return copyOrderedHistory(energyHistory);
    }

    public int[] getOrderedInputHistory() {
        return copyOrderedHistory(inputHistory);
    }

    public int[] getOrderedOutputHistory() {
        return copyOrderedHistory(outputHistory);
    }

    public int getLatestInputRate() {
        return historySamples <= 0 ? 0 : inputHistory[getLatestHistoryIndex()];
    }

    public int getLatestOutputRate() {
        return historySamples <= 0 ? 0 : outputHistory[getLatestHistoryIndex()];
    }

    public List<PortOverview> getPortOverview() {
        return portOverview;
    }

    @Override
    public MultiblockPartState getMultiblockPartState() {
        return multiblockPartState;
    }

    @Override
    public Component getDisplayName() {
        return customName.isEmpty() ? getDefaultName() : Component.literal(customName);
    }

    @Override
    public String getCustomNameText() {
        return customName;
    }

    @Override
    public void setCustomNameText(String customName) {
        String normalized = normalizeCustomName(customName);
        if (Objects.equals(this.customName, normalized)) {
            return;
        }
        this.customName = normalized;
        setChanged();
        sync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        multiblockPartState.writeToTag(tag);
        if (!customName.isEmpty()) {
            tag.putString("custom_name", customName);
        }
        tag.putInt("stored_energy", storedEnergy);
        tag.putBoolean("has_stored_blueprint", hasStoredBlueprint);
        tag.putInt("stored_origin_x", storedOriginPos.getX());
        tag.putInt("stored_origin_y", storedOriginPos.getY());
        tag.putInt("stored_origin_z", storedOriginPos.getZ());
        tag.putString("stored_front", storedFront.getName());
        tag.putInt("stored_width", storedWidth);
        tag.putInt("stored_height", storedHeight);
        tag.putInt("stored_depth", storedDepth);
        tag.putInt("capacity", capacity);
        tag.putInt("max_transfer", maxTransfer);
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
        customName = normalizeCustomName(tag.getString("custom_name"));
        storedEnergy = Math.max(0, tag.getInt("stored_energy"));
        hasStoredBlueprint = tag.getBoolean("has_stored_blueprint");
        storedOriginPos = new BlockPos(tag.getInt("stored_origin_x"), tag.getInt("stored_origin_y"), tag.getInt("stored_origin_z"));
        storedFront = Direction.byName(tag.getString("stored_front"));
        if (storedFront == null || !storedFront.getAxis().isHorizontal()) {
            storedFront = Direction.NORTH;
        }
        storedWidth = Math.max(0, tag.getInt("stored_width"));
        storedHeight = Math.max(0, tag.getInt("stored_height"));
        storedDepth = Math.max(0, tag.getInt("stored_depth"));
        capacity = Math.max(0, tag.getInt("capacity"));
        maxTransfer = Math.max(0, tag.getInt("max_transfer"));
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
        if (!customName.isEmpty()) {
            tag.putString("custom_name", customName);
        }
        tag.putInt("stored_energy", storedEnergy);
        tag.putBoolean("has_stored_blueprint", hasStoredBlueprint);
        tag.putInt("stored_origin_x", storedOriginPos.getX());
        tag.putInt("stored_origin_y", storedOriginPos.getY());
        tag.putInt("stored_origin_z", storedOriginPos.getZ());
        tag.putString("stored_front", storedFront.getName());
        tag.putInt("stored_width", storedWidth);
        tag.putInt("stored_height", storedHeight);
        tag.putInt("stored_depth", storedDepth);
        tag.putInt("capacity", capacity);
        tag.putInt("max_transfer", maxTransfer);
        tag.putInt("cell_count", cellCount);
        tag.putInt("history_cursor", historyCursor);
        tag.putInt("history_samples", historySamples);
        tag.putIntArray("energy_history", energyHistory);
        tag.putIntArray("input_history", inputHistory);
        tag.putIntArray("output_history", outputHistory);
        writePortOverview(tag);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MatterBatteryCoreMenu(containerId, playerInventory, this, data);
    }

    @Override
    public void onMultiblockAssembled(MultiblockStructure structure, MultiblockRole role) {
        if (level instanceof ServerLevel serverLevel && role == MultiblockRole.CONTROLLER) {
            storeBlueprint(structure);
            recalculateStats(serverLevel, structure);
            updateInternalVisualStates(serverLevel, structure, true);
            resetTelemetry(serverLevel.getGameTime());
            rebuildPortOverview(serverLevel, structure);
            broadcastFormationOverlay(serverLevel, structure, MatterBatteryFormationOverlayState.PulseType.FORMED);
            sync();
        }
    }

    @Override
    public void onMultiblockDisassembled(MultiblockStructure structure) {
        if (worldPosition.equals(structure.controllerPos())) {
            storeBlueprint(structure);
            if (level instanceof ServerLevel serverLevel) {
                updateInternalVisualStates(serverLevel, structure, false);
                broadcastFormationOverlay(serverLevel, structure, MatterBatteryFormationOverlayState.PulseType.BROKEN);
            }
        }
        clearTransientStructureState();
        sync();
    }

    public boolean tryAssemble(@Nullable Player player, boolean notifyFailure) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (MultiblockStructureRegistry.getByMember(serverLevel, worldPosition).isPresent()) {
            return true;
        }
        if (hasStoredBlueprint() && tryRecoverStoredStructure(serverLevel)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.matterworks.matter_battery.assembled"), true);
            }
            return true;
        }

        Direction front = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        var validation = MultiblockStructureRegistry.validate(serverLevel, worldPosition, front, MatterBatteryMultiblockDefinition.INSTANCE);
        if (!validation.success() || validation.match() == null) {
            if (notifyFailure && player != null) {
                player.displayClientMessage(Component.translatable("message.matterworks.matter_battery.invalid", validation.message()), true);
            }
            return false;
        }

        boolean assembled = MultiblockStructureRegistry.assemble(serverLevel, validation.match()).isPresent();
        if (assembled && player != null) {
            player.displayClientMessage(Component.translatable("message.matterworks.matter_battery.assembled"), true);
        }
        return assembled;
    }

    public void disassemble(@Nullable Player player) {
        if (level instanceof ServerLevel serverLevel && MultiblockStructureRegistry.disassembleByMember(serverLevel, worldPosition) && player != null) {
            player.displayClientMessage(Component.translatable("message.matterworks.matter_battery.disassembled"), true);
        }
    }

    public boolean isMenuStillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    public void configurePort(BlockPos portPos, SideAccessMode mode, int newMaxTransfer) {
        if (!(level instanceof ServerLevel serverLevel) || !isFormed()) {
            return;
        }
        var structureOptional = MultiblockStructureRegistry.getByMember(serverLevel, worldPosition);
        if (structureOptional.isEmpty() || !structureOptional.get().contains(portPos)) {
            return;
        }
        BlockEntity blockEntity = serverLevel.getBlockEntity(portPos);
        if (blockEntity instanceof MatterBatteryPortBlockEntity portBlockEntity) {
            portBlockEntity.configure(mode, newMaxTransfer);
            rebuildPortOverview(serverLevel, structureOptional.get());
            sync();
        }
    }

    private void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (shouldAttemptRecovery(serverLevel)) {
            lastRecoveryAttemptTick = serverLevel.getGameTime();
            tryRecoverStoredStructure(serverLevel);
        }

        if (!multiblockPartState.isFormed()) {
            return;
        }

        var structureOptional = MultiblockStructureRegistry.getByMember(serverLevel, worldPosition);
        if (structureOptional.isPresent() && serverLevel.getGameTime() - lastHistorySampleTick >= HISTORY_SAMPLE_TICKS) {
            sampleTelemetry(serverLevel, structureOptional.get());
        }
    }

    private void recalculateStats(ServerLevel serverLevel, MultiblockStructure structure) {
        int cells = 1;
        for (BlockPos memberPos : structure.members().keySet()) {
            BlockState memberState = serverLevel.getBlockState(memberPos);
            if (memberState.is(ModBlocks.MATTER_CAPACITOR_CELL.get())) {
                cells++;
            }
        }

        cellCount = cells;
        capacity = cellCount * CAPACITY_PER_CELL;
        maxTransfer = capacity > 0 ? BASE_TRANSFER_RATE : 0;
        if (storedEnergy > capacity) {
            storedEnergy = capacity;
        }
        setChanged();
    }

    private void clearTransientStructureState() {
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

    private void storeBlueprint(MultiblockStructure structure) {
        hasStoredBlueprint = true;
        storedOriginPos = structure.originPos().immutable();
        storedFront = structure.front();
        storedWidth = structure.width();
        storedHeight = structure.height();
        storedDepth = structure.depth();
        setChanged();
    }

    private boolean shouldAttemptRecovery(ServerLevel serverLevel) {
        if (!hasStoredBlueprint || !MatterBatteryMultiblockLayout.isValidSize(storedWidth, storedHeight, storedDepth)) {
            return false;
        }
        if (MultiblockStructureRegistry.getByMember(serverLevel, worldPosition).isPresent()) {
            return false;
        }
        return serverLevel.getGameTime() - lastRecoveryAttemptTick >= RECOVERY_RETRY_TICKS;
    }

    public boolean tryRecoverStoredStructure(ServerLevel serverLevel) {
        return MatterBatteryMultiblockHelper.recoverStructure(serverLevel, storedOriginPos, storedFront, storedWidth, storedHeight, storedDepth);
    }

    public boolean usesStoredBlueprintPosition(BlockPos pos) {
        if (!hasStoredBlueprint()) {
            return false;
        }
        MatterBatteryMultiblockLayout.WorldBounds bounds = MatterBatteryMultiblockLayout.getWorldBounds(
                storedOriginPos, storedFront, storedWidth, storedHeight, storedDepth
        );
        BlockPos minPos = bounds.minPos();
        BlockPos maxPos = minPos.offset(bounds.sizeX() - 1, bounds.sizeY() - 1, bounds.sizeZ() - 1);
        return pos.getX() >= minPos.getX() && pos.getX() <= maxPos.getX()
                && pos.getY() >= minPos.getY() && pos.getY() <= maxPos.getY()
                && pos.getZ() >= minPos.getZ() && pos.getZ() <= maxPos.getZ();
    }

    public @Nullable MultiblockStructure createStoredBlueprintStructure() {
        if (!hasStoredBlueprint()) {
            return null;
        }
        return MatterBatteryMultiblockHelper.createStructure(
                java.util.UUID.randomUUID(),
                worldPosition,
                storedOriginPos,
                storedFront,
                storedWidth,
                storedHeight,
                storedDepth
        );
    }

    private void updateInternalVisualStates(ServerLevel serverLevel, MultiblockStructure structure, boolean formed) {
        for (var entry : structure.members().entrySet()) {
            MultiblockRole role = entry.getValue();
            if (role != MultiblockRole.CONTROLLER && role != MultiblockRole.INTERNAL) {
                continue;
            }

            BlockPos memberPos = entry.getKey();
            BlockState currentState = serverLevel.getBlockState(memberPos);
            BlockState updatedState = currentState;
            if (currentState.hasProperty(MatterBatteryCoreBlock.FORMED)) {
                updatedState = updatedState.setValue(MatterBatteryCoreBlock.FORMED, formed);
            }
            if (currentState.hasProperty(MatterCapacitorCellBlock.FORMED)) {
                updatedState = updatedState.setValue(MatterCapacitorCellBlock.FORMED, formed);
            }
            if (updatedState != currentState) {
                serverLevel.setBlock(memberPos, updatedState, 3);
            }
        }
    }

    public void refreshStructureStats(ServerLevel serverLevel, MultiblockStructure structure) {
        if (!worldPosition.equals(structure.controllerPos())) {
            return;
        }
        recalculateStats(serverLevel, structure);
        updateInternalVisualStates(serverLevel, structure, true);
        rebuildPortOverview(serverLevel, structure);
        sync();
    }

    private int receiveEnergy(int maxReceive, boolean simulate) {
        if (!isFormed() || capacity <= 0 || maxTransfer <= 0 || maxReceive <= 0) {
            return 0;
        }
        syncTransferWindow();
        int availableTransfer = Math.max(0, maxTransfer - receivedThisTick);
        int accepted = Math.min(Math.min(maxReceive, availableTransfer), capacity - storedEnergy);
        if (accepted > 0 && !simulate) {
            storedEnergy += accepted;
            receivedThisTick += accepted;
            sampleReceivedAccum += accepted;
            setChanged();
        }
        return accepted;
    }

    private int extractEnergy(int maxExtract, boolean simulate) {
        if (!isFormed() || maxTransfer <= 0 || maxExtract <= 0 || storedEnergy <= 0) {
            return 0;
        }
        syncTransferWindow();
        int availableTransfer = Math.max(0, maxTransfer - extractedThisTick);
        int extracted = Math.min(Math.min(maxExtract, availableTransfer), storedEnergy);
        if (extracted > 0 && !simulate) {
            storedEnergy -= extracted;
            extractedThisTick += extracted;
            sampleExtractedAccum += extracted;
            setChanged();
        }
        return extracted;
    }

    private void syncTransferWindow() {
        if (level == null) {
            return;
        }
        long currentTick = level.getGameTime();
        if (currentTick != transferWindowTick) {
            transferWindowTick = currentTick;
            receivedThisTick = 0;
            extractedThisTick = 0;
        }
    }

    private void sampleTelemetry(ServerLevel serverLevel, MultiblockStructure structure) {
        long currentTick = serverLevel.getGameTime();
        int elapsed = lastHistorySampleTick == Long.MIN_VALUE ? HISTORY_SAMPLE_TICKS : (int) Math.max(1L, currentTick - lastHistorySampleTick);
        pushHistorySample(storedEnergy, sampleReceivedAccum / elapsed, sampleExtractedAccum / elapsed);
        sampleReceivedAccum = 0;
        sampleExtractedAccum = 0;
        lastHistorySampleTick = currentTick;
        rebuildPortOverview(serverLevel, structure);
        setChanged();
        sync();
    }

    private void resetTelemetry(long currentTick) {
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

    private void sync() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void broadcastFormationOverlay(ServerLevel serverLevel, MultiblockStructure structure, MatterBatteryFormationOverlayState.PulseType pulseType) {
        MatterBatteryMultiblockLayout.WorldBounds bounds = MatterBatteryMultiblockLayout.getWorldBounds(
                structure.originPos(), structure.front(), structure.width(), structure.height(), structure.depth()
        );
        double centerX = bounds.minPos().getX() + (bounds.sizeX() / 2.0D);
        double centerY = bounds.minPos().getY() + (bounds.sizeY() / 2.0D);
        double centerZ = bounds.minPos().getZ() + (bounds.sizeZ() / 2.0D);
        double maxDistanceSqr = 96.0D * 96.0D;
        ShowMatterBatteryFormationPayload payload = new ShowMatterBatteryFormationPayload(
                bounds.minPos(), bounds.sizeX(), bounds.sizeY(), bounds.sizeZ(), 100, pulseType.ordinal()
        );
        for (var player : serverLevel.players()) {
            if (player.distanceToSqr(centerX, centerY, centerZ) <= maxDistanceSqr) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    private Component getDefaultName() {
        return Component.translatable(ModBlocks.MATTER_BATTERY_CORE.get().getDescriptionId());
    }

    private static String normalizeCustomName(String customName) {
        String normalized = customName.strip();
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    public record PortOverview(BlockPos pos, String displayName, int networkId, SideAccessMode mode, int maxTransfer, int inputRate, int outputRate, Direction outwardSide) {
    }

    public final class BatteryEnergyStorage implements net.neoforged.neoforge.energy.IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return MatterBatteryCoreBlockEntity.this.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return MatterBatteryCoreBlockEntity.this.extractEnergy(maxExtract, simulate);
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
            return isFormed() && maxTransfer > 0 && storedEnergy > 0;
        }

        @Override
        public boolean canReceive() {
            return isFormed() && maxTransfer > 0 && storedEnergy < capacity;
        }
    }
}
