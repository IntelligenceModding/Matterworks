package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.block.MatterPylonBlock;
import de.artemis.matterworks.common.debug.SideConfigDebugTracker;
import de.artemis.matterworks.common.filter.MatterFilterData;
import de.artemis.matterworks.common.menu.MatterPylonMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.transport.PylonMode;
import de.artemis.matterworks.common.upgrade.PowerCrystalData;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import de.artemis.matterworks.common.world.PylonChunkLoading;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.Nameable;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.WeakHashMap;

public class MatterPylonBlockEntity extends BlockEntity implements MenuProvider, CustomNamedBlockEntity {
    public static final int CHANNEL_COUNT = 4;
    public static final int CHANNEL_ENERGY = 0;
    public static final int CHANNEL_ITEMS = 1;
    public static final int CHANNEL_FLUIDS = 2;
    public static final int CHANNEL_REDSTONE = 3;
    public static final int FILTER_SLOT_ITEM_IMPORT_WHITELIST = 0;
    public static final int FILTER_SLOT_ITEM_IMPORT_BLACKLIST = 1;
    public static final int FILTER_SLOT_ITEM_EXPORT_WHITELIST = 2;
    public static final int FILTER_SLOT_ITEM_EXPORT_BLACKLIST = 3;
    public static final int FILTER_SLOT_FLUID_IMPORT_WHITELIST = 4;
    public static final int FILTER_SLOT_FLUID_IMPORT_BLACKLIST = 5;
    public static final int FILTER_SLOT_FLUID_EXPORT_WHITELIST = 6;
    public static final int FILTER_SLOT_FLUID_EXPORT_BLACKLIST = 7;
    public static final int FILTER_SLOT_COUNT = 8;
    public static final int CRYSTAL_SLOT_COUNT = 3;
    public static final int NETWORK_COLOR_CODE_PARTS = 3;
    private static final int MAX_LINKS = 4;
    private static final int MAX_LINK_DISTANCE = 16;
    private static final int MAX_ENERGY_TRANSFER_PER_TICK = 120;
    private static final int MAX_ITEM_TRANSFER_PER_TICK = 16;
    private static final int MAX_FLUID_TRANSFER_PER_TICK = 250;
    private static final float TRANSFER_BOOST_PER_CRYSTAL = 0.5F;
    private static final int CRYSTAL_MAINTENANCE_INTERVAL = PowerCrystalChargerBlockEntity.TICKS_PER_PERCENT;
    private static final int CRYSTAL_MAINTENANCE_ENERGY_COST = PowerCrystalChargerBlockEntity.TICKS_PER_PERCENT * PowerCrystalChargerBlockEntity.ENERGY_PER_TICK;
    public static final int DEFAULT_PYLON_ID = 1;
    public static final int DATA_COUNT = CHANNEL_COUNT * 2;
    private static final Map<UUID, PendingLink> PENDING_LINKS = new HashMap<>();
    private static final Set<MatterPylonBlockEntity> CLIENT_LOADED_NODES = Collections.newSetFromMap(new WeakHashMap<>());

    private final LinkedHashSet<BlockPos> linkedPylons = new LinkedHashSet<>();
    private final PylonMode[] modes = createDefaultModes();
    private final int[] pylonIds = createDefaultPylonIds();
    private final DyeColor[][] networkColorCodes = createDefaultNetworkColorCodes();
    private final ItemStackHandler filterHandler = new ItemStackHandler(FILTER_SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case FILTER_SLOT_ITEM_IMPORT_WHITELIST, FILTER_SLOT_ITEM_IMPORT_BLACKLIST,
                        FILTER_SLOT_ITEM_EXPORT_WHITELIST, FILTER_SLOT_ITEM_EXPORT_BLACKLIST -> stack.getItem() == ModItems.MATTER_ITEM_FILTER.get();
                case FILTER_SLOT_FLUID_IMPORT_WHITELIST, FILTER_SLOT_FLUID_IMPORT_BLACKLIST,
                        FILTER_SLOT_FLUID_EXPORT_WHITELIST, FILTER_SLOT_FLUID_EXPORT_BLACKLIST -> stack.getItem() == ModItems.MATTER_FLUID_FILTER.get();
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private final ItemStackHandler crystalHandler = new ItemStackHandler(CRYSTAL_SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return supportsUpgradeCrystals() && PowerCrystalEffects.isPowerCrystal(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private ItemStack pendingItemStack = ItemStack.EMPTY;
    private FluidStack pendingFluidStack = FluidStack.EMPTY;
    private int redstoneOutputSignal;
    private long lastReceivedRedstoneGameTime = Long.MIN_VALUE;
    private final int[] transferDisplayAmounts = new int[CHANNEL_COUNT];
    private final TransferDisplayRole[] transferDisplayRoles = createDefaultTransferDisplayRoles();
    private final int[] transferSourceAmounts = new int[CHANNEL_COUNT];
    private final int[] transferSinkAmounts = new int[CHANNEL_COUNT];
    private final int[] transferTransitAmounts = new int[CHANNEL_COUNT];
    private final Map<BlockPos, Integer> activeLinkChannelMasks = new HashMap<>();
    private long lastTransferGameTime = Long.MIN_VALUE;
    private final int[] lastSyncedTransferDisplayAmounts = createUnsyncedTransferDisplayAmounts();
    private final TransferDisplayRole[] lastSyncedTransferDisplayRoles = new TransferDisplayRole[CHANNEL_COUNT];
    private final Map<BlockPos, Integer> lastSyncedActiveLinkChannelMasks = new HashMap<>();
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int channel = getChannelForDataIndex(index);
            return isModeDataIndex(index) ? modes[channel].ordinal() : pylonIds[channel];
        }

        @Override
        public void set(int index, int value) {
            int channel = getChannelForDataIndex(index);
            if (isModeDataIndex(index)) {
                modes[channel] = PylonMode.values()[Mth.clamp(value, 0, PylonMode.values().length - 1)];
            } else {
                pylonIds[channel] = sanitizePylonId(value);
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };
    private String customName = "";

    public MatterPylonBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.MATTER_PYLON.get(), pos, blockState);
    }

    protected MatterPylonBlockEntity(BlockEntityType<? extends MatterPylonBlockEntity> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            if (!supportsChannel(channel)) {
                modes[channel] = PylonMode.DISABLED;
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MatterPylonBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public static void clearServerLevelState(ServerLevel level) {
        NetworkCacheManager.clear(level);
        PENDING_LINKS.entrySet().removeIf(entry -> entry.getValue().isForLevel(level));
    }

    public static void clearAllSharedState() {
        NetworkCacheManager.clearAll();
        PENDING_LINKS.clear();
    }

    public void handleLinkUse(Player player) {
        if (level == null || level.isClientSide()) {
            return;
        }
        handleLinkInteraction(player);
    }

    public void cycleMode(int channel) {
        if (!isValidChannel(channel) || !supportsChannel(channel)) {
            return;
        }
        modes[channel] = modes[channel].next();
        markNetworkDirty();
        setChanged();
    }

    public void cycleModeBackward(int channel) {
        if (!isValidChannel(channel) || !supportsChannel(channel)) {
            return;
        }
        modes[channel] = modes[channel].previous();
        markNetworkDirty();
        setChanged();
    }

    public void setMode(int channel, PylonMode mode) {
        if (!isValidChannel(channel) || !supportsChannel(channel)) {
            return;
        }
        PylonMode sanitized = mode == null ? PylonMode.DISABLED : mode;
        if (modes[channel] == sanitized) {
            return;
        }
        modes[channel] = sanitized;
        markNetworkDirty();
        setChanged();
        syncVisualState();
    }

    public PylonMode getMode(int channel) {
        return isValidChannel(channel) && supportsChannel(channel) ? modes[channel] : PylonMode.DISABLED;
    }

    public int getPylonId(int channel) {
        return isValidChannel(channel) ? pylonIds[channel] : DEFAULT_PYLON_ID;
    }

    public DyeColor getNetworkColor(int channel, int index) {
        return isValidChannel(channel) && index >= 0 && index < NETWORK_COLOR_CODE_PARTS
                ? networkColorCodes[channel][index]
                : DyeColor.WHITE;
    }

    public void setNetworkColor(int channel, int index, DyeColor color) {
        if (!isValidChannel(channel) || index < 0 || index >= NETWORK_COLOR_CODE_PARTS || !supportsNetworkColorChannel(channel)) {
            return;
        }
        DyeColor sanitized = color == null ? DyeColor.WHITE : color;
        if (networkColorCodes[channel][index] == sanitized) {
            return;
        }
        networkColorCodes[channel][index] = sanitized;
        markNetworkDirty();
        setChanged();
        syncVisualState();
    }

    public int getTransferDisplayAmount(int channel) {
        return isValidChannel(channel) ? transferDisplayAmounts[channel] : 0;
    }

    public TransferDisplayRole getTransferDisplayRole(int channel) {
        return isValidChannel(channel) ? transferDisplayRoles[channel] : TransferDisplayRole.IDLE;
    }

    public int getTransferRoleAmount(int channel, TransferDisplayRole role) {
        if (!isValidChannel(channel) || role == null) {
            return 0;
        }
        return switch (role) {
            case SOURCE -> transferSourceAmounts[channel];
            case SINK -> transferSinkAmounts[channel];
            case TRANSIT -> transferTransitAmounts[channel];
            case IDLE -> 0;
        };
    }

    public int getRedstoneOutputSignal() {
        return redstoneOutputSignal;
    }

    public ItemStackHandler getFilterHandler() {
        return filterHandler;
    }

    public ItemStackHandler getCrystalHandler() {
        return crystalHandler;
    }

    public boolean supportsFilterChannel(int channel) {
        return getImportWhitelistFilterSlotIndex(channel) >= 0 && supportsChannel(channel);
    }

    public int getActiveLinkChannelMask(BlockPos linkedPos) {
        return activeLinkChannelMasks.getOrDefault(linkedPos, 0);
    }

    public void openMatterNetworkMenu(Player player) {
        openMatterNetworkMenu(player, false);
    }

    public void openMatterNetworkMenu(Player player, boolean remoteAccess) {
        player.openMenu(
                new SimpleMenuProvider((containerId, inventory, menuPlayer) -> new MatterPylonMenu(containerId, inventory, this, data, remoteAccess), getMatterNetworkMenuTitle()),
                worldPosition
        );
    }

    public boolean openPrimaryMenu(Player player, boolean remoteAccess) {
        return false;
    }

    public boolean isMatterNetworkMenuStillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    public Set<BlockPos> getLinkedNodePositions() {
        return Set.copyOf(getTraversalLinkedPositions());
    }

    public BlockPos getControllerTrackedPos() {
        return worldPosition;
    }

    public String getControllerTrackedDisplayName() {
        return getDisplayName().getString();
    }

    public boolean hasControllerExternalTarget() {
        return false;
    }

    protected Set<BlockPos> collectConnectedNodePositions() {
        if (level == null) {
            return Set.of();
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        LinkedHashSet<BlockPos> members = new LinkedHashSet<>();
        queue.add(worldPosition);
        members.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.removeFirst();
            MatterPylonBlockEntity current = getNode(level, currentPos);
            if (current == null) {
                continue;
            }

            for (BlockPos linkedPos : current.getTraversalLinkedPositions()) {
                MatterPylonBlockEntity linked = getNode(level, linkedPos);
                if (linked == null || !linked.hasTraversalLinkTo(currentPos)) {
                    continue;
                }
                if (members.add(linkedPos)) {
                    queue.addLast(linkedPos);
                }
            }
        }

        return Set.copyOf(members);
    }

    public static Set<MatterPylonBlockEntity> getClientLoadedNodes(Level level) {
        Set<MatterPylonBlockEntity> nodes = new LinkedHashSet<>();
        for (MatterPylonBlockEntity node : CLIENT_LOADED_NODES) {
            if (node.level == level && !node.isRemoved()) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    public AABB getRenderBoundingBox() {
        AABB bounds = new AABB(worldPosition);
        for (BlockPos linkedPos : getTraversalLinkedPositions()) {
            bounds = bounds.minmax(new AABB(linkedPos));
        }
        return bounds.inflate(1.0D);
    }

    public void setPylonId(int channel, int pylonId) {
        if (!isValidChannel(channel) || !supportsChannel(channel)) {
            return;
        }
        int sanitized = sanitizePylonId(pylonId);
        if (this.pylonIds[channel] != sanitized) {
            this.pylonIds[channel] = sanitized;
            markNetworkDirty();
            setChanged();
        }
    }

    public ContainerData getData() {
        return data;
    }

    public void unlinkAll() {
        if (level == null || level.isClientSide()) {
            return;
        }

        List<BlockPos> linkedPositions = new ArrayList<>(linkedPylons);
        linkedPylons.clear();
        markNetworkDirty();
        for (BlockPos linkedPos : linkedPositions) {
            MatterPylonBlockEntity other = getNode(level, linkedPos);
            if (other != null) {
                other.linkedPylons.remove(worldPosition);
                other.markNetworkDirty();
                other.setChanged();
                other.syncVisualState();
            }
        }
        setChanged();
        syncVisualState();
    }

    protected void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        clearTransferDisplayIfStale(serverLevel.getGameTime());
        clearStaleRedstoneOutput(serverLevel.getGameTime());
        pruneInvalidLinks();
        maintainCrystalCharge(serverLevel);

        processEnergyChannel(serverLevel);
        processItemChannel(serverLevel);
        processFluidChannel(serverLevel);
        processRedstoneChannel(serverLevel);
    }

    private void processEnergyChannel(ServerLevel serverLevel) {
        if (!supportsChannel(CHANNEL_ENERGY) || !modes[CHANNEL_ENERGY].canExport()) {
            return;
        }

        IEnergyStorage sourceStorage = getAttachedEnergyStorage(false);
        if (sourceStorage == null || !sourceStorage.canExtract()) {
            return;
        }

        List<EnergyTransferRoute> routes = findEnergyRoutesToImporters(CHANNEL_ENERGY);
        if (routes.isEmpty()) {
            return;
        }

        distributeEnergyAcrossRoutes(serverLevel, sourceStorage, routes, getMaxEnergyTransferPerTick());
    }

    private void processItemChannel(ServerLevel serverLevel) {
        if (!supportsChannel(CHANNEL_ITEMS) || !modes[CHANNEL_ITEMS].canExport()) {
            return;
        }

        List<ItemTransferRoute> routes = findItemRoutesToImporters(CHANNEL_ITEMS);
        if (routes.isEmpty()) {
            return;
        }

        IItemHandler sourceHandler = getAttachedItemHandler();
        if (sourceHandler == null && pendingItemStack.isEmpty()) {
            return;
        }

        distributeItemsAcrossRoutes(serverLevel, sourceHandler, routes);
    }

    private void processFluidChannel(ServerLevel serverLevel) {
        if (!supportsChannel(CHANNEL_FLUIDS) || !modes[CHANNEL_FLUIDS].canExport()) {
            return;
        }

        List<FluidTransferRoute> routes = findFluidRoutesToImporters(CHANNEL_FLUIDS);
        if (routes.isEmpty()) {
            return;
        }

        IFluidHandler sourceHandler = getAttachedFluidHandler();
        if (sourceHandler == null && pendingFluidStack.isEmpty()) {
            return;
        }

        distributeFluidsAcrossRoutes(serverLevel, sourceHandler, routes);
    }

    private void processRedstoneChannel(ServerLevel serverLevel) {
        if (!supportsChannel(CHANNEL_REDSTONE) || !modes[CHANNEL_REDSTONE].canExport()) {
            return;
        }

        int signal = getAttachedRedstoneSignal();
        if (signal <= 0) {
            return;
        }

        List<RedstoneTransferRoute> routes = findRedstoneRoutesToImporters(CHANNEL_REDSTONE);
        if (routes.isEmpty()) {
            return;
        }

        distributeRedstoneAcrossRoutes(serverLevel, signal, routes);
    }

    private void handleLinkInteraction(Player player) {
        if (level == null) {
            return;
        }

        PendingLink pendingLink = PENDING_LINKS.get(player.getUUID());
        ResourceKey<Level> dimension = level.dimension();

        if (pendingLink == null) {
            PENDING_LINKS.put(player.getUUID(), new PendingLink(dimension, worldPosition, new WeakReference<>((ServerLevel) level)));
            sendActionBar(player, Component.translatable("message.matterworks.pylon.link_started", Component.literal(worldPosition.toShortString())));
            return;
        }

        if (!pendingLink.dimension.equals(dimension)) {
            PENDING_LINKS.put(player.getUUID(), new PendingLink(dimension, worldPosition, new WeakReference<>((ServerLevel) level)));
            sendActionBar(player, Component.translatable("message.matterworks.pylon.invalid_target"));
            return;
        }

        if (pendingLink.pos.equals(worldPosition)) {
            PENDING_LINKS.remove(player.getUUID());
            sendActionBar(player, Component.translatable("message.matterworks.pylon.link_cancelled"));
            return;
        }

        MatterPylonBlockEntity other = getNode(level, pendingLink.pos);
        if (other == null) {
            PENDING_LINKS.remove(player.getUUID());
            sendActionBar(player, Component.translatable("message.matterworks.pylon.invalid_target"));
            return;
        }

        if (worldPosition.distSqr(other.worldPosition) > (double) (MAX_LINK_DISTANCE * MAX_LINK_DISTANCE)) {
            PENDING_LINKS.remove(player.getUUID());
            sendActionBar(player, Component.translatable("message.matterworks.pylon.link_too_far", MAX_LINK_DISTANCE));
            return;
        }

        if (!canLinkTo(other) || !other.canLinkTo(this)) {
            PENDING_LINKS.remove(player.getUUID());
            sendActionBar(player, Component.translatable("message.matterworks.pylon.link_incompatible"));
            return;
        }

        if (linkedPylons.contains(other.worldPosition)) {
            linkedPylons.remove(other.worldPosition);
            other.linkedPylons.remove(worldPosition);
            markNetworkDirty();
            other.markNetworkDirty();
            setChanged();
            other.setChanged();
            syncVisualState();
            other.syncVisualState();
            PENDING_LINKS.remove(player.getUUID());
            sendActionBar(player, Component.translatable("message.matterworks.pylon.link_removed"));
            return;
        }

        if (linkedPylons.size() >= MAX_LINKS || other.linkedPylons.size() >= MAX_LINKS) {
            PENDING_LINKS.remove(player.getUUID());
            sendActionBar(player, Component.translatable("message.matterworks.pylon.link_limit", MAX_LINKS));
            return;
        }

        linkedPylons.add(other.worldPosition.immutable());
        other.linkedPylons.add(worldPosition.immutable());
        markNetworkDirty();
        other.markNetworkDirty();
        setChanged();
        other.setChanged();
        syncVisualState();
        other.syncVisualState();
        PENDING_LINKS.remove(player.getUUID());
        sendActionBar(player, Component.translatable("message.matterworks.pylon.link_created"));
    }

    private void pruneInvalidLinks() {
        if (level == null || linkedPylons.isEmpty()) {
            return;
        }

        List<BlockPos> toRemove = new ArrayList<>();
        for (BlockPos linkedPos : linkedPylons) {
            MatterPylonBlockEntity other = getNode(level, linkedPos);
            if (other == null || !other.linkedPylons.contains(worldPosition)) {
                toRemove.add(linkedPos);
            }
        }

        if (!toRemove.isEmpty()) {
            linkedPylons.removeAll(toRemove);
            markNetworkDirty();
            setChanged();
            syncVisualState();
        }
    }

    private List<EnergyTransferRoute> findEnergyRoutesToImporters(int channel) {
        if (!(level instanceof ServerLevel serverLevel) || getTraversalLinkCount() == 0) {
            return List.of();
        }

        List<EnergyTransferRoute> routes = new ArrayList<>();
        for (CachedRoute cachedRoute : NetworkCacheManager.getRoutes(serverLevel, worldPosition, channel)) {
            MatterPylonBlockEntity targetPylon = getNode(serverLevel, cachedRoute.targetPos());
            if (targetPylon == null) {
                markNetworkDirty();
                continue;
            }
            IEnergyStorage targetStorage = targetPylon.getAttachedEnergyStorage(true);
            if (targetStorage != null && targetStorage.canReceive()) {
                routes.add(new EnergyTransferRoute(cachedRoute.targetPos(), cachedRoute.path(), targetStorage, cachedRoute.priority()));
            }
        }
        return routes;
    }

    private List<ItemTransferRoute> findItemRoutesToImporters(int channel) {
        if (!(level instanceof ServerLevel serverLevel) || getTraversalLinkCount() == 0) {
            return List.of();
        }

        List<ItemTransferRoute> routes = new ArrayList<>();
        for (CachedRoute cachedRoute : NetworkCacheManager.getRoutes(serverLevel, worldPosition, channel)) {
            MatterPylonBlockEntity targetPylon = getNode(serverLevel, cachedRoute.targetPos());
            if (targetPylon == null) {
                markNetworkDirty();
                continue;
            }
            IItemHandler targetHandler = targetPylon.getAttachedItemHandler();
            if (targetHandler != null) {
                routes.add(new ItemTransferRoute(cachedRoute.targetPos(), cachedRoute.path(), targetPylon, targetHandler, cachedRoute.priority()));
            }
        }
        return routes;
    }

    private List<FluidTransferRoute> findFluidRoutesToImporters(int channel) {
        if (!(level instanceof ServerLevel serverLevel) || getTraversalLinkCount() == 0) {
            return List.of();
        }

        List<FluidTransferRoute> routes = new ArrayList<>();
        for (CachedRoute cachedRoute : NetworkCacheManager.getRoutes(serverLevel, worldPosition, channel)) {
            MatterPylonBlockEntity targetPylon = getNode(serverLevel, cachedRoute.targetPos());
            if (targetPylon == null) {
                markNetworkDirty();
                continue;
            }
            IFluidHandler targetHandler = targetPylon.getAttachedFluidHandler();
            if (targetHandler != null) {
                routes.add(new FluidTransferRoute(cachedRoute.targetPos(), cachedRoute.path(), targetPylon, targetHandler, cachedRoute.priority()));
            }
        }
        return routes;
    }

    private List<RedstoneTransferRoute> findRedstoneRoutesToImporters(int channel) {
        if (!(level instanceof ServerLevel serverLevel) || getTraversalLinkCount() == 0) {
            return List.of();
        }

        List<RedstoneTransferRoute> routes = new ArrayList<>();
        for (CachedRoute cachedRoute : NetworkCacheManager.getRoutes(serverLevel, worldPosition, channel)) {
            MatterPylonBlockEntity targetPylon = getNode(serverLevel, cachedRoute.targetPos());
            if (targetPylon == null) {
                markNetworkDirty();
                continue;
            }
            routes.add(new RedstoneTransferRoute(cachedRoute.targetPos(), cachedRoute.path(), targetPylon, cachedRoute.priority()));
        }
        return routes;
    }

    protected @Nullable IEnergyStorage getAttachedEnergyStorage(boolean importer) {
        if (level == null) {
            return null;
        }

        Direction facing = getBlockState().getValue(MatterPylonBlock.FACING);
        BlockPos attachedPos = worldPosition.relative(facing.getOpposite());
        IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, attachedPos, facing);
        if (storage == null) {
            return null;
        }
        if (importer && !storage.canReceive()) {
            return null;
        }
        if (!importer && !storage.canExtract()) {
            return null;
        }
        return storage;
    }

    protected @Nullable IItemHandler getAttachedItemHandler() {
        if (level == null) {
            return null;
        }

        Direction facing = getBlockState().getValue(MatterPylonBlock.FACING);
        BlockPos attachedPos = worldPosition.relative(facing.getOpposite());
        return level.getCapability(Capabilities.ItemHandler.BLOCK, attachedPos, facing);
    }

    protected @Nullable IFluidHandler getAttachedFluidHandler() {
        if (level == null) {
            return null;
        }

        Direction facing = getBlockState().getValue(MatterPylonBlock.FACING);
        BlockPos attachedPos = worldPosition.relative(facing.getOpposite());
        return level.getCapability(Capabilities.FluidHandler.BLOCK, attachedPos, facing);
    }

    protected boolean matchesItemImportFilter(ItemStack candidate) {
        if (level == null) {
            return true;
        }
        ItemStack whitelist = getFilterStack(FILTER_SLOT_ITEM_IMPORT_WHITELIST);
        ItemStack blacklist = getFilterStack(FILTER_SLOT_ITEM_IMPORT_BLACKLIST);
        if (!whitelist.isEmpty() && MatterFilterData.hasItemEntries(whitelist)
                && !MatterFilterData.containsItem(whitelist, level.registryAccess(), candidate)) {
            return false;
        }
        return blacklist.isEmpty() || !MatterFilterData.hasItemEntries(blacklist)
                || !MatterFilterData.containsItem(blacklist, level.registryAccess(), candidate);
    }

    protected boolean matchesItemExportFilter(ItemStack candidate) {
        if (level == null) {
            return true;
        }
        ItemStack whitelist = getFilterStack(FILTER_SLOT_ITEM_EXPORT_WHITELIST);
        ItemStack blacklist = getFilterStack(FILTER_SLOT_ITEM_EXPORT_BLACKLIST);
        if (!whitelist.isEmpty() && MatterFilterData.hasItemEntries(whitelist)
                && !MatterFilterData.containsItem(whitelist, level.registryAccess(), candidate)) {
            return false;
        }
        return blacklist.isEmpty() || !MatterFilterData.hasItemEntries(blacklist)
                || !MatterFilterData.containsItem(blacklist, level.registryAccess(), candidate);
    }

    protected boolean matchesFluidImportFilter(FluidStack candidate) {
        if (level == null) {
            return true;
        }
        ItemStack whitelist = getFilterStack(FILTER_SLOT_FLUID_IMPORT_WHITELIST);
        ItemStack blacklist = getFilterStack(FILTER_SLOT_FLUID_IMPORT_BLACKLIST);
        if (!whitelist.isEmpty() && MatterFilterData.hasFluidEntries(whitelist)
                && !MatterFilterData.containsFluid(whitelist, candidate)) {
            return false;
        }
        return blacklist.isEmpty() || !MatterFilterData.hasFluidEntries(blacklist)
                || !MatterFilterData.containsFluid(blacklist, candidate);
    }

    protected boolean matchesFluidExportFilter(FluidStack candidate) {
        if (level == null) {
            return true;
        }
        ItemStack whitelist = getFilterStack(FILTER_SLOT_FLUID_EXPORT_WHITELIST);
        ItemStack blacklist = getFilterStack(FILTER_SLOT_FLUID_EXPORT_BLACKLIST);
        if (!whitelist.isEmpty() && MatterFilterData.hasFluidEntries(whitelist)
                && !MatterFilterData.containsFluid(whitelist, candidate)) {
            return false;
        }
        return blacklist.isEmpty() || !MatterFilterData.hasFluidEntries(blacklist)
                || !MatterFilterData.containsFluid(blacklist, candidate);
    }

    protected int getAttachedRedstoneSignal() {
        if (level == null) {
            return 0;
        }

        Direction facing = getBlockState().getValue(MatterPylonBlock.FACING);
        BlockPos attachedPos = worldPosition.relative(facing.getOpposite());
        return level.getSignal(attachedPos, facing);
    }

    public SimpleContainer createDropInventory() {
        SimpleContainer inventory = new SimpleContainer(FILTER_SLOT_COUNT + CRYSTAL_SLOT_COUNT);
        for (int slot = 0; slot < filterHandler.getSlots(); slot++) {
            inventory.setItem(slot, filterHandler.getStackInSlot(slot).copy());
        }
        for (int slot = 0; slot < crystalHandler.getSlots(); slot++) {
            inventory.setItem(FILTER_SLOT_COUNT + slot, crystalHandler.getStackInSlot(slot).copy());
        }
        return inventory;
    }

    private int distributeEnergyAcrossRoutes(ServerLevel serverLevel, IEnergyStorage sourceStorage, List<EnergyTransferRoute> routes, int maxTransfer) {
        int remaining = Math.min(maxTransfer, sourceStorage.extractEnergy(maxTransfer, true));
        if (remaining <= 0) {
            return 0;
        }

        Map<Integer, List<EnergyTransferRoute>> routesByPriority = new TreeMap<>(Comparator.reverseOrder());
        for (EnergyTransferRoute route : routes) {
            routesByPriority.computeIfAbsent(route.priority, ignored -> new ArrayList<>()).add(route);
        }

        for (List<EnergyTransferRoute> priorityRoutes : routesByPriority.values()) {
            if (remaining <= 0) {
                break;
            }
            remaining -= transferPriorityTier(serverLevel, sourceStorage, priorityRoutes, remaining);
        }

        return maxTransfer - remaining;
    }

    private int transferPriorityTier(ServerLevel serverLevel, IEnergyStorage sourceStorage, List<EnergyTransferRoute> routes, int maxTransfer) {
        int moved = 0;
        List<EnergyTransferRoute> activeRoutes = new ArrayList<>(routes);
        Map<BlockPos, Integer> transferredByTarget = new HashMap<>();

        while (moved < maxTransfer && !activeRoutes.isEmpty()) {
            boolean anyMoved = false;

            for (int index = 0; index < activeRoutes.size() && moved < maxTransfer; ) {
                EnergyTransferRoute route = activeRoutes.get(index);
                if (route.targetStorage.receiveEnergy(1, true) <= 0) {
                    activeRoutes.remove(index);
                    continue;
                }

                int extracted = sourceStorage.extractEnergy(1, false);
                if (extracted <= 0) {
                    activeRoutes.clear();
                    break;
                }

                int inserted = route.targetStorage.receiveEnergy(extracted, false);
                if (inserted <= 0) {
                    activeRoutes.remove(index);
                    continue;
                }

                moved += inserted;
                anyMoved = true;
                transferredByTarget.merge(route.targetPos, inserted, Integer::sum);
                index++;
            }

            if (!anyMoved) {
                break;
            }
        }

        if (moved > 0) {
            for (EnergyTransferRoute route : routes) {
                Integer routeMoved = transferredByTarget.get(route.targetPos);
                if (routeMoved != null && routeMoved > 0) {
                    recordTransferAlongPath(CHANNEL_ENERGY, route.path, routeMoved, serverLevel.getGameTime());
                }
            }
        }

        return moved;
    }

    private void distributeItemsAcrossRoutes(ServerLevel serverLevel, @Nullable IItemHandler sourceHandler, List<ItemTransferRoute> routes) {
        Map<Integer, List<ItemTransferRoute>> routesByPriority = new TreeMap<>(Comparator.reverseOrder());
        for (ItemTransferRoute route : routes) {
            routesByPriority.computeIfAbsent(route.priority, ignored -> new ArrayList<>()).add(route);
        }

        int remaining = getMaxItemTransferPerTick();
        for (List<ItemTransferRoute> priorityRoutes : routesByPriority.values()) {
            if (remaining <= 0) {
                break;
            }
            remaining -= transferItemPriorityTier(serverLevel, sourceHandler, priorityRoutes, remaining);
        }
    }

    private int transferItemPriorityTier(ServerLevel serverLevel, @Nullable IItemHandler sourceHandler, List<ItemTransferRoute> routes, int maxTransfer) {
        int moved = 0;
        int roundRobinIndex = 0;
        Map<BlockPos, Integer> transferredByTarget = new HashMap<>();

        while (moved < maxTransfer && !routes.isEmpty()) {
            boolean movedThisPass = false;
            int routeCount = routes.size();
            int startIndex = roundRobinIndex % routeCount;

            for (int processed = 0; processed < routeCount && moved < maxTransfer; processed++) {
                ItemTransferRoute route = routes.get((startIndex + processed) % routeCount);
                int remaining = maxTransfer - moved;
                int remainingRoutes = routeCount - processed;
                int shareBudget = Math.max(1, (remaining + remainingRoutes - 1) / remainingRoutes);

                int inserted;
                if (!pendingItemStack.isEmpty()) {
                    inserted = movePendingItemToRoute(route, shareBudget);
                } else if (sourceHandler != null) {
                    inserted = moveSourceItemToRoute(sourceHandler, route, shareBudget);
                } else {
                    inserted = 0;
                }

                if (inserted > 0) {
                    moved += inserted;
                    movedThisPass = true;
                    transferredByTarget.merge(route.targetPos(), inserted, Integer::sum);
                }
            }

            if (!movedThisPass) {
                break;
            }

            roundRobinIndex = (startIndex + 1) % routeCount;
        }

        if (moved > 0) {
            for (ItemTransferRoute route : routes) {
                Integer routeMoved = transferredByTarget.get(route.targetPos);
                if (routeMoved != null && routeMoved > 0) {
                    recordTransferAlongPath(CHANNEL_ITEMS, route.path, routeMoved, serverLevel.getGameTime());
                }
            }
        }

        return moved;
    }

    private void distributeFluidsAcrossRoutes(ServerLevel serverLevel, @Nullable IFluidHandler sourceHandler, List<FluidTransferRoute> routes) {
        Map<Integer, List<FluidTransferRoute>> routesByPriority = new TreeMap<>(Comparator.reverseOrder());
        for (FluidTransferRoute route : routes) {
            routesByPriority.computeIfAbsent(route.priority, ignored -> new ArrayList<>()).add(route);
        }

        int remaining = getMaxFluidTransferPerTick();
        for (List<FluidTransferRoute> priorityRoutes : routesByPriority.values()) {
            if (remaining <= 0) {
                break;
            }
            remaining -= transferFluidPriorityTier(serverLevel, sourceHandler, priorityRoutes, remaining);
        }
    }

    private int transferFluidPriorityTier(ServerLevel serverLevel, @Nullable IFluidHandler sourceHandler, List<FluidTransferRoute> routes, int maxTransfer) {
        int moved = 0;
        int roundRobinIndex = 0;
        Map<BlockPos, Integer> transferredByTarget = new HashMap<>();

        while (moved < maxTransfer && !routes.isEmpty()) {
            boolean movedThisPass = false;
            int routeCount = routes.size();
            int startIndex = roundRobinIndex % routeCount;

            for (int processed = 0; processed < routeCount && moved < maxTransfer; processed++) {
                FluidTransferRoute route = routes.get((startIndex + processed) % routeCount);
                int remaining = maxTransfer - moved;
                int remainingRoutes = routeCount - processed;
                int shareBudget = Math.max(1, (remaining + remainingRoutes - 1) / remainingRoutes);

                int inserted;
                if (!pendingFluidStack.isEmpty()) {
                    inserted = movePendingFluidToRoute(route, shareBudget);
                } else if (sourceHandler != null) {
                    inserted = moveSourceFluidToRoute(sourceHandler, route, shareBudget);
                } else {
                    inserted = 0;
                }

                if (inserted > 0) {
                    moved += inserted;
                    movedThisPass = true;
                    transferredByTarget.merge(route.targetPos(), inserted, Integer::sum);
                }
            }

            if (!movedThisPass) {
                break;
            }

            roundRobinIndex = (startIndex + 1) % routeCount;
        }

        if (moved > 0) {
            for (FluidTransferRoute route : routes) {
                Integer routeMoved = transferredByTarget.get(route.targetPos);
                if (routeMoved != null && routeMoved > 0) {
                    recordTransferAlongPath(CHANNEL_FLUIDS, route.path, routeMoved, serverLevel.getGameTime());
                }
            }
        }

        return moved;
    }

    private void distributeRedstoneAcrossRoutes(ServerLevel serverLevel, int signal, List<RedstoneTransferRoute> routes) {
        int highestPriority = routes.stream().mapToInt(RedstoneTransferRoute::priority).max().orElse(DEFAULT_PYLON_ID);
        long gameTime = serverLevel.getGameTime();

        for (RedstoneTransferRoute route : routes) {
            if (route.priority != highestPriority) {
                continue;
            }
            route.targetPylon.acceptIncomingRedstoneSignal(signal, gameTime);
            recordTransferAlongPath(CHANNEL_REDSTONE, route.path, signal, gameTime);
        }
    }

    private void acceptIncomingRedstoneSignal(int signal, long gameTime) {
        int clamped = Mth.clamp(signal, 0, 15);
        if (lastReceivedRedstoneGameTime != gameTime) {
            lastReceivedRedstoneGameTime = gameTime;
            if (redstoneOutputSignal != clamped) {
                updateRedstoneOutput(clamped);
            }
            return;
        }

        if (clamped > redstoneOutputSignal) {
            updateRedstoneOutput(clamped);
        }
    }

    private int movePendingItemToRoute(ItemTransferRoute route, int maxTransfer) {
        if (pendingItemStack.isEmpty() || maxTransfer <= 0) {
            return 0;
        }

        ItemStack candidate = pendingItemStack.copyWithCount(Math.min(pendingItemStack.getCount(), maxTransfer));
        if (!matchesItemExportFilter(candidate) || !route.targetPylon.matchesItemImportFilter(candidate)) {
            return 0;
        }

        ItemStack simulatedRemainder = ItemHandlerHelper.insertItem(route.targetHandler, candidate.copy(), true);
        int accepted = candidate.getCount() - simulatedRemainder.getCount();
        if (accepted <= 0) {
            return 0;
        }

        ItemStack movedStack = pendingItemStack.copyWithCount(accepted);
        ItemStack actualRemainder = ItemHandlerHelper.insertItem(route.targetHandler, movedStack, false);
        int inserted = accepted - actualRemainder.getCount();
        if (inserted <= 0) {
            return 0;
        }

        pendingItemStack.shrink(inserted);
        if (pendingItemStack.isEmpty()) {
            pendingItemStack = ItemStack.EMPTY;
        }
        if (!actualRemainder.isEmpty()) {
            storePendingItem(actualRemainder);
        }
        setChanged();
        return inserted;
    }

    private int moveSourceItemToRoute(IItemHandler sourceHandler, ItemTransferRoute route, int maxTransfer) {
        if (maxTransfer <= 0) {
            return 0;
        }

        for (int slot = 0; slot < sourceHandler.getSlots(); slot++) {
            ItemStack simulatedExtract = sourceHandler.extractItem(slot, maxTransfer, true);
            if (simulatedExtract.isEmpty()) {
                continue;
            }
            if (!matchesItemExportFilter(simulatedExtract)) {
                continue;
            }
            if (!route.targetPylon.matchesItemImportFilter(simulatedExtract)) {
                continue;
            }

            ItemStack simulatedRemainder = ItemHandlerHelper.insertItem(route.targetHandler, simulatedExtract.copy(), true);
            int accepted = simulatedExtract.getCount() - simulatedRemainder.getCount();
            if (accepted <= 0) {
                continue;
            }

            ItemStack extracted = sourceHandler.extractItem(slot, accepted, false);
            if (extracted.isEmpty()) {
                continue;
            }

            ItemStack actualRemainder = ItemHandlerHelper.insertItem(route.targetHandler, extracted, false);
            int inserted = extracted.getCount() - actualRemainder.getCount();
            if (inserted <= 0) {
                storePendingItem(extracted);
                return 0;
            }

            if (!actualRemainder.isEmpty()) {
                storePendingItem(actualRemainder);
            }
            if (inserted != extracted.getCount() || !actualRemainder.isEmpty()) {
                setChanged();
            }
            return inserted;
        }

        return 0;
    }

    private int movePendingFluidToRoute(FluidTransferRoute route, int maxTransfer) {
        if (pendingFluidStack.isEmpty() || maxTransfer <= 0) {
            return 0;
        }

        FluidStack candidate = pendingFluidStack.copyWithAmount(Math.min(pendingFluidStack.getAmount(), maxTransfer));
        if (!matchesFluidExportFilter(candidate) || !route.targetPylon.matchesFluidImportFilter(candidate)) {
            return 0;
        }

        int accepted = route.targetHandler.fill(candidate, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return 0;
        }

        FluidStack transferStack = pendingFluidStack.copyWithAmount(accepted);
        int inserted = route.targetHandler.fill(transferStack, IFluidHandler.FluidAction.EXECUTE);
        if (inserted <= 0) {
            return 0;
        }

        pendingFluidStack.shrink(inserted);
        if (pendingFluidStack.isEmpty()) {
            pendingFluidStack = FluidStack.EMPTY;
        }
        if (inserted < accepted) {
            storePendingFluid(transferStack.copyWithAmount(accepted - inserted));
        }
        setChanged();
        return inserted;
    }

    private int moveSourceFluidToRoute(IFluidHandler sourceHandler, FluidTransferRoute route, int maxTransfer) {
        if (maxTransfer <= 0) {
            return 0;
        }

        FluidStack simulatedDrain = findDrainableFluid(sourceHandler, maxTransfer);
        if (simulatedDrain.isEmpty()) {
            return 0;
        }
        if (!matchesFluidExportFilter(simulatedDrain)) {
            return 0;
        }

        if (!route.targetPylon.matchesFluidImportFilter(simulatedDrain)) {
            return 0;
        }
        int accepted = route.targetHandler.fill(simulatedDrain, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return 0;
        }

        FluidStack drained = sourceHandler.drain(simulatedDrain.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return 0;
        }

        int inserted = route.targetHandler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (inserted <= 0) {
            storePendingFluid(drained);
            return 0;
        }

        if (inserted < drained.getAmount()) {
            storePendingFluid(drained.copyWithAmount(drained.getAmount() - inserted));
        }
        if (inserted != drained.getAmount()) {
            setChanged();
        }
        return inserted;
    }

    private void storePendingItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (pendingItemStack.isEmpty()) {
            pendingItemStack = stack.copy();
        } else if (ItemStack.isSameItemSameComponents(pendingItemStack, stack)) {
            pendingItemStack.grow(stack.getCount());
        } else {
            pendingItemStack.grow(stack.getCount());
        }
        setChanged();
    }

    private void storePendingFluid(FluidStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (pendingFluidStack.isEmpty()) {
            pendingFluidStack = stack.copy();
        } else if (FluidStack.isSameFluidSameComponents(pendingFluidStack, stack)) {
            pendingFluidStack.grow(stack.getAmount());
        } else {
            pendingFluidStack.grow(stack.getAmount());
        }
        setChanged();
    }

    private static List<BlockPos> buildPath(Map<BlockPos, BlockPos> parent, BlockPos endPos) {
        List<BlockPos> path = new ArrayList<>();
        BlockPos current = endPos;
        while (current != null) {
            path.add(0, current);
            current = parent.get(current);
        }
        return path;
    }

    private static boolean canInsertItem(IItemHandler handler, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return ItemHandlerHelper.insertItem(handler, stack.copy(), true).isEmpty();
    }

    private static FluidStack findDrainableFluid(IFluidHandler handler, int maxDrain) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluidInTank = handler.getFluidInTank(tank);
            if (!fluidInTank.isEmpty()) {
                return fluidInTank.copyWithAmount(Math.min(fluidInTank.getAmount(), maxDrain));
            }
        }
        return FluidStack.EMPTY;
    }

    private static @Nullable MatterPylonBlockEntity getNode(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof MatterPylonBlockEntity pylonBlockEntity ? pylonBlockEntity : null;
    }

    protected final void markNetworkDirty() {
        if (level instanceof ServerLevel serverLevel) {
            NetworkCacheManager.markDirty(serverLevel, worldPosition);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            CLIENT_LOADED_NODES.add(this);
            if (this instanceof de.artemis.matterworks.common.io.SideConfigurableBlockEntity) {
                SideConfigDebugTracker.onClientLoad(this);
            }
        }
        markNetworkDirty();
        if (level instanceof ServerLevel serverLevel) {
            refreshChunkLoadingTickets(serverLevel);
        }
    }

    @Override
    public void setRemoved() {
        markNetworkDirty();
        CLIENT_LOADED_NODES.remove(this);
        SideConfigDebugTracker.onClientUnload(this);
        super.setRemoved();
    }

    public void releaseChunkLoadingTickets() {
        if (level instanceof ServerLevel serverLevel) {
            releaseChunkLoadingTickets(serverLevel);
        }
    }

    private static void sendActionBar(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    private void clearTransferDisplayIfStale(long gameTime) {
        if (hasAnyTransferDisplay() && lastTransferGameTime < gameTime) {
            resetTransferTracking();
            syncTransferVisualState();
        }
    }

    private void clearStaleRedstoneOutput(long gameTime) {
        if (redstoneOutputSignal > 0 && lastReceivedRedstoneGameTime < gameTime) {
            updateRedstoneOutput(0);
        }
    }

    private void updateRedstoneOutput(int signal) {
        int clamped = Mth.clamp(signal, 0, 15);
        if (redstoneOutputSignal == clamped) {
            return;
        }

        redstoneOutputSignal = clamped;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
            level.updateNeighborsAt(worldPosition, state.getBlock());
            level.updateNeighborsAt(worldPosition.relative(state.getValue(MatterPylonBlock.FACING).getOpposite()), state.getBlock());
        }
    }

    private void recordTransferAlongPath(int channel, List<BlockPos> path, int amount, long gameTime) {
        if (!(level instanceof ServerLevel serverLevel) || amount <= 0) {
            return;
        }

        for (int index = 0; index < path.size(); index++) {
            BlockPos pos = path.get(index);
            MatterPylonBlockEntity pylon = getNode(serverLevel, pos);
            if (pylon != null) {
                pylon.recordTransfer(channel, amount, resolveDisplayRole(index, path.size()), gameTime);
            }
        }

        for (int index = 0; index < path.size() - 1; index++) {
            BlockPos currentPos = path.get(index);
            BlockPos nextPos = path.get(index + 1);
            MatterPylonBlockEntity currentPylon = getNode(serverLevel, currentPos);
            MatterPylonBlockEntity nextPylon = getNode(serverLevel, nextPos);
            if (currentPylon != null) {
                currentPylon.recordActiveLink(channel, nextPos, gameTime);
            }
            if (nextPylon != null) {
                nextPylon.recordActiveLink(channel, currentPos, gameTime);
            }
        }
    }

    private void recordTransfer(int channel, int amount, TransferDisplayRole role, long gameTime) {
        if (!isValidChannel(channel) || amount <= 0) {
            return;
        }

        if (lastTransferGameTime != gameTime) {
            resetTransferTracking();
        }

        lastTransferGameTime = gameTime;
        switch (role) {
            case SOURCE -> transferSourceAmounts[channel] += amount;
            case SINK -> transferSinkAmounts[channel] += amount;
            case TRANSIT -> transferTransitAmounts[channel] += amount;
            case IDLE -> {
            }
        }
        updateTransferDisplayFromBuckets(channel);
        syncTransferVisualState();
    }

    private void recordActiveLink(int channel, BlockPos linkedPos, long gameTime) {
        if (!isValidChannel(channel) || !hasTraversalLinkTo(linkedPos)) {
            return;
        }

        if (lastTransferGameTime != gameTime) {
            resetTransferTracking();
        }

        lastTransferGameTime = gameTime;
        int currentMask = activeLinkChannelMasks.getOrDefault(linkedPos, 0);
        int updatedMask = currentMask | (1 << channel);
        if (updatedMask != currentMask) {
            activeLinkChannelMasks.put(linkedPos.immutable(), updatedMask);
            syncTransferVisualState();
        }
    }

    private void updateTransferDisplayFromBuckets(int channel) {
        if (transferSourceAmounts[channel] > 0 || transferSinkAmounts[channel] > 0) {
            if (transferSourceAmounts[channel] >= transferSinkAmounts[channel]) {
                transferDisplayAmounts[channel] = transferSourceAmounts[channel];
                transferDisplayRoles[channel] = TransferDisplayRole.SOURCE;
            } else {
                transferDisplayAmounts[channel] = transferSinkAmounts[channel];
                transferDisplayRoles[channel] = TransferDisplayRole.SINK;
            }
            return;
        }

        if (transferTransitAmounts[channel] > 0) {
            transferDisplayAmounts[channel] = transferTransitAmounts[channel];
            transferDisplayRoles[channel] = TransferDisplayRole.TRANSIT;
            return;
        }

        transferDisplayAmounts[channel] = 0;
        transferDisplayRoles[channel] = TransferDisplayRole.IDLE;
    }

    private void resetTransferTracking() {
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            transferDisplayAmounts[channel] = 0;
            transferDisplayRoles[channel] = TransferDisplayRole.IDLE;
            transferSourceAmounts[channel] = 0;
            transferSinkAmounts[channel] = 0;
            transferTransitAmounts[channel] = 0;
        }
        activeLinkChannelMasks.clear();
    }

    private void syncTransferVisualState() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!transferDisplayChangedSinceLastSync() && !activeLinkChannelMasksChangedSinceLastSync()) {
            return;
        }

        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            lastSyncedTransferDisplayAmounts[channel] = transferDisplayAmounts[channel];
            lastSyncedTransferDisplayRoles[channel] = transferDisplayRoles[channel];
        }
        lastSyncedActiveLinkChannelMasks.clear();
        lastSyncedActiveLinkChannelMasks.putAll(activeLinkChannelMasks);
        setChanged();
        serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    private static TransferDisplayRole resolveDisplayRole(int index, int pathSize) {
        if (index == 0) {
            return TransferDisplayRole.SOURCE;
        }
        if (index == pathSize - 1) {
            return TransferDisplayRole.SINK;
        }
        return TransferDisplayRole.TRANSIT;
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
        syncVisualState();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MatterPylonMenu(containerId, playerInventory, this, data);
    }

    protected boolean supportsChannel(int channel) {
        return true;
    }

    protected boolean supportsNetworkColorChannel(int channel) {
        return supportsChannel(channel);
    }

    protected Set<BlockPos> getTraversalLinkedPositions() {
        return Set.copyOf(linkedPylons);
    }

    protected boolean hasTraversalLinkTo(BlockPos pos) {
        return linkedPylons.contains(pos);
    }

    protected int getTraversalLinkCount() {
        return getTraversalLinkedPositions().size();
    }

    protected boolean canTraverseLinkForChannel(int channel, MatterPylonBlockEntity exporter, BlockPos linkedPos) {
        return true;
    }

    public boolean supportsUpgradeCrystals() {
        return getClass() == MatterPylonBlockEntity.class;
    }

    protected Component getDefaultName() {
        return Component.translatable(ModBlocks.MATTER_PYLON.get().getDescriptionId());
    }

    protected boolean canLinkTo(MatterPylonBlockEntity other) {
        return true;
    }

    public boolean supportsConfiguredChannel(int channel) {
        return supportsChannel(channel);
    }

    public boolean matchesNetworkColorCode(MatterPylonBlockEntity other, int channel) {
        if (other == null || !isValidChannel(channel)) {
            return false;
        }
        for (int index = 0; index < NETWORK_COLOR_CODE_PARTS; index++) {
            if (getNetworkColor(channel, index) != other.getNetworkColor(channel, index)) {
                return false;
            }
        }
        return true;
    }

    public int getEffectiveTransferCap(int channel) {
        return switch (channel) {
            case CHANNEL_ENERGY -> getMaxEnergyTransferPerTick();
            case CHANNEL_ITEMS -> getMaxItemTransferPerTick();
            case CHANNEL_FLUIDS -> getMaxFluidTransferPerTick();
            case CHANNEL_REDSTONE -> 15;
            default -> 0;
        };
    }

    public int getActiveCrystalCount(int channel) {
        net.minecraft.world.item.Item crystalItem = switch (channel) {
            case CHANNEL_ITEMS -> ModItems.CRIMSON_POWER_CRYSTAL.get();
            case CHANNEL_ENERGY -> ModItems.VERDANT_POWER_CRYSTAL.get();
            case CHANNEL_FLUIDS -> ModItems.AZURE_POWER_CRYSTAL.get();
            default -> null;
        };
        if (crystalItem == null) {
            return 0;
        }

        int activeCrystals = 0;
        for (int slot = 0; slot < crystalHandler.getSlots(); slot++) {
            ItemStack stack = crystalHandler.getStackInSlot(slot);
            if (stack.is(crystalItem) && PowerCrystalEffects.isActive(stack)) {
                activeCrystals++;
            }
        }
        return activeCrystals;
    }

    public int getCrystalMaintenanceEnergyPerTick() {
        if (!supportsUpgradeCrystals()) {
            return 0;
        }

        int activeCrystals = 0;
        for (int slot = 0; slot < crystalHandler.getSlots(); slot++) {
            if (PowerCrystalEffects.isActive(crystalHandler.getStackInSlot(slot))) {
                activeCrystals++;
            }
        }
        return activeCrystals * PowerCrystalChargerBlockEntity.ENERGY_PER_TICK;
    }

    private void maintainCrystalCharge(ServerLevel serverLevel) {
        if (!supportsUpgradeCrystals() || serverLevel.getGameTime() % CRYSTAL_MAINTENANCE_INTERVAL != 0L) {
            return;
        }

        boolean changed = false;
        for (int slot = 0; slot < crystalHandler.getSlots(); slot++) {
            ItemStack stack = crystalHandler.getStackInSlot(slot);
            if (!PowerCrystalEffects.isPowerCrystal(stack)) {
                continue;
            }

            int beforeCharge = PowerCrystalData.getChargePercent(stack);
            if (consumeCrystalMaintenanceEnergy(serverLevel, CRYSTAL_MAINTENANCE_ENERGY_COST)) {
                int updatedCharge = Math.min(PowerCrystalData.MAX_CHARGE, beforeCharge + 1);
                if (updatedCharge != beforeCharge) {
                    PowerCrystalData.setChargePercent(stack, updatedCharge);
                    changed = true;
                }
            } else {
                int updatedCharge = PowerCrystalData.drainCharge(stack, 1);
                if (updatedCharge != beforeCharge) {
                    changed = true;
                }
            }
        }

        if (changed) {
            setChanged();
            syncVisualState();
        }
    }

    private boolean consumeCrystalMaintenanceEnergy(ServerLevel serverLevel, int amount) {
        if (amount <= 0) {
            return true;
        }

        List<EnergyMaintenanceSource> sources = collectCrystalMaintenanceSources(serverLevel);
        if (sources.isEmpty()) {
            return false;
        }

        int available = 0;
        for (EnergyMaintenanceSource source : sources) {
            available += source.storage().extractEnergy(amount - available, true);
            if (available >= amount) {
                break;
            }
        }
        if (available < amount) {
            return false;
        }

        int remaining = amount;
        long gameTime = serverLevel.getGameTime();
        for (EnergyMaintenanceSource source : sources) {
            if (remaining <= 0) {
                break;
            }
            int extracted = source.storage().extractEnergy(remaining, false);
            if (extracted <= 0) {
                continue;
            }
            remaining -= extracted;
            if (!source.path().isEmpty()) {
                recordTransferAlongPath(CHANNEL_ENERGY, source.path(), extracted, gameTime);
            }
        }
        return remaining <= 0;
    }

    private List<EnergyMaintenanceSource> collectCrystalMaintenanceSources(ServerLevel serverLevel) {
        List<EnergyMaintenanceSource> sources = new ArrayList<>();

        IEnergyStorage localStorage = getAttachedEnergyStorage(false);
        if (localStorage != null && localStorage.canExtract()) {
            sources.add(new EnergyMaintenanceSource(localStorage, List.of()));
        }

        if (!supportsChannel(CHANNEL_ENERGY) || !modes[CHANNEL_ENERGY].canImport() || getTraversalLinkCount() == 0) {
            return sources;
        }

        List<EnergyMaintenanceSource> networkSources = new ArrayList<>();
        for (BlockPos memberPos : collectConnectedNodePositions()) {
            if (memberPos.equals(worldPosition)) {
                continue;
            }

            MatterPylonBlockEntity exporter = getNode(serverLevel, memberPos);
            if (exporter == null || !exporter.supportsChannel(CHANNEL_ENERGY) || !exporter.modes[CHANNEL_ENERGY].canExport()) {
                continue;
            }

            IEnergyStorage exporterStorage = exporter.getAttachedEnergyStorage(false);
            if (exporterStorage == null || !exporterStorage.canExtract()) {
                continue;
            }

            for (CachedRoute cachedRoute : NetworkCacheManager.getRoutes(serverLevel, memberPos, CHANNEL_ENERGY)) {
                if (cachedRoute.targetPos().equals(worldPosition)) {
                    networkSources.add(new EnergyMaintenanceSource(exporterStorage, cachedRoute.path()));
                    break;
                }
            }
        }

        networkSources.sort(Comparator.comparingInt(source -> source.path().size()));
        sources.addAll(networkSources);
        return sources;
    }

    private int getMaxEnergyTransferPerTick() {
        return getBoostedTransferAmount(MAX_ENERGY_TRANSFER_PER_TICK, ModItems.VERDANT_POWER_CRYSTAL.get());
    }

    private int getMaxItemTransferPerTick() {
        return getBoostedTransferAmount(MAX_ITEM_TRANSFER_PER_TICK, ModItems.CRIMSON_POWER_CRYSTAL.get());
    }

    private int getMaxFluidTransferPerTick() {
        return getBoostedTransferAmount(MAX_FLUID_TRANSFER_PER_TICK, ModItems.AZURE_POWER_CRYSTAL.get());
    }

    private int getBoostedTransferAmount(int baseAmount, net.minecraft.world.item.Item crystalItem) {
        if (!supportsUpgradeCrystals()) {
            return baseAmount;
        }

        int activeCrystals = 0;
        for (int slot = 0; slot < crystalHandler.getSlots(); slot++) {
            ItemStack stack = crystalHandler.getStackInSlot(slot);
            if (stack.is(crystalItem) && PowerCrystalEffects.isActive(stack)) {
                activeCrystals++;
            }
        }
        return Math.max(1, Math.round(baseAmount * (1.0F + activeCrystals * TRANSFER_BOOST_PER_CRYSTAL)));
    }

    protected Component getMatterNetworkMenuTitle() {
        return Component.translatable("screen.matterworks.matter_network.title");
    }

    protected void refreshChunkLoadingTickets(ServerLevel serverLevel) {
        if (getBlockState().hasProperty(MatterPylonBlock.FACING)) {
            PylonChunkLoading.forcePylonTickets(serverLevel, worldPosition, getBlockState().getValue(MatterPylonBlock.FACING));
            return;
        }
        PylonChunkLoading.forceNodeTickets(serverLevel, worldPosition);
    }

    protected void releaseChunkLoadingTickets(ServerLevel serverLevel) {
        if (getBlockState().hasProperty(MatterPylonBlock.FACING)) {
            PylonChunkLoading.releasePylonTickets(serverLevel, worldPosition, getBlockState().getValue(MatterPylonBlock.FACING));
            return;
        }
        PylonChunkLoading.releaseNodeTickets(serverLevel, worldPosition);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (!customName.isEmpty()) {
            tag.putString("custom_name", customName);
        }
        tag.putInt("redstone_output_signal", redstoneOutputSignal);
        writeTransferDisplayTag(tag);
        writeActiveLinkChannelMasksTag(tag);
        writeLinkedPositionsTag(tag);
        writeNetworkColorCodeTag(tag);
        if (supportsUpgradeCrystals()) {
            tag.put("crystal_inventory", crystalHandler.serializeNBT(registries));
        }
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            tag.putString(getModeTagName(channel), modes[channel].name().toLowerCase(Locale.ROOT));
            tag.putInt(getPylonIdTagName(channel), pylonIds[channel]);
        }
        tag.putInt("redstone_output_signal", redstoneOutputSignal);
        writeTransferDisplayTag(tag);
        writeActiveLinkChannelMasksTag(tag);
        if (!pendingItemStack.isEmpty()) {
            tag.put("pending_item", pendingItemStack.save(registries, new CompoundTag()));
        }
        if (!pendingFluidStack.isEmpty()) {
            tag.put("pending_fluid", pendingFluidStack.save(registries));
        }
        tag.put("filter_inventory", filterHandler.serializeNBT(registries));
        tag.put("crystal_inventory", crystalHandler.serializeNBT(registries));
        writeLinkedPositionsTag(tag);
        writeNetworkColorCodeTag(tag);
        if (!customName.isEmpty()) {
            tag.putString("custom_name", customName);
        }
    }

    private void writeLinkedPositionsTag(CompoundTag tag) {
        ListTag links = new ListTag();
        for (BlockPos linkedPos : linkedPylons) {
            CompoundTag linkTag = new CompoundTag();
            linkTag.putInt("x", linkedPos.getX());
            linkTag.putInt("y", linkedPos.getY());
            linkTag.putInt("z", linkedPos.getZ());
            links.add(linkTag);
        }
        tag.put("links", links);
    }

    private void writeNetworkColorCodeTag(CompoundTag tag) {
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            for (int index = 0; index < NETWORK_COLOR_CODE_PARTS; index++) {
                tag.putString(getNetworkColorTagName(channel, index), networkColorCodes[channel][index].getName());
            }
        }
    }

    private void writeActiveLinkChannelMasksTag(CompoundTag tag) {
        ListTag activeLinks = new ListTag();
        for (Map.Entry<BlockPos, Integer> entry : activeLinkChannelMasks.entrySet()) {
            if (entry.getValue() == 0) {
                continue;
            }
            CompoundTag linkTag = new CompoundTag();
            linkTag.putInt("x", entry.getKey().getX());
            linkTag.putInt("y", entry.getKey().getY());
            linkTag.putInt("z", entry.getKey().getZ());
            linkTag.putInt("mask", entry.getValue());
            activeLinks.add(linkTag);
        }
        tag.put("active_link_channel_masks", activeLinks);
    }

    private void readNetworkColorCodeTag(CompoundTag tag) {
        if (hasPerChannelNetworkColorCodeTag(tag)) {
            for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
                for (int index = 0; index < NETWORK_COLOR_CODE_PARTS; index++) {
                    networkColorCodes[channel][index] = parseNetworkColor(tag.getString(getNetworkColorTagName(channel, index)));
                }
            }
            return;
        }

        DyeColor[] legacyColors = createDefaultNetworkColorCode();
        for (int index = 0; index < NETWORK_COLOR_CODE_PARTS; index++) {
            legacyColors[index] = parseNetworkColor(tag.getString(getLegacyNetworkColorTagName(index)));
        }
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            System.arraycopy(legacyColors, 0, networkColorCodes[channel], 0, NETWORK_COLOR_CODE_PARTS);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("mode") || tag.contains("pylon_id")) {
            PylonMode legacyMode = parseMode(tag.getString("mode"));
            int legacyId = sanitizePylonId(tag.contains("pylon_id") ? tag.getInt("pylon_id") : DEFAULT_PYLON_ID);
            for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
                modes[channel] = legacyMode;
                pylonIds[channel] = legacyId;
            }
        } else {
            for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
                modes[channel] = parseMode(tag.getString(getModeTagName(channel)));
                pylonIds[channel] = sanitizePylonId(tag.contains(getPylonIdTagName(channel)) ? tag.getInt(getPylonIdTagName(channel)) : DEFAULT_PYLON_ID);
            }
        }
        readTransferDisplayTag(tag);
        redstoneOutputSignal = Mth.clamp(tag.getInt("redstone_output_signal"), 0, 15);
        lastReceivedRedstoneGameTime = Long.MIN_VALUE;
        pendingItemStack = tag.contains("pending_item") ? ItemStack.parseOptional(registries, tag.getCompound("pending_item")) : ItemStack.EMPTY;
        pendingFluidStack = tag.contains("pending_fluid") ? FluidStack.parseOptional(registries, tag.getCompound("pending_fluid")) : FluidStack.EMPTY;
        readNetworkColorCodeTag(tag);
        if (tag.contains("filter_inventory")) {
            filterHandler.deserializeNBT(registries, tag.getCompound("filter_inventory"));
        }
        if (tag.contains("crystal_inventory")) {
            crystalHandler.deserializeNBT(registries, tag.getCompound("crystal_inventory"));
        }
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            transferSourceAmounts[channel] = 0;
            transferSinkAmounts[channel] = 0;
            transferTransitAmounts[channel] = 0;
            lastSyncedTransferDisplayAmounts[channel] = transferDisplayAmounts[channel];
            lastSyncedTransferDisplayRoles[channel] = transferDisplayRoles[channel];
        }
        activeLinkChannelMasks.clear();
        ListTag activeLinks = tag.getList("active_link_channel_masks", Tag.TAG_COMPOUND);
        for (Tag entry : activeLinks) {
            if (entry instanceof CompoundTag linkTag) {
                activeLinkChannelMasks.put(
                        new BlockPos(linkTag.getInt("x"), linkTag.getInt("y"), linkTag.getInt("z")),
                        linkTag.getInt("mask")
                );
            }
        }
        lastSyncedActiveLinkChannelMasks.clear();
        lastSyncedActiveLinkChannelMasks.putAll(activeLinkChannelMasks);
        linkedPylons.clear();
        ListTag links = tag.getList("links", Tag.TAG_COMPOUND);
        for (Tag entry : links) {
            if (entry instanceof CompoundTag linkTag) {
                linkedPylons.add(new BlockPos(linkTag.getInt("x"), linkTag.getInt("y"), linkTag.getInt("z")));
            }
        }
        customName = normalizeCustomName(tag.getString("custom_name"));
    }

    public static int getModeDataIndex(int channel) {
        return channel * 2;
    }

    public static int getPylonIdDataIndex(int channel) {
        return channel * 2 + 1;
    }

    private void writeTransferDisplayTag(CompoundTag tag) {
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            tag.putInt(getTransferDisplayAmountTagName(channel), transferDisplayAmounts[channel]);
            tag.putString(getTransferDisplayRoleTagName(channel), transferDisplayRoles[channel].name().toLowerCase(Locale.ROOT));
        }
    }

    private void readTransferDisplayTag(CompoundTag tag) {
        if (tag.contains("transfer_display_amount") || tag.contains("transfer_display_role")) {
            transferDisplayAmounts[CHANNEL_ENERGY] = tag.getInt("transfer_display_amount");
            transferDisplayRoles[CHANNEL_ENERGY] = parseTransferDisplayRole(tag.getString("transfer_display_role"));
            for (int channel = 1; channel < CHANNEL_COUNT; channel++) {
                transferDisplayAmounts[channel] = 0;
                transferDisplayRoles[channel] = TransferDisplayRole.IDLE;
            }
            return;
        }

        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            transferDisplayAmounts[channel] = tag.getInt(getTransferDisplayAmountTagName(channel));
            transferDisplayRoles[channel] = parseTransferDisplayRole(tag.getString(getTransferDisplayRoleTagName(channel)));
        }
    }

    private static int sanitizePylonId(int id) {
        return Math.max(DEFAULT_PYLON_ID, id);
    }

    private ItemStack getFilterStack(int slotIndex) {
        return slotIndex >= 0 && slotIndex < filterHandler.getSlots() ? filterHandler.getStackInSlot(slotIndex) : ItemStack.EMPTY;
    }

    public static int getImportWhitelistFilterSlotIndex(int channel) {
        return switch (channel) {
            case CHANNEL_ITEMS -> FILTER_SLOT_ITEM_IMPORT_WHITELIST;
            case CHANNEL_FLUIDS -> FILTER_SLOT_FLUID_IMPORT_WHITELIST;
            default -> -1;
        };
    }

    public static int getImportBlacklistFilterSlotIndex(int channel) {
        return switch (channel) {
            case CHANNEL_ITEMS -> FILTER_SLOT_ITEM_IMPORT_BLACKLIST;
            case CHANNEL_FLUIDS -> FILTER_SLOT_FLUID_IMPORT_BLACKLIST;
            default -> -1;
        };
    }

    public static int getExportWhitelistFilterSlotIndex(int channel) {
        return switch (channel) {
            case CHANNEL_ITEMS -> FILTER_SLOT_ITEM_EXPORT_WHITELIST;
            case CHANNEL_FLUIDS -> FILTER_SLOT_FLUID_EXPORT_WHITELIST;
            default -> -1;
        };
    }

    public static int getExportBlacklistFilterSlotIndex(int channel) {
        return switch (channel) {
            case CHANNEL_ITEMS -> FILTER_SLOT_ITEM_EXPORT_BLACKLIST;
            case CHANNEL_FLUIDS -> FILTER_SLOT_FLUID_EXPORT_BLACKLIST;
            default -> -1;
        };
    }

    private static int[] createDefaultPylonIds() {
        int[] ids = new int[CHANNEL_COUNT];
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            ids[channel] = DEFAULT_PYLON_ID;
        }
        return ids;
    }

    private static PylonMode[] createDefaultModes() {
        PylonMode[] channelModes = new PylonMode[CHANNEL_COUNT];
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            channelModes[channel] = PylonMode.IMPORT_EXPORT;
        }
        return channelModes;
    }

    private static TransferDisplayRole[] createDefaultTransferDisplayRoles() {
        TransferDisplayRole[] roles = new TransferDisplayRole[CHANNEL_COUNT];
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            roles[channel] = TransferDisplayRole.IDLE;
        }
        return roles;
    }

    private static int[] createUnsyncedTransferDisplayAmounts() {
        int[] amounts = new int[CHANNEL_COUNT];
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            amounts[channel] = Integer.MIN_VALUE;
        }
        return amounts;
    }

    private static DyeColor[][] createDefaultNetworkColorCodes() {
        DyeColor[][] colors = new DyeColor[CHANNEL_COUNT][NETWORK_COLOR_CODE_PARTS];
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            colors[channel] = createDefaultNetworkColorCode();
        }
        return colors;
    }

    private static DyeColor[] createDefaultNetworkColorCode() {
        DyeColor[] colors = new DyeColor[NETWORK_COLOR_CODE_PARTS];
        for (int index = 0; index < NETWORK_COLOR_CODE_PARTS; index++) {
            colors[index] = DyeColor.WHITE;
        }
        return colors;
    }

    protected void syncVisualState() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static boolean isValidChannel(int channel) {
        return channel >= 0 && channel < CHANNEL_COUNT;
    }

    private static boolean isModeDataIndex(int index) {
        return index % 2 == 0;
    }

    private static int getChannelForDataIndex(int index) {
        return Mth.clamp(index / 2, 0, CHANNEL_COUNT - 1);
    }

    private static String getModeTagName(int channel) {
        return "channel_" + (channel + 1) + "_mode";
    }

    private static String getPylonIdTagName(int channel) {
        return "channel_" + (channel + 1) + "_id";
    }

    private static String getNetworkColorTagName(int channel, int index) {
        return "channel_" + (channel + 1) + "_network_color_" + (index + 1);
    }

    private static String getLegacyNetworkColorTagName(int index) {
        return "network_color_" + (index + 1);
    }

    private static boolean hasPerChannelNetworkColorCodeTag(CompoundTag tag) {
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            for (int index = 0; index < NETWORK_COLOR_CODE_PARTS; index++) {
                if (tag.contains(getNetworkColorTagName(channel, index), Tag.TAG_STRING)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getTransferDisplayAmountTagName(int channel) {
        return "channel_" + (channel + 1) + "_transfer_display_amount";
    }

    private static String getTransferDisplayRoleTagName(int channel) {
        return "channel_" + (channel + 1) + "_transfer_display_role";
    }

    private static String normalizeCustomName(String customName) {
        String normalized = customName.strip();
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private static DyeColor parseNetworkColor(String serializedColor) {
        DyeColor color = DyeColor.byName(serializedColor, null);
        return color == null ? DyeColor.WHITE : color;
    }

    private static PylonMode parseMode(String serializedMode) {
        return switch (serializedMode) {
            case "export" -> PylonMode.EXPORT;
            case "import" -> PylonMode.IMPORT;
            case "relay", "import_export" -> PylonMode.IMPORT_EXPORT;
            default -> PylonMode.IMPORT_EXPORT;
        };
    }

    private static TransferDisplayRole parseTransferDisplayRole(String serializedRole) {
        return switch (serializedRole) {
            case "source" -> TransferDisplayRole.SOURCE;
            case "sink" -> TransferDisplayRole.SINK;
            case "transit" -> TransferDisplayRole.TRANSIT;
            default -> TransferDisplayRole.IDLE;
        };
    }

    private boolean hasAnyTransferDisplay() {
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            if (transferDisplayAmounts[channel] > 0) {
                return true;
            }
        }
        return !activeLinkChannelMasks.isEmpty();
    }

    private boolean transferDisplayChangedSinceLastSync() {
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            if (lastSyncedTransferDisplayAmounts[channel] != transferDisplayAmounts[channel]
                    || lastSyncedTransferDisplayRoles[channel] != transferDisplayRoles[channel]) {
                return true;
            }
        }
        return false;
    }

    private boolean activeLinkChannelMasksChangedSinceLastSync() {
        return !lastSyncedActiveLinkChannelMasks.equals(activeLinkChannelMasks);
    }

    private record PendingLink(ResourceKey<Level> dimension, BlockPos pos, WeakReference<ServerLevel> levelRef) {
        private boolean isForLevel(ServerLevel level) {
            ServerLevel referencedLevel = levelRef.get();
            return referencedLevel == level || (referencedLevel == null && dimension.equals(level.dimension()));
        }
    }

    public enum TransferDisplayRole {
        IDLE,
        SOURCE,
        TRANSIT,
        SINK
    }

    private static final class NetworkCacheManager {
        private static final Map<ServerLevel, DimensionNetworkCache> CACHES = new WeakHashMap<>();

        private static List<CachedRoute> getRoutes(ServerLevel level, BlockPos exporterPos, int channel) {
            DimensionNetworkCache cache = CACHES.computeIfAbsent(level, ignored -> new DimensionNetworkCache());
            CachedNetwork network = cache.getOrBuild(level, exporterPos);
            return network.routesByExporterAndChannel
                    .getOrDefault(exporterPos, Map.of())
                    .getOrDefault(channel, List.of());
        }

        private static void markDirty(ServerLevel level, BlockPos pos) {
            CACHES.computeIfAbsent(level, ignored -> new DimensionNetworkCache()).markDirty(pos);
        }

        private static void clear(ServerLevel level) {
            CACHES.remove(level);
        }

        private static void clearAll() {
            CACHES.clear();
        }
    }

    private static final class DimensionNetworkCache {
        private final Map<BlockPos, CachedNetwork> networksByMember = new HashMap<>();

        private CachedNetwork getOrBuild(ServerLevel level, BlockPos memberPos) {
            CachedNetwork cachedNetwork = networksByMember.get(memberPos);
            if (cachedNetwork != null && !cachedNetwork.dirty) {
                return cachedNetwork;
            }
            CachedNetwork rebuilt = buildNetwork(level, memberPos);
            for (BlockPos member : rebuilt.members) {
                networksByMember.put(member, rebuilt);
            }
            return rebuilt;
        }

        private void markDirty(BlockPos pos) {
            CachedNetwork cachedNetwork = networksByMember.remove(pos);
            if (cachedNetwork != null) {
                cachedNetwork.dirty = true;
                for (BlockPos member : cachedNetwork.members) {
                    networksByMember.remove(member);
                }
            }
        }

        private CachedNetwork buildNetwork(ServerLevel level, BlockPos seedPos) {
            MatterPylonBlockEntity seedPylon = getNode(level, seedPos);
            if (seedPylon == null) {
                return CachedNetwork.empty(seedPos);
            }

            ArrayDeque<BlockPos> componentQueue = new ArrayDeque<>();
            Set<BlockPos> members = new LinkedHashSet<>();
            componentQueue.add(seedPos);
            members.add(seedPos);

            while (!componentQueue.isEmpty()) {
                BlockPos currentPos = componentQueue.removeFirst();
                MatterPylonBlockEntity currentPylon = getNode(level, currentPos);
                if (currentPylon == null) {
                    continue;
                }

                for (BlockPos linkedPos : currentPylon.getTraversalLinkedPositions()) {
                    MatterPylonBlockEntity linkedPylon = getNode(level, linkedPos);
                    if (linkedPylon == null || !linkedPylon.hasTraversalLinkTo(currentPos)) {
                        continue;
                    }
                    if (members.add(linkedPos)) {
                        componentQueue.addLast(linkedPos);
                    }
                }
            }

            Map<BlockPos, Map<Integer, List<CachedRoute>>> routesByExporterAndChannel = new HashMap<>();
            for (BlockPos exporterPos : members) {
                Map<Integer, List<CachedRoute>> routesByChannel = buildRoutesForExporter(level, exporterPos, members);
                if (!routesByChannel.isEmpty()) {
                    routesByExporterAndChannel.put(exporterPos, routesByChannel);
                }
            }
            return new CachedNetwork(Set.copyOf(members), routesByExporterAndChannel);
        }

        private Map<Integer, List<CachedRoute>> buildRoutesForExporter(ServerLevel level, BlockPos exporterPos, Set<BlockPos> members) {
            MatterPylonBlockEntity exporterPylon = getNode(level, exporterPos);
            if (exporterPylon == null) {
                return Map.of();
            }

            Map<Integer, List<CachedRoute>> routesByChannel = new HashMap<>();

            for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
                ArrayDeque<BlockPos> queue = new ArrayDeque<>();
                Map<BlockPos, BlockPos> parent = new HashMap<>();
                Set<BlockPos> visited = new HashSet<>();

                queue.add(exporterPos);
                visited.add(exporterPos);

                while (!queue.isEmpty()) {
                    BlockPos currentPos = queue.removeFirst();
                    MatterPylonBlockEntity currentPylon = getNode(level, currentPos);
                    if (currentPylon == null) {
                        continue;
                    }

                    if (!currentPos.equals(exporterPos)
                            && currentPylon.supportsChannel(channel)
                            && currentPylon.modes[channel].canImport()
                            && exporterPylon.matchesNetworkColorCode(currentPylon, channel)) {
                        routesByChannel
                                .computeIfAbsent(channel, ignored -> new ArrayList<>())
                                .add(new CachedRoute(currentPos, buildPath(parent, currentPos), currentPylon.pylonIds[channel]));
                    }

                    for (BlockPos linkedPos : currentPylon.getTraversalLinkedPositions()) {
                        if (!members.contains(linkedPos) || !currentPylon.canTraverseLinkForChannel(channel, exporterPylon, linkedPos)) {
                            continue;
                        }
                        MatterPylonBlockEntity linkedPylon = getNode(level, linkedPos);
                        if (linkedPylon == null
                                || !linkedPylon.hasTraversalLinkTo(currentPos)
                                || !linkedPylon.canTraverseLinkForChannel(channel, exporterPylon, currentPos)) {
                            continue;
                        }
                        if (visited.add(linkedPos)) {
                            parent.put(linkedPos, currentPos);
                            queue.addLast(linkedPos);
                        }
                    }
                }
            }

            return routesByChannel;
        }
    }

    private static final class CachedNetwork {
        private final Set<BlockPos> members;
        private final Map<BlockPos, Map<Integer, List<CachedRoute>>> routesByExporterAndChannel;
        private boolean dirty;

        private CachedNetwork(Set<BlockPos> members, Map<BlockPos, Map<Integer, List<CachedRoute>>> routesByExporterAndChannel) {
            this.members = members;
            this.routesByExporterAndChannel = routesByExporterAndChannel;
        }

        private static CachedNetwork empty(BlockPos pos) {
            return new CachedNetwork(Set.of(pos), Map.of());
        }
    }

    private record CachedRoute(BlockPos targetPos, List<BlockPos> path, int priority) {
    }

    private record EnergyTransferRoute(BlockPos targetPos, List<BlockPos> path, IEnergyStorage targetStorage, int priority) {
    }

    private record ItemTransferRoute(BlockPos targetPos, List<BlockPos> path, MatterPylonBlockEntity targetPylon, IItemHandler targetHandler, int priority) {
    }

    private record FluidTransferRoute(BlockPos targetPos, List<BlockPos> path, MatterPylonBlockEntity targetPylon, IFluidHandler targetHandler, int priority) {
    }

    private record RedstoneTransferRoute(BlockPos targetPos, List<BlockPos> path, MatterPylonBlockEntity targetPylon, int priority) {
    }

    private record EnergyMaintenanceSource(IEnergyStorage storage, List<BlockPos> path) {
    }

}
