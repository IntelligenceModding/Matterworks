package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockHelper;
import de.artemis.matterworks.common.multiblock.MultiblockPartEntity;
import de.artemis.matterworks.common.multiblock.MultiblockPartState;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.transport.PylonMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class MatterBatteryPortBlockEntity extends MatterPylonBlockEntity implements MultiblockPartEntity {
    private final MultiblockPartState multiblockPartState = new MultiblockPartState();
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

    private int maxTransfer = MatterBatteryCoreBlockEntity.BASE_TRANSFER_RATE;
    private long transferWindowTick = Long.MIN_VALUE;
    private long telemetrySampleTick = Long.MIN_VALUE;
    private int inputUsedThisTick;
    private int outputUsedThisTick;
    private int sampleInputAccum;
    private int sampleOutputAccum;
    private int lastInputRate;
    private int lastOutputRate;

    public MatterBatteryPortBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MULTIBLOCK_PORT.get(), pos, blockState);
        setMode(CHANNEL_ENERGY, PylonMode.IMPORT_EXPORT);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MatterBatteryPortBlockEntity blockEntity) {
        blockEntity.serverTick();
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

    public void configure(SideAccessMode newMode, int newMaxTransfer) {
        setMode(CHANNEL_ENERGY, toPylonMode(newMode));
        maxTransfer = Math.max(0, Math.min(newMaxTransfer, MatterBatteryCoreBlockEntity.BASE_TRANSFER_RATE));
        setChanged();
        syncVisualState();
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        multiblockPartState.readFromTag(tag);
        maxTransfer = Math.max(0, tag.getInt("max_transfer"));
        if (maxTransfer == 0 && !tag.contains("max_transfer")) {
            maxTransfer = MatterBatteryCoreBlockEntity.BASE_TRANSFER_RATE;
        }
        lastInputRate = Math.max(0, tag.getInt("last_input_rate"));
        lastOutputRate = Math.max(0, tag.getInt("last_output_rate"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        multiblockPartState.writeToTag(tag);
        tag.putInt("max_transfer", maxTransfer);
        tag.putInt("last_input_rate", lastInputRate);
        tag.putInt("last_output_rate", lastOutputRate);
        return tag;
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
        if (elapsed >= MatterBatteryCoreBlockEntity.HISTORY_SAMPLE_TICKS) {
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
        return MatterBatteryMultiblockHelper.getOutwardSide(multiblockPartState.getLocalPos(), multiblockPartState.getFront());
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
}
