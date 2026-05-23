package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.block.MatterPylonBlock;
import de.artemis.matterworks.common.menu.SingularityLinkMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.world.PylonChunkLoading;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class SingularityLinkBlockEntity extends MatterPylonBlockEntity implements MenuProvider {
    public static final int SINGULARITY_SLOT = 0;
    public static final int DATA_FORMED = 0;
    public static final int DATA_LINKED = 1;
    public static final int DATA_COUNT = 2;
    private static final int FRAME_RADIUS = 1;

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

    private final ContainerData linkData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FORMED -> formed ? 1 : 0;
                case DATA_LINKED -> hasActivePartner() ? 1 : 0;
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
    private int lastSavedCode = -1;
    private boolean lastSavedFormed;
    private boolean lastSavedHasSingularity;

    public SingularityLinkBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SINGULARITY_LINK.get(), pos, blockState);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, SingularityLinkBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public ItemStackHandler getSingularityHandler() {
        return singularityHandler;
    }

    public boolean isStructureFormed() {
        return formed;
    }

    public boolean hasActivePartner() {
        return pairedBridgePos != null;
    }

    public @Nullable BlockPos getPairedBridgePos() {
        return pairedBridgePos;
    }

    public void onBridgeRemoved() {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SingularityLinkSavedData.get(serverLevel).remove(worldPosition);
            clearPartnerTicket(serverLevel);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SingularityLinkMenu(containerId, playerInventory, this, linkData);
    }

    @Override
    public void openMatterNetworkMenu(Player player, boolean remoteAccess) {
        player.openMenu(
                new SimpleMenuProvider((containerId, inventory, menuPlayer) -> new SingularityLinkMenu(containerId, inventory, this, linkData, remoteAccess), getDisplayName()),
                worldPosition
        );
    }

    @Override
    public boolean openPrimaryMenu(Player player, boolean remoteAccess) {
        openMatterNetworkMenu(player, remoteAccess);
        return true;
    }

    @Override
    public boolean isMatterNetworkMenuStillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    protected void serverTick() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        boolean structureChanged = refreshStructureState();
        refreshSavedState(serverLevel);
        boolean partnerChanged = refreshPartnerState(serverLevel);
        if (structureChanged || partnerChanged) {
            markNetworkDirty();
            setChanged();
            syncVisualState();
        }
        super.serverTick();
    }

    @Override
    protected boolean supportsChannel(int channel) {
        return false;
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
        DyeColor current = super.getNetworkColor(CHANNEL_ENERGY, index);
        DyeColor target = color == null ? DyeColor.WHITE : color;
        if (current == target) {
            return;
        }
        for (int actualChannel = 0; actualChannel < CHANNEL_COUNT; actualChannel++) {
            super.setNetworkColor(actualChannel, index, target);
        }
    }

    @Override
    protected Set<BlockPos> getTraversalLinkedPositions() {
        LinkedHashSet<BlockPos> links = new LinkedHashSet<>(super.getTraversalLinkedPositions());
        if (level instanceof net.minecraft.server.level.ServerLevel && canTraverseToRemotePartner()) {
            links.add(pairedBridgePos.immutable());
        }
        return Set.copyOf(links);
    }

    @Override
    protected boolean hasTraversalLinkTo(BlockPos pos) {
        return super.hasTraversalLinkTo(pos) || (canTraverseToRemotePartner() && pairedBridgePos.equals(pos));
    }

    @Override
    protected boolean canTraverseLinkForChannel(int channel, MatterPylonBlockEntity exporter, BlockPos linkedPos) {
        if (!isRemotePartner(linkedPos)) {
            return true;
        }
        return exporter != null && exporter.matchesNetworkColorCode(this, channel);
    }

    @Override
    protected @Nullable net.neoforged.neoforge.energy.IEnergyStorage getAttachedEnergyStorage(boolean importer) {
        return null;
    }

    @Override
    protected @Nullable net.neoforged.neoforge.items.IItemHandler getAttachedItemHandler() {
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
    protected void refreshChunkLoadingTickets(net.minecraft.server.level.ServerLevel serverLevel) {
        super.refreshChunkLoadingTickets(serverLevel);
        applyPartnerTicket(serverLevel, pairedBridgePos);
    }

    @Override
    protected void releaseChunkLoadingTickets(net.minecraft.server.level.ServerLevel serverLevel) {
        clearPartnerTicket(serverLevel);
        super.releaseChunkLoadingTickets(serverLevel);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("singularity_inventory", singularityHandler.serializeNBT(registries));
        tag.putBoolean("formed", formed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("singularity_inventory")) {
            singularityHandler.deserializeNBT(registries, tag.getCompound("singularity_inventory"));
        }
        formed = tag.getBoolean("formed");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("singularity_inventory", singularityHandler.serializeNBT(registries));
        tag.putBoolean("formed", formed);
        return tag;
    }

    private boolean refreshStructureState() {
        boolean nextFormed = validateFrameStructure();
        if (formed == nextFormed) {
            return false;
        }
        formed = nextFormed;
        return true;
    }

    private void refreshSavedState(net.minecraft.server.level.ServerLevel serverLevel) {
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

    private boolean refreshPartnerState(net.minecraft.server.level.ServerLevel serverLevel) {
        BlockPos nextPartner = canOperateAsRemoteBridge()
                ? SingularityLinkSavedData.get(serverLevel).findUniquePartner(worldPosition, getLinkCode())
                : null;
        if (Objects.equals(pairedBridgePos, nextPartner)) {
            return false;
        }
        clearPartnerTicket(serverLevel);
        pairedBridgePos = nextPartner == null ? null : nextPartner.immutable();
        applyPartnerTicket(serverLevel, pairedBridgePos);
        return true;
    }

    private boolean validateFrameStructure() {
        if (level == null || !getBlockState().hasProperty(MatterPylonBlock.FACING)) {
            return false;
        }

        Direction facing = getBlockState().getValue(MatterPylonBlock.FACING);
        Direction horizontalAxis = facing.getAxis().isVertical() ? Direction.EAST : facing.getClockWise();
        Direction verticalAxis = facing.getAxis().isVertical() ? Direction.SOUTH : Direction.UP;

        for (int horizontal = -FRAME_RADIUS; horizontal <= FRAME_RADIUS; horizontal++) {
            for (int vertical = -FRAME_RADIUS; vertical <= FRAME_RADIUS; vertical++) {
                if (horizontal == 0 && vertical == 0) {
                    continue;
                }
                BlockPos framePos = worldPosition.relative(horizontalAxis, horizontal).relative(verticalAxis, vertical);
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
        return pairedBridgePos != null && level instanceof net.minecraft.server.level.ServerLevel serverLevel
                && serverLevel.getBlockEntity(pairedBridgePos) instanceof SingularityLinkBlockEntity partner
                && partner.canOperateAsRemoteBridge()
                && partner.getLinkCode() == getLinkCode();
    }

    private boolean isRemotePartner(BlockPos pos) {
        return pairedBridgePos != null && pairedBridgePos.equals(pos);
    }

    private void applyPartnerTicket(net.minecraft.server.level.ServerLevel serverLevel, @Nullable BlockPos partnerPos) {
        if (partnerPos == null) {
            return;
        }
        PylonChunkLoading.forceRemoteNodeTickets(serverLevel, worldPosition, partnerPos);
        ticketedPartnerPos = partnerPos.immutable();
    }

    private void clearPartnerTicket(net.minecraft.server.level.ServerLevel serverLevel) {
        if (ticketedPartnerPos == null) {
            return;
        }
        PylonChunkLoading.releaseRemoteNodeTickets(serverLevel, worldPosition, ticketedPartnerPos);
        ticketedPartnerPos = null;
    }
}
