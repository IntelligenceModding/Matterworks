package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.block.MultiblockPortBlock;
import de.artemis.matterworks.common.menu.MatterBatteryCoreMenu;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockHelper;
import de.artemis.matterworks.common.multiblock.MultiblockPartEntity;
import de.artemis.matterworks.common.multiblock.MultiblockPartState;
import de.artemis.matterworks.common.multiblock.MultiblockRole;
import de.artemis.matterworks.common.multiblock.MultiblockStructure;
import de.artemis.matterworks.common.network.SetMultiblockPortColorPayload;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class MultiblockPortBlockEntity extends MatterPylonBlockEntity implements MultiblockPartEntity {
    private static final String TAG_PORT_COLOR_ID = "port_color_id";

    private final MultiblockPartState multiblockPartState = new MultiblockPartState();
    private final IEnergyStorage networkEnergyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            MatterBatteryCoreBlockEntity controller = getController();
            if (controller == null) {
                return 0;
            }
            advanceTelemetryWindows();
            int accepted = controller.getEnergyStorage().receiveEnergy(maxReceive, simulate);
            if (accepted > 0 && !simulate) {
                sampleInputAccum += accepted;
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            MatterBatteryCoreBlockEntity controller = getController();
            if (controller == null) {
                return 0;
            }
            advanceTelemetryWindows();
            int extracted = controller.getEnergyStorage().extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
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
            return controller != null && controller.getEnergyStorage().canExtract();
        }

        @Override
        public boolean canReceive() {
            MatterBatteryCoreBlockEntity controller = getController();
            return controller != null && controller.getEnergyStorage().canReceive();
        }
    };

    private long transferWindowTick = Long.MIN_VALUE;
    private long telemetrySampleTick = Long.MIN_VALUE;
    private int sampleInputAccum;
    private int sampleOutputAccum;
    private int lastInputRate;
    private int lastOutputRate;
    private DyeColor portColor = DyeColor.WHITE;

    public MultiblockPortBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MULTIBLOCK_PORT.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MultiblockPortBlockEntity blockEntity) {
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
        if (isFunctionalPort()) {
            handleLinkUse(player);
        }
    }

    public boolean isFunctionalPort() {
        MatterBatteryCoreBlockEntity controller = getController();
        return controller != null && controller.isFormed();
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

    public DyeColor getPortColor() {
        return portColor;
    }

    public int getPortColorId() {
        return portColor.getId();
    }

    public boolean setPortColor(DyeColor color) {
        DyeColor sanitized = color == null ? DyeColor.WHITE : color;
        if (portColor == sanitized) {
            return false;
        }

        portColor = sanitized;
        setChanged();
        syncBlockStateColor();
        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersTrackingChunk(
                    serverLevel,
                    new net.minecraft.world.level.ChunkPos(worldPosition),
                    new SetMultiblockPortColorPayload(worldPosition, portColor.getId())
            );
        }
        refreshControllerPortOverview();
        return true;
    }

    public static ItemStack createColoredPortStack(DyeColor color) {
        ItemStack stack = new ItemStack(ModBlocks.MULTIBLOCK_PORT.get());
        applyColorToStack(stack, color);
        return stack;
    }

    public static void applyColorToStack(ItemStack stack, DyeColor color) {
        if (stack.isEmpty() || !stack.is(ModBlocks.MULTIBLOCK_PORT.get().asItem())) {
            return;
        }
        DyeColor sanitized = color == null ? DyeColor.WHITE : color;
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_PORT_COLOR_ID, sanitized.getId());
        BlockItem.setBlockEntityData(stack, ModBlockEntities.MULTIBLOCK_PORT.get(), tag);
        stack.set(DataComponents.ITEM_NAME, getPortDisplayName(sanitized));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(sanitized.getId()));
    }

    public static boolean hasPortColor(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModBlocks.MULTIBLOCK_PORT.get().asItem())) {
            return false;
        }
        CustomData customData = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        return !customData.isEmpty() && customData.copyTag().contains(TAG_PORT_COLOR_ID);
    }

    public static DyeColor getPortColor(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModBlocks.MULTIBLOCK_PORT.get().asItem())) {
            return DyeColor.WHITE;
        }
        CustomData customData = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (customData.isEmpty()) {
            return DyeColor.WHITE;
        }
        CompoundTag tag = customData.copyTag();
        return tag.contains(TAG_PORT_COLOR_ID) ? DyeColor.byId(tag.getInt(TAG_PORT_COLOR_ID)) : DyeColor.WHITE;
    }

    public void applySyncedPortColor(DyeColor color) {
        DyeColor sanitized = color == null ? DyeColor.WHITE : color;
        if (portColor == sanitized) {
            refreshClientRender();
            return;
        }
        portColor = sanitized;
        refreshClientRender();
    }

    public boolean isMenuStillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && isFunctionalPort()
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    public int getNetworkId() {
        return getPylonId(CHANNEL_ENERGY);
    }

    public int getLastInputRate() {
        return lastInputRate;
    }

    public int getLastOutputRate() {
        return lastOutputRate;
    }

    @Override
    public void openMatterNetworkMenu(Player player, boolean remoteAccess) {
        if (isFunctionalPort()) {
            super.openMatterNetworkMenu(player, remoteAccess);
        }
    }

    @Override
    public boolean openPrimaryMenu(Player player, boolean remoteAccess) {
        return isFunctionalPort()
                && level instanceof ServerLevel serverLevel
                && MatterBatteryMultiblockHelper.tryOpenBatteryMenu(serverLevel, worldPosition, player);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        MatterBatteryCoreBlockEntity controller = getController();
        if (controller == null) {
            throw new IllegalStateException("Multiblock port menu requested without a battery core controller");
        }
        return new MatterBatteryCoreMenu(containerId, playerInventory, controller, controller.getData(), worldPosition);
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
        markNetworkDirty();
        syncVisualState();
    }

    @Override
    public void onMultiblockDisassembled(MultiblockStructure structure) {
        markNetworkDirty();
        syncVisualState();
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
        return isFunctionalPort() && other.supportsChannel(CHANNEL_ENERGY);
    }

    @Override
    protected Set<BlockPos> getTraversalLinkedPositions() {
        return isFunctionalPort() ? super.getTraversalLinkedPositions() : Set.of();
    }

    @Override
    protected boolean hasTraversalLinkTo(BlockPos pos) {
        return isFunctionalPort() && super.hasTraversalLinkTo(pos);
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
        return Component.literal("Multiblock Port Network");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        multiblockPartState.writeToTag(tag);
        tag.putInt("last_input_rate", lastInputRate);
        tag.putInt("last_output_rate", lastOutputRate);
        tag.putInt(TAG_PORT_COLOR_ID, portColor.getId());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        multiblockPartState.readFromTag(tag);
        lastInputRate = Math.max(0, tag.getInt("last_input_rate"));
        lastOutputRate = Math.max(0, tag.getInt("last_output_rate"));
        DyeColor previousColor = portColor;
        portColor = tag.contains(TAG_PORT_COLOR_ID) ? DyeColor.byId(tag.getInt(TAG_PORT_COLOR_ID)) : DyeColor.WHITE;
        if (previousColor != portColor) {
            if (level instanceof ServerLevel) {
                syncBlockStateColor();
            } else {
                refreshClientRender();
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        multiblockPartState.writeToTag(tag);
        tag.putInt("last_input_rate", lastInputRate);
        tag.putInt("last_output_rate", lastOutputRate);
        tag.putInt(TAG_PORT_COLOR_ID, portColor.getId());
        return tag;
    }

    private void advanceTelemetryWindows() {
        if (level == null) {
            return;
        }

        long currentTick = level.getGameTime();
        if (currentTick != transferWindowTick) {
            transferWindowTick = currentTick;
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

    private @Nullable MatterBatteryCoreBlockEntity getController() {
        if (!multiblockPartState.isFormed() || level == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(multiblockPartState.getControllerPos());
        return blockEntity instanceof MatterBatteryCoreBlockEntity controller ? controller : null;
    }

    private void refreshControllerPortOverview() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        MatterBatteryCoreBlockEntity controller = getController();
        if (controller == null) {
            return;
        }
        MatterBatteryMultiblockHelper.getOrRecoverBatteryStructure(serverLevel, worldPosition)
                .ifPresent(structure -> controller.refreshStructureStats(serverLevel, structure));
    }

    private void refreshClientRender() {
        if (level != null && level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void syncBlockStateColor() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(MultiblockPortBlock.COLOR) && state.getValue(MultiblockPortBlock.COLOR) != portColor) {
            level.setBlock(worldPosition, state.setValue(MultiblockPortBlock.COLOR, portColor), 3);
            return;
        }
        syncVisualState();
    }

    public static Component getPortDisplayName(DyeColor color) {
        DyeColor sanitized = color == null ? DyeColor.WHITE : color;
        return Component.literal(formatColorName(sanitized) + " Multiblock Port");
    }

    private static String formatColorName(DyeColor color) {
        StringBuilder builder = new StringBuilder();
        for (String part : color.getName().split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private @Nullable Direction getOutwardSide() {
        if (!multiblockPartState.isFormed()) {
            return null;
        }
        return MatterBatteryMultiblockHelper.getOutwardSide(multiblockPartState.getLocalPos(), multiblockPartState.getFront(), multiblockPartState.getWidth(), multiblockPartState.getHeight(), multiblockPartState.getDepth());
    }

}
