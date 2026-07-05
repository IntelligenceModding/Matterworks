package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.menu.SingularityLinkMenu;
import de.artemis.matterworks.common.network.SetMatterNetworkTrackingPayload;
import de.artemis.matterworks.common.network.SingularityLinkActionPayload;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.world.PylonChunkLoading;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SingularityLinkBlockEntity extends MatterPylonBlockEntity implements MenuProvider {
    public static final int SINGULARITY_SLOT = 0;
    public static final int DATA_FORMED = 0;
    public static final int DATA_LINKED = 1;
    public static final int DATA_HAS_SINGULARITY = 2;
    public static final int DATA_CONNECT_STATE = 3;
    public static final int DATA_ENERGY = 4;
    public static final int DATA_ENERGY_CAPACITY = 5;
    public static final int DATA_COUNT = 6;
    private static final int FRAME_RADIUS = 1;
    private static final int ENERGY_CAPACITY = 100_000;
    private static final int MAX_ENERGY_INPUT = 2_000;
    private static final int LINK_TRANSFER_ENERGY_COST = 80;

    private final ItemStackHandler singularityHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            markNetworkDirty();
            syncVisualState();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.MATTER_SINGULARITY.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private final LinkEnergyStorage internalEnergyStorage = new LinkEnergyStorage();

    private final IEnergyStorage externalEnergyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return internalEnergyStorage.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return internalEnergyStorage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return internalEnergyStorage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    private final ContainerData linkData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FORMED -> formed ? 1 : 0;
                case DATA_LINKED -> hasLinkedPartner() ? 1 : 0;
                case DATA_HAS_SINGULARITY -> hasSingularityInserted() ? 1 : 0;
                case DATA_CONNECT_STATE -> connectAvailability.ordinal();
                case DATA_ENERGY -> internalEnergyStorage.getEnergyStored();
                case DATA_ENERGY_CAPACITY -> internalEnergyStorage.getMaxEnergyStored();
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

    private boolean formed;
    private @Nullable BlockPos pairedBridgePos;
    private @Nullable BlockPos ticketedPartnerPos;
    private final Set<BlockPos> ticketedFrameChunkAnchors = new LinkedHashSet<>();
    private int lastSavedCode = -1;
    private boolean lastSavedFormed;
    private boolean lastSavedHasSingularity;
    private long lastOutboundBridgeEnergyUseGameTime = Long.MIN_VALUE;
    private SingularityLinkSavedData.ConnectAvailability connectAvailability = SingularityLinkSavedData.ConnectAvailability.UNAVAILABLE;

    public SingularityLinkBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SINGULARITY_LINK.get(), pos, blockState);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, SingularityLinkBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public ItemStackHandler getSingularityHandler() {
        return singularityHandler;
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return externalEnergyStorage;
    }

    public boolean isStructureFormed() {
        return formed;
    }

    public boolean canOpenSingularityMenu() {
        return formed && getResolvedStructureAxes() != null;
    }

    public boolean hasLinkedPartner() {
        return pairedBridgePos != null;
    }

    public boolean hasInsertedSingularity() {
        return hasSingularityInserted();
    }

    public boolean isActiveSingularityLink() {
        return hasValidRemoteBridge();
    }

    public boolean hasActiveSingularityTransfer() {
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            if (getTransferDisplayAmount(channel) > 0) {
                return true;
            }
        }
        return false;
    }

    public int getEnergyStored() {
        return internalEnergyStorage.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return internalEnergyStorage.getMaxEnergyStored();
    }

    public SingularityLinkSavedData.ConnectAvailability getConnectAvailability() {
        return connectAvailability;
    }

    public @Nullable StructureAxes getResolvedStructureAxes() {
        if (level == null) {
            return null;
        }

        StructureAxes[] candidates = new StructureAxes[]{
                new StructureAxes(Direction.EAST, Direction.SOUTH),
                new StructureAxes(Direction.EAST, Direction.UP),
                new StructureAxes(Direction.SOUTH, Direction.UP)
        };
        for (StructureAxes axes : candidates) {
            if (matchesFrameStructure(axes)) {
                return axes;
            }
        }
        return null;
    }

    public @Nullable BlockPos getPairedBridgePos() {
        return pairedBridgePos;
    }

    public void onBridgeRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            SingularityLinkSavedData.get(serverLevel).remove(worldPosition);
            clearPartnerTicket(serverLevel);
        }
    }

    public void handleMenuAction(Player player, int action) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        switch (action) {
            case SingularityLinkActionPayload.ACTION_LOCATE -> locatePartner(serverLevel, player);
            case SingularityLinkActionPayload.ACTION_CONNECT -> connectToMatchingPartner(serverLevel, player);
            case SingularityLinkActionPayload.ACTION_DISCONNECT -> disconnectPartner(serverLevel, player);
            default -> {
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            refreshStructureState();
            refreshSavedState(serverLevel);
            refreshPartnerState(serverLevel);
            refreshConnectAvailability(serverLevel);
            refreshPartnerTicket(serverLevel);
        }
        return new SingularityLinkMenu(containerId, playerInventory, this, linkData);
    }

    @Override
    public void openMatterNetworkMenu(Player player, boolean remoteAccess) {
        if (!canOpenSingularityMenu()) {
            return;
        }
        player.openMenu(
                new SimpleMenuProvider((containerId, inventory, menuPlayer) -> new SingularityLinkMenu(containerId, inventory, this, linkData, remoteAccess), getDisplayName()),
                worldPosition
        );
    }

    @Override
    public boolean openPrimaryMenu(Player player, boolean remoteAccess) {
        if (!canOpenSingularityMenu()) {
            return false;
        }
        openMatterNetworkMenu(player, remoteAccess);
        return true;
    }

    @Override
    public boolean isMatterNetworkMenuStillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this || !canOpenSingularityMenu()) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    protected void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean structureChanged = refreshStructureState();
        refreshSavedState(serverLevel);
        boolean partnerChanged = refreshPartnerState(serverLevel);
        boolean connectStateChanged = refreshConnectAvailability(serverLevel);
        refreshFrameChunkTickets(serverLevel);
        refreshPartnerTicket(serverLevel);

        super.serverTick();

        if (structureChanged || partnerChanged || connectStateChanged) {
            markNetworkDirty();
            setChanged();
            syncVisualState();
        }
    }

    @Override
    protected boolean supportsChannel(int channel) {
        return channel == CHANNEL_ENERGY;
    }

    @Override
    protected boolean supportsNetworkColorChannel(int channel) {
        return true;
    }

    @Override
    public boolean supportsConfiguredChannel(int channel) {
        return false;
    }

    @Override
    public boolean supportsFilterChannel(int channel) {
        return false;
    }

    @Override
    public void setMode(int channel, de.artemis.matterworks.common.transport.PylonMode mode) {
    }

    @Override
    public void cycleMode(int channel) {
    }

    @Override
    public void cycleModeBackward(int channel) {
    }

    @Override
    public void setPylonId(int channel, int pylonId) {
    }

    @Override
    public void setNetworkColor(int channel, int index, DyeColor color) {
        int previousCode = getLinkCode();
        DyeColor current = super.getNetworkColor(CHANNEL_ENERGY, index);
        DyeColor target = color == null ? DyeColor.WHITE : color;
        if (current == target) {
            return;
        }

        super.setNetworkColor(CHANNEL_ENERGY, index, target);

        if (level instanceof ServerLevel serverLevel) {
            if (previousCode != getLinkCode() && pairedBridgePos != null) {
                SingularityLinkSavedData.get(serverLevel).disconnect(worldPosition);
                refreshPartnerState(serverLevel);
                refreshConnectAvailability(serverLevel);
            }
            refreshSavedState(serverLevel);
        }
    }

    @Override
    protected Set<BlockPos> getTraversalLinkedPositions() {
        LinkedHashSet<BlockPos> links = new LinkedHashSet<>(super.getTraversalLinkedPositions());
        if (level instanceof ServerLevel && hasValidRemoteBridge()) {
            links.add(pairedBridgePos.immutable());
        }
        return Set.copyOf(links);
    }

    @Override
    protected boolean hasTraversalLinkTo(BlockPos pos) {
        return super.hasTraversalLinkTo(pos) || (hasValidRemoteBridge() && pairedBridgePos.equals(pos));
    }

    @Override
    protected boolean canTraverseLinkForChannel(int channel, MatterPylonBlockEntity exporter, BlockPos linkedPos) {
        if (!isRemotePartner(linkedPos)) {
            return true;
        }
        return hasValidRemoteBridge();
    }

    @Override
    protected @Nullable IEnergyStorage getAttachedEnergyStorage(boolean importer) {
        return importer ? externalEnergyStorage : null;
    }

    @Override
    protected @Nullable IItemHandler getAttachedItemHandler() {
        return null;
    }

    @Override
    protected @Nullable net.neoforged.neoforge.fluids.capability.IFluidHandler getAttachedFluidHandler() {
        return null;
    }

    @Override
    protected int getAttachedRedstoneSignal() {
        return 0;
    }

    @Override
    public SimpleContainer createDropInventory() {
        SimpleContainer inventory = new SimpleContainer(1);
        inventory.setItem(0, singularityHandler.getStackInSlot(SINGULARITY_SLOT).copy());
        return inventory;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(ModBlocks.SINGULARITY_LINK.get().getDescriptionId());
    }

    @Override
    protected void refreshChunkLoadingTickets(ServerLevel serverLevel) {
        super.refreshChunkLoadingTickets(serverLevel);
        refreshFrameChunkTickets(serverLevel);
        refreshPartnerTicket(serverLevel);
    }

    @Override
    protected void releaseChunkLoadingTickets(ServerLevel serverLevel) {
        clearFrameChunkTickets(serverLevel);
        clearPartnerTicket(serverLevel);
        super.releaseChunkLoadingTickets(serverLevel);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("singularity_inventory", singularityHandler.serializeNBT(registries));
        tag.putBoolean("formed", formed);
        tag.putInt("energy", internalEnergyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("singularity_inventory")) {
            singularityHandler.deserializeNBT(registries, tag.getCompound("singularity_inventory"));
        }
        formed = tag.getBoolean("formed");
        internalEnergyStorage.setStoredEnergy(tag.getInt("energy"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("singularity_inventory", singularityHandler.serializeNBT(registries));
        tag.putBoolean("formed", formed);
        tag.putInt("energy", internalEnergyStorage.getEnergyStored());
        return tag;
    }

    private boolean refreshStructureState() {
        StructureAxes axes = getResolvedStructureAxes();
        if (axes == null && formed && !areFrameValidationChunksLoaded()) {
            return false;
        }

        boolean nextFormed = axes != null;
        if (formed == nextFormed) {
            return false;
        }
        formed = nextFormed;
        return true;
    }

    private void refreshSavedState(ServerLevel serverLevel) {
        boolean hasSingularity = hasSingularityInserted();
        int code = getLinkCode();
        if (code == lastSavedCode && formed == lastSavedFormed && hasSingularity == lastSavedHasSingularity) {
            return;
        }
        SingularityLinkSavedData.get(serverLevel).update(worldPosition, code, formed, hasSingularity);
        lastSavedCode = code;
        lastSavedFormed = formed;
        lastSavedHasSingularity = hasSingularity;
    }

    private boolean refreshPartnerState(ServerLevel serverLevel) {
        BlockPos nextPartner = SingularityLinkSavedData.get(serverLevel).getConnectedPartner(worldPosition);
        if (Objects.equals(pairedBridgePos, nextPartner)) {
            return false;
        }
        pairedBridgePos = nextPartner == null ? null : nextPartner.immutable();
        return true;
    }

    private boolean refreshConnectAvailability(ServerLevel serverLevel) {
        SingularityLinkSavedData.ConnectAvailability nextAvailability = SingularityLinkSavedData.get(serverLevel).getConnectAvailability(worldPosition, getLinkCode());
        if (connectAvailability == nextAvailability) {
            return false;
        }
        connectAvailability = nextAvailability;
        return true;
    }

    private boolean matchesFrameStructure(StructureAxes axes) {
        if (level == null || axes == null) {
            return false;
        }

        for (int horizontal = -FRAME_RADIUS; horizontal <= FRAME_RADIUS; horizontal++) {
            for (int vertical = -FRAME_RADIUS; vertical <= FRAME_RADIUS; vertical++) {
                if (horizontal == 0 && vertical == 0) {
                    continue;
                }
                BlockPos framePos = worldPosition.relative(axes.horizontalAxis(), horizontal).relative(axes.verticalAxis(), vertical);
                if (!level.hasChunkAt(framePos)) {
                    return false;
                }
                if (!level.getBlockState(framePos).is(ModBlocks.SINGULARITY_LINK_FRAME.get())) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean canOperateAsRemoteBridge() {
        return formed && hasSingularityInserted();
    }

    private boolean hasSingularityInserted() {
        return singularityHandler.getStackInSlot(SINGULARITY_SLOT).is(ModItems.MATTER_SINGULARITY.get());
    }

    private int getLinkCode() {
        return super.getNetworkColor(CHANNEL_ENERGY, 0).getId()
                | (super.getNetworkColor(CHANNEL_ENERGY, 1).getId() << 4)
                | (super.getNetworkColor(CHANNEL_ENERGY, 2).getId() << 8);
    }

    private boolean canTraverseToRemotePartner() {
        return hasValidRemoteBridge() && hasBridgeEnergyBudget();
    }

    private boolean hasValidRemoteBridge() {
        return pairedBridgePos != null
                && canOperateAsRemoteBridge()
                && level instanceof ServerLevel serverLevel
                && serverLevel.getBlockEntity(pairedBridgePos) instanceof SingularityLinkBlockEntity partner
                && partner.pairedBridgePos != null
                && partner.pairedBridgePos.equals(worldPosition)
                && partner.canOperateAsRemoteBridge()
                && partner.getLinkCode() == getLinkCode();
    }

    private boolean hasBridgeEnergyBudget() {
        return internalEnergyStorage.getEnergyStored() >= LINK_TRANSFER_ENERGY_COST;
    }

    private boolean isRemotePartner(BlockPos pos) {
        return pairedBridgePos != null && pairedBridgePos.equals(pos);
    }

    private void refreshPartnerTicket(ServerLevel serverLevel) {
        BlockPos desiredPartner = hasValidRemoteBridge() ? pairedBridgePos : null;
        if (Objects.equals(ticketedPartnerPos, desiredPartner)) {
            return;
        }
        clearPartnerTicket(serverLevel);
        applyPartnerTicket(serverLevel, desiredPartner);
    }

    private void applyPartnerTicket(ServerLevel serverLevel, @Nullable BlockPos partnerPos) {
        if (partnerPos == null) {
            return;
        }
        PylonChunkLoading.forceRemoteNodeTickets(serverLevel, worldPosition, partnerPos);
        ticketedPartnerPos = partnerPos.immutable();
    }

    private void clearPartnerTicket(ServerLevel serverLevel) {
        if (ticketedPartnerPos == null) {
            return;
        }
        PylonChunkLoading.releaseRemoteNodeTickets(serverLevel, worldPosition, ticketedPartnerPos);
        ticketedPartnerPos = null;
    }

    private void refreshFrameChunkTickets(ServerLevel serverLevel) {
        Set<BlockPos> desiredAnchors = getDesiredFrameChunkAnchors();
        if (ticketedFrameChunkAnchors.equals(desiredAnchors)) {
            return;
        }

        List<BlockPos> toRelease = new ArrayList<>();
        for (BlockPos anchor : ticketedFrameChunkAnchors) {
            if (!desiredAnchors.contains(anchor)) {
                toRelease.add(anchor);
            }
        }
        if (!toRelease.isEmpty()) {
            PylonChunkLoading.releaseAdditionalNodeTickets(serverLevel, worldPosition, toRelease);
            ticketedFrameChunkAnchors.removeAll(toRelease);
        }

        List<BlockPos> toForce = new ArrayList<>();
        for (BlockPos anchor : desiredAnchors) {
            if (!ticketedFrameChunkAnchors.contains(anchor)) {
                toForce.add(anchor);
            }
        }
        if (!toForce.isEmpty()) {
            PylonChunkLoading.forceAdditionalNodeTickets(serverLevel, worldPosition, toForce);
            ticketedFrameChunkAnchors.addAll(toForce);
        }
    }

    private void clearFrameChunkTickets(ServerLevel serverLevel) {
        if (ticketedFrameChunkAnchors.isEmpty()) {
            return;
        }
        PylonChunkLoading.releaseAdditionalNodeTickets(serverLevel, worldPosition, ticketedFrameChunkAnchors);
        ticketedFrameChunkAnchors.clear();
    }

    private boolean areFrameValidationChunksLoaded() {
        for (BlockPos anchor : getAllPotentialFrameChunkAnchors()) {
            if (!level.hasChunkAt(anchor)) {
                return false;
            }
        }
        return true;
    }

    private Set<BlockPos> getDesiredFrameChunkAnchors() {
        if (!formed) {
            return Collections.emptySet();
        }

        StructureAxes axes = getResolvedStructureAxes();
        if (axes != null) {
            return getFrameChunkAnchors(axes);
        }

        return getAllPotentialFrameChunkAnchors();
    }

    private Set<BlockPos> getAllPotentialFrameChunkAnchors() {
        LinkedHashSet<BlockPos> anchors = new LinkedHashSet<>();
        for (StructureAxes axes : getCandidateStructureAxes()) {
            anchors.addAll(getFrameChunkAnchors(axes));
        }
        return anchors;
    }

    private Set<BlockPos> getFrameChunkAnchors(StructureAxes axes) {
        Map<Long, BlockPos> anchorsByChunk = new LinkedHashMap<>();
        ChunkPos ownerChunk = new ChunkPos(worldPosition);

        for (int horizontal = -FRAME_RADIUS; horizontal <= FRAME_RADIUS; horizontal++) {
            for (int vertical = -FRAME_RADIUS; vertical <= FRAME_RADIUS; vertical++) {
                if (horizontal == 0 && vertical == 0) {
                    continue;
                }

                BlockPos framePos = worldPosition.relative(axes.horizontalAxis(), horizontal).relative(axes.verticalAxis(), vertical);
                ChunkPos frameChunk = new ChunkPos(framePos);
                if (frameChunk.equals(ownerChunk)) {
                    continue;
                }

                BlockPos anchorPos = new BlockPos(frameChunk.getMinBlockX(), worldPosition.getY(), frameChunk.getMinBlockZ());
                anchorsByChunk.putIfAbsent(frameChunk.toLong(), anchorPos);
            }
        }

        return new LinkedHashSet<>(anchorsByChunk.values());
    }

    private static StructureAxes[] getCandidateStructureAxes() {
        return new StructureAxes[]{
                new StructureAxes(Direction.EAST, Direction.SOUTH),
                new StructureAxes(Direction.EAST, Direction.UP),
                new StructureAxes(Direction.SOUTH, Direction.UP)
        };
    }

    private void locatePartner(ServerLevel serverLevel, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || pairedBridgePos == null) {
            return;
        }
        PacketDistributor.sendToPlayer(serverPlayer, new SetMatterNetworkTrackingPayload(true, pairedBridgePos, getLocatorLabel(pairedBridgePos, serverLevel)));
    }

    private void connectToMatchingPartner(ServerLevel serverLevel, Player player) {
        if (!canOpenSingularityMenu()) {
            return;
        }

        SingularityLinkSavedData data = SingularityLinkSavedData.get(serverLevel);
        SingularityLinkSavedData.ConnectAvailability availability = data.getConnectAvailability(worldPosition, getLinkCode());
        if (availability != SingularityLinkSavedData.ConnectAvailability.AVAILABLE) {
            sendStatus(player, getConnectFailureMessage(availability));
            connectAvailability = availability;
            return;
        }

        BlockPos candidate = data.findUniqueConnectCandidate(worldPosition, getLinkCode());
        if (candidate == null || !data.connect(worldPosition, candidate)) {
            sendStatus(player, Component.literal("Connection failed"));
            refreshConnectAvailability(serverLevel);
            return;
        }

        syncConnectedPair(serverLevel, candidate);
        sendStatus(player, Component.literal("Link connected"));
    }

    private void disconnectPartner(ServerLevel serverLevel, Player player) {
        if (pairedBridgePos == null) {
            return;
        }

        BlockPos previousPartner = pairedBridgePos;
        if (!SingularityLinkSavedData.get(serverLevel).disconnect(worldPosition)) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SetMatterNetworkTrackingPayload(false, previousPartner, ""));
        }
        syncConnectedPair(serverLevel, previousPartner);
        sendStatus(player, Component.literal("Link disconnected"));
    }

    private void syncConnectedPair(ServerLevel serverLevel, @Nullable BlockPos partnerPos) {
        refreshSavedState(serverLevel);
        refreshPartnerState(serverLevel);
        refreshConnectAvailability(serverLevel);
        refreshPartnerTicket(serverLevel);
        markNetworkDirty();
        setChanged();
        syncVisualState();

        if (partnerPos == null) {
            return;
        }

        if (serverLevel.getBlockEntity(partnerPos) instanceof SingularityLinkBlockEntity partner) {
            partner.refreshSavedState(serverLevel);
            partner.refreshPartnerState(serverLevel);
            partner.refreshConnectAvailability(serverLevel);
            partner.refreshPartnerTicket(serverLevel);
            partner.markNetworkDirty();
            partner.setChanged();
            partner.syncVisualState();
        }
    }

    private Component getConnectFailureMessage(SingularityLinkSavedData.ConnectAvailability availability) {
        return switch (availability) {
            case UNAVAILABLE -> Component.literal("Build the frame and insert a singularity first");
            case NO_MATCH -> Component.literal("No matching singularity link found");
            case PAIR_OCCUPIED -> Component.literal("This color code already has a linked pair");
            case MULTIPLE_MATCHES -> Component.literal("More than one matching singularity link was found");
            case LINKED -> Component.literal("This singularity link is already connected");
            case AVAILABLE -> Component.literal("Ready to connect");
        };
    }

    private void sendStatus(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    private static String getLocatorLabel(BlockPos partnerPos, ServerLevel serverLevel) {
        if (serverLevel.getBlockEntity(partnerPos) instanceof MatterPylonBlockEntity target) {
            return target.getControllerTrackedDisplayName() + " [" + target.getControllerTrackedPos().toShortString() + "]";
        }
        return "Singularity Link [" + partnerPos.toShortString() + "]";
    }

    @Override
    protected void onTransferAlongPath(int channel, java.util.List<BlockPos> path, int index, int amount, long gameTime) {
        if (pairedBridgePos == null || amount <= 0 || index < 0 || index >= path.size() - 1) {
            return;
        }
        if (!worldPosition.equals(path.get(index)) || !pairedBridgePos.equals(path.get(index + 1))) {
            return;
        }
        if (lastOutboundBridgeEnergyUseGameTime == gameTime) {
            return;
        }
        internalEnergyStorage.extractEnergy(LINK_TRANSFER_ENERGY_COST, false);
        lastOutboundBridgeEnergyUseGameTime = gameTime;
    }

    @Override
    protected boolean canTransferAlongPathSegment(int channel, java.util.List<BlockPos> path, int index) {
        if (pairedBridgePos == null || index < 0 || index >= path.size() - 1) {
            return true;
        }
        if (!worldPosition.equals(path.get(index)) || !pairedBridgePos.equals(path.get(index + 1))) {
            return true;
        }
        return hasValidRemoteBridge() && hasBridgeEnergyBudget();
    }

    @Override
    protected boolean participatesInChannelColorMatching(int channel, MatterPylonBlockEntity other) {
        return false;
    }

    private final class LinkEnergyStorage extends net.neoforged.neoforge.energy.EnergyStorage {
        private LinkEnergyStorage() {
            super(ENERGY_CAPACITY, MAX_ENERGY_INPUT, LINK_TRANSFER_ENERGY_COST);
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

        private void setStoredEnergy(int energy) {
            this.energy = Math.max(0, Math.min(getMaxEnergyStored(), energy));
        }
    }

    public record StructureAxes(Direction horizontalAxis, Direction verticalAxis) {
    }
}
