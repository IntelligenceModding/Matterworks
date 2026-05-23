package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.menu.EnergyCellMenu;
import de.artemis.matterworks.common.menu.FluidTankMenu;
import de.artemis.matterworks.common.menu.MatterNetworkControllerMenu;
import de.artemis.matterworks.common.menu.MatterNetworkMonitorMenu;
import de.artemis.matterworks.common.menu.MatterPylonMenu;
import de.artemis.matterworks.common.menu.MatterStorageBarrelMenu;
import de.artemis.matterworks.common.transport.PylonMode;
import de.artemis.matterworks.common.network.MatterNetworkControllerActionPayload;
import de.artemis.matterworks.common.network.SetMatterNetworkTrackingPayload;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MatterNetworkControllerBlockEntity extends MatterPylonBlockEntity {
    private static final int OVERVIEW_REFRESH_INTERVAL = 10;

    private List<ControllerEntry> overviewEntries = List.of();

    public MatterNetworkControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MATTER_NETWORK_CONTROLLER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MatterNetworkControllerBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public List<ControllerEntry> getOverviewEntries() {
        return List.copyOf(overviewEntries);
    }

    public boolean isControllerMenuStillValid(Player player, boolean remoteAccess) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return remoteAccess || player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    public void openRemoteMenu(ServerPlayer player) {
        player.openMenu(
                new RemoteMenuProvider(getDisplayName()) {
                    @Override
                    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player menuPlayer) {
                        return new MatterNetworkControllerMenu(containerId, inventory, MatterNetworkControllerBlockEntity.this, true);
                    }
                },
                buffer -> {
                    buffer.writeBlockPos(worldPosition);
                    buffer.writeBoolean(true);
                }
        );
    }

    public boolean isConnectedTargetPosition(BlockPos targetPos) {
        return targetPos != null && !targetPos.equals(worldPosition) && collectConnectedNodePositions().contains(targetPos);
    }

    public void handleAction(Player player, BlockPos targetPos, int action) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        MatterPylonBlockEntity target = getConnectedTarget(serverLevel, targetPos);
        if (target == null) {
            return;
        }

        if (action == MatterNetworkControllerActionPayload.ACTION_LOCATE) {
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new SetMatterNetworkTrackingPayload(true, target.getControllerTrackedPos(), getLocatorLabel(target)));
            }
            return;
        }

        if (action == MatterNetworkControllerActionPayload.ACTION_OPEN_GUI && player instanceof ServerPlayer serverPlayer) {
            RemoteMenuProvider menuProvider = createRemoteMenuProvider(target);
            if (menuProvider == null) {
                return;
            }
            PacketDistributor.sendToPlayer(serverPlayer, new SetMatterNetworkTrackingPayload(false, target.getControllerTrackedPos(), ""));
            serverPlayer.openMenu(menuProvider, buffer -> {
                buffer.writeBlockPos(targetPos);
                buffer.writeBoolean(true);
            });
        }
    }

    public void handleSetTargetPylonId(Player player, BlockPos targetPos, int channel, int pylonId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        MatterPylonBlockEntity target = getConnectedTarget(serverLevel, targetPos);
        if (target == null || !target.supportsConfiguredChannel(channel)) {
            return;
        }
        target.setPylonId(channel, pylonId);
        refreshOverview();
    }

    public void handleCycleTargetMode(Player player, BlockPos targetPos, int channel, boolean backward) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        MatterPylonBlockEntity target = getConnectedTarget(serverLevel, targetPos);
        if (target == null || !target.supportsConfiguredChannel(channel)) {
            return;
        }
        if (backward) {
            target.cycleModeBackward(channel);
        } else {
            target.cycleMode(channel);
        }
        refreshOverview();
    }

    @Override
    protected void serverTick() {
        super.serverTick();
        if (level instanceof ServerLevel serverLevel && serverLevel.getGameTime() % OVERVIEW_REFRESH_INTERVAL == 0L) {
            refreshOverview();
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(ModBlocks.MATTER_NETWORK_CONTROLLER.get().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (!player.level().isClientSide()) {
            refreshOverview();
        }
        return new MatterNetworkControllerMenu(containerId, playerInventory, this, false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeOverviewTag(tag, registries);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeOverviewTag(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readOverviewTag(tag, registries);
    }

    private void refreshOverview() {
        if (level == null) {
            return;
        }

        HolderLookup.Provider registries = level.registryAccess();
        Set<BlockPos> members = collectConnectedNodePositions();
        List<ControllerEntry> entries = new ArrayList<>(members.size());
        for (BlockPos pos : members) {
            if (pos.equals(worldPosition)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MatterPylonBlockEntity node) {
                entries.add(new ControllerEntry(
                        pos.immutable(),
                        node.getDisplayName().getString(),
                        node.getNetworkColor(CHANNEL_ENERGY, 0).getId(),
                        node.getNetworkColor(CHANNEL_ENERGY, 1).getId(),
                        node.getNetworkColor(CHANNEL_ENERGY, 2).getId(),
                        node.getNetworkColor(CHANNEL_ITEMS, 0).getId(),
                        node.getNetworkColor(CHANNEL_ITEMS, 1).getId(),
                        node.getNetworkColor(CHANNEL_ITEMS, 2).getId(),
                        node.getNetworkColor(CHANNEL_FLUIDS, 0).getId(),
                        node.getNetworkColor(CHANNEL_FLUIDS, 1).getId(),
                        node.getNetworkColor(CHANNEL_FLUIDS, 2).getId(),
                        node.getNetworkColor(CHANNEL_REDSTONE, 0).getId(),
                        node.getNetworkColor(CHANNEL_REDSTONE, 1).getId(),
                        node.getNetworkColor(CHANNEL_REDSTONE, 2).getId(),
                        getSupportedChannelMask(node),
                        hasRecentTransfers(node),
                        node.getLinkedNodePositions().size(),
                        node.getMode(CHANNEL_ENERGY).ordinal(),
                        node.getMode(CHANNEL_ITEMS).ordinal(),
                        node.getMode(CHANNEL_FLUIDS).ordinal(),
                        node.getMode(CHANNEL_REDSTONE).ordinal(),
                        node.getPylonId(CHANNEL_ENERGY),
                        node.getPylonId(CHANNEL_ITEMS),
                        node.getPylonId(CHANNEL_FLUIDS),
                        node.getPylonId(CHANNEL_REDSTONE),
                        node.getTransferDisplayAmount(CHANNEL_ENERGY),
                        node.getTransferDisplayRole(CHANNEL_ENERGY).ordinal(),
                        node.getTransferDisplayAmount(CHANNEL_ITEMS),
                        node.getTransferDisplayRole(CHANNEL_ITEMS).ordinal(),
                        node.getTransferDisplayAmount(CHANNEL_FLUIDS),
                        node.getTransferDisplayRole(CHANNEL_FLUIDS).ordinal(),
                        node.getTransferDisplayAmount(CHANNEL_REDSTONE),
                        node.getTransferDisplayRole(CHANNEL_REDSTONE).ordinal(),
                        node.supportsUpgradeCrystals(),
                        saveStackTag(node.getCrystalHandler().getStackInSlot(0), registries),
                        saveStackTag(node.getCrystalHandler().getStackInSlot(1), registries),
                        saveStackTag(node.getCrystalHandler().getStackInSlot(2), registries)
                ));
            }
        }
        entries.sort(Comparator.comparing(ControllerEntry::displayName).thenComparing(entry -> entry.pos().asLong()));

        if (!Objects.equals(overviewEntries, entries)) {
            overviewEntries = List.copyOf(entries);
            setChanged();
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }
    }

    private static boolean hasRecentTransfers(MatterPylonBlockEntity node) {
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            if (node.getTransferDisplayAmount(channel) > 0) {
                return true;
            }
        }
        return false;
    }

    private static int getSupportedChannelMask(MatterPylonBlockEntity node) {
        int supportedChannelMask = 0;
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            if (node.supportsConfiguredChannel(channel)) {
                supportedChannelMask |= 1 << channel;
            }
        }
        return supportedChannelMask;
    }

    private MatterPylonBlockEntity getConnectedTarget(ServerLevel serverLevel, BlockPos targetPos) {
        if (!collectConnectedNodePositions().contains(targetPos)) {
            return null;
        }
        return serverLevel.getBlockEntity(targetPos) instanceof MatterPylonBlockEntity target ? target : null;
    }

    private String getLocatorLabel(MatterPylonBlockEntity target) {
        return target.getControllerTrackedDisplayName() + " [" + target.getControllerTrackedPos().toShortString() + "]";
    }

    private RemoteMenuProvider createRemoteMenuProvider(MatterPylonBlockEntity target) {
        if (target instanceof MatterNetworkControllerBlockEntity controller) {
            return new RemoteMenuProvider(controller.getDisplayName()) {
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new MatterNetworkControllerMenu(containerId, inventory, controller, true);
                }
            };
        }
        if (target instanceof EnergyCellBlockEntity energyCell) {
            return new RemoteMenuProvider(energyCell.getDisplayName()) {
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new EnergyCellMenu(containerId, inventory, energyCell, energyCell.getData(), true);
                }
            };
        }
        if (target instanceof FluidTankBlockEntity fluidTank) {
            return new RemoteMenuProvider(fluidTank.getDisplayName()) {
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new FluidTankMenu(containerId, inventory, fluidTank, fluidTank.getData(), true);
                }
            };
        }
        if (target instanceof MatterNetworkMonitorBlockEntity monitor) {
            return new RemoteMenuProvider(monitor.getDisplayName()) {
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new MatterNetworkMonitorMenu(containerId, inventory, monitor, true);
                }
            };
        }
        if (target instanceof MatterStorageBarrelBlockEntity storageBarrel) {
            return new RemoteMenuProvider(storageBarrel.getDisplayName()) {
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new MatterStorageBarrelMenu(containerId, inventory, storageBarrel, true);
                }
            };
        }
        if (target instanceof MatterBatteryPortBlockEntity batteryPort) {
            return new RemoteMenuProvider(batteryPort.getDisplayName()) {
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new MatterPylonMenu(containerId, inventory, batteryPort, batteryPort.getData(), true);
                }
            };
        }
        if (target.getClass() == MatterPylonBlockEntity.class) {
            return new RemoteMenuProvider(target.getDisplayName()) {
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new MatterPylonMenu(containerId, inventory, target, target.getData(), true);
                }
            };
        }
        return null;
    }

    private void writeOverviewTag(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (ControllerEntry entry : overviewEntries) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("x", entry.pos().getX());
            entryTag.putInt("y", entry.pos().getY());
            entryTag.putInt("z", entry.pos().getZ());
            entryTag.putString("display_name", entry.displayName());
            for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
                for (int index = 0; index < NETWORK_COLOR_CODE_PARTS; index++) {
                    entryTag.putInt(getOverviewColorTagName(channel, index), entry.getColorId(channel, index));
                }
            }
            entryTag.putInt("supported_channels", entry.supportedChannelMask());
            entryTag.putBoolean("active", entry.active());
            entryTag.putInt("link_count", entry.linkCount());
            entryTag.putInt("energy_mode", entry.energyMode());
            entryTag.putInt("item_mode", entry.itemMode());
            entryTag.putInt("fluid_mode", entry.fluidMode());
            entryTag.putInt("redstone_mode", entry.redstoneMode());
            entryTag.putInt("energy_id", entry.energyId());
            entryTag.putInt("item_id", entry.itemId());
            entryTag.putInt("fluid_id", entry.fluidId());
            entryTag.putInt("redstone_id", entry.redstoneId());
            entryTag.putInt("energy_amount", entry.energyAmount());
            entryTag.putInt("energy_role", entry.energyRole());
            entryTag.putInt("item_amount", entry.itemAmount());
            entryTag.putInt("item_role", entry.itemRole());
            entryTag.putInt("fluid_amount", entry.fluidAmount());
            entryTag.putInt("fluid_role", entry.fluidRole());
            entryTag.putInt("redstone_amount", entry.redstoneAmount());
            entryTag.putInt("redstone_role", entry.redstoneRole());
            entryTag.putBoolean("supports_upgrade_crystals", entry.supportsUpgradeCrystals());
            entryTag.put("crystal_1", entry.crystalOneStackTag().copy());
            entryTag.put("crystal_2", entry.crystalTwoStackTag().copy());
            entryTag.put("crystal_3", entry.crystalThreeStackTag().copy());
            entries.add(entryTag);
        }
        tag.put("controller_overview", entries);
    }

    private void readOverviewTag(CompoundTag tag, HolderLookup.Provider registries) {
        List<ControllerEntry> entries = new ArrayList<>();
        ListTag listTag = tag.getList("controller_overview", Tag.TAG_COMPOUND);
        for (Tag entry : listTag) {
            if (entry instanceof CompoundTag entryTag) {
                entries.add(new ControllerEntry(
                        new BlockPos(entryTag.getInt("x"), entryTag.getInt("y"), entryTag.getInt("z")),
                        entryTag.getString("display_name"),
                        readOverviewColorId(entryTag, CHANNEL_ENERGY, 0),
                        readOverviewColorId(entryTag, CHANNEL_ENERGY, 1),
                        readOverviewColorId(entryTag, CHANNEL_ENERGY, 2),
                        readOverviewColorId(entryTag, CHANNEL_ITEMS, 0),
                        readOverviewColorId(entryTag, CHANNEL_ITEMS, 1),
                        readOverviewColorId(entryTag, CHANNEL_ITEMS, 2),
                        readOverviewColorId(entryTag, CHANNEL_FLUIDS, 0),
                        readOverviewColorId(entryTag, CHANNEL_FLUIDS, 1),
                        readOverviewColorId(entryTag, CHANNEL_FLUIDS, 2),
                        readOverviewColorId(entryTag, CHANNEL_REDSTONE, 0),
                        readOverviewColorId(entryTag, CHANNEL_REDSTONE, 1),
                        readOverviewColorId(entryTag, CHANNEL_REDSTONE, 2),
                        entryTag.contains("supported_channels") ? entryTag.getInt("supported_channels") : (1 << CHANNEL_COUNT) - 1,
                        entryTag.getBoolean("active"),
                        entryTag.getInt("link_count"),
                        sanitizeModeOrdinal(entryTag.contains("energy_mode") ? entryTag.getInt("energy_mode") : PylonMode.IMPORT_EXPORT.ordinal()),
                        sanitizeModeOrdinal(entryTag.contains("item_mode") ? entryTag.getInt("item_mode") : PylonMode.IMPORT_EXPORT.ordinal()),
                        sanitizeModeOrdinal(entryTag.contains("fluid_mode") ? entryTag.getInt("fluid_mode") : PylonMode.IMPORT_EXPORT.ordinal()),
                        sanitizeModeOrdinal(entryTag.contains("redstone_mode") ? entryTag.getInt("redstone_mode") : PylonMode.IMPORT_EXPORT.ordinal()),
                        entryTag.contains("energy_id") ? entryTag.getInt("energy_id") : DEFAULT_PYLON_ID,
                        entryTag.contains("item_id") ? entryTag.getInt("item_id") : DEFAULT_PYLON_ID,
                        entryTag.contains("fluid_id") ? entryTag.getInt("fluid_id") : DEFAULT_PYLON_ID,
                        entryTag.contains("redstone_id") ? entryTag.getInt("redstone_id") : DEFAULT_PYLON_ID,
                        entryTag.getInt("energy_amount"),
                        entryTag.getInt("energy_role"),
                        entryTag.getInt("item_amount"),
                        entryTag.getInt("item_role"),
                        entryTag.getInt("fluid_amount"),
                        entryTag.getInt("fluid_role"),
                        entryTag.getInt("redstone_amount"),
                        entryTag.getInt("redstone_role"),
                        entryTag.getBoolean("supports_upgrade_crystals"),
                        entryTag.contains("crystal_1", Tag.TAG_COMPOUND) ? entryTag.getCompound("crystal_1").copy() : saveStackTag(net.minecraft.world.item.ItemStack.EMPTY, registries),
                        entryTag.contains("crystal_2", Tag.TAG_COMPOUND) ? entryTag.getCompound("crystal_2").copy() : saveStackTag(net.minecraft.world.item.ItemStack.EMPTY, registries),
                        entryTag.contains("crystal_3", Tag.TAG_COMPOUND) ? entryTag.getCompound("crystal_3").copy() : saveStackTag(net.minecraft.world.item.ItemStack.EMPTY, registries)
                ));
            }
        }
        overviewEntries = List.copyOf(entries);
    }

    private static CompoundTag saveStackTag(net.minecraft.world.item.ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!stack.isEmpty()) {
            stack.save(registries, tag);
        }
        return tag;
    }

    public record ControllerEntry(
            BlockPos pos,
            String displayName,
            int energyColorOneId,
            int energyColorTwoId,
            int energyColorThreeId,
            int itemColorOneId,
            int itemColorTwoId,
            int itemColorThreeId,
            int fluidColorOneId,
            int fluidColorTwoId,
            int fluidColorThreeId,
            int redstoneColorOneId,
            int redstoneColorTwoId,
            int redstoneColorThreeId,
            int supportedChannelMask,
            boolean active,
            int linkCount,
            int energyMode,
            int itemMode,
            int fluidMode,
            int redstoneMode,
            int energyId,
            int itemId,
            int fluidId,
            int redstoneId,
            int energyAmount,
            int energyRole,
            int itemAmount,
            int itemRole,
            int fluidAmount,
            int fluidRole,
            int redstoneAmount,
            int redstoneRole,
            boolean supportsUpgradeCrystals,
            CompoundTag crystalOneStackTag,
            CompoundTag crystalTwoStackTag,
            CompoundTag crystalThreeStackTag
    ) {
        public int getColorId(int channel, int index) {
            return switch (channel) {
                case CHANNEL_ENERGY -> getEnergyColorId(index);
                case CHANNEL_ITEMS -> getItemColorId(index);
                case CHANNEL_FLUIDS -> getFluidColorId(index);
                case CHANNEL_REDSTONE -> getRedstoneColorId(index);
                default -> net.minecraft.world.item.DyeColor.WHITE.getId();
            };
        }

        public ControllerEntry withColor(int channel, int index, int colorId) {
            int sanitizedColorId = net.minecraft.world.item.DyeColor.byId(colorId).getId();
            return new ControllerEntry(
                    pos,
                    displayName,
                    channel == CHANNEL_ENERGY && index == 0 ? sanitizedColorId : energyColorOneId,
                    channel == CHANNEL_ENERGY && index == 1 ? sanitizedColorId : energyColorTwoId,
                    channel == CHANNEL_ENERGY && index == 2 ? sanitizedColorId : energyColorThreeId,
                    channel == CHANNEL_ITEMS && index == 0 ? sanitizedColorId : itemColorOneId,
                    channel == CHANNEL_ITEMS && index == 1 ? sanitizedColorId : itemColorTwoId,
                    channel == CHANNEL_ITEMS && index == 2 ? sanitizedColorId : itemColorThreeId,
                    channel == CHANNEL_FLUIDS && index == 0 ? sanitizedColorId : fluidColorOneId,
                    channel == CHANNEL_FLUIDS && index == 1 ? sanitizedColorId : fluidColorTwoId,
                    channel == CHANNEL_FLUIDS && index == 2 ? sanitizedColorId : fluidColorThreeId,
                    channel == CHANNEL_REDSTONE && index == 0 ? sanitizedColorId : redstoneColorOneId,
                    channel == CHANNEL_REDSTONE && index == 1 ? sanitizedColorId : redstoneColorTwoId,
                    channel == CHANNEL_REDSTONE && index == 2 ? sanitizedColorId : redstoneColorThreeId,
                    supportedChannelMask,
                    active,
                    linkCount,
                    energyMode,
                    itemMode,
                    fluidMode,
                    redstoneMode,
                    energyId,
                    itemId,
                    fluidId,
                    redstoneId,
                    energyAmount,
                    energyRole,
                    itemAmount,
                    itemRole,
                    fluidAmount,
                    fluidRole,
                    redstoneAmount,
                    redstoneRole,
                    supportsUpgradeCrystals,
                    crystalOneStackTag,
                    crystalTwoStackTag,
                    crystalThreeStackTag
            );
        }

        private int getEnergyColorId(int index) {
            return switch (index) {
                case 0 -> energyColorOneId;
                case 1 -> energyColorTwoId;
                case 2 -> energyColorThreeId;
                default -> net.minecraft.world.item.DyeColor.WHITE.getId();
            };
        }

        private int getItemColorId(int index) {
            return switch (index) {
                case 0 -> itemColorOneId;
                case 1 -> itemColorTwoId;
                case 2 -> itemColorThreeId;
                default -> net.minecraft.world.item.DyeColor.WHITE.getId();
            };
        }

        private int getFluidColorId(int index) {
            return switch (index) {
                case 0 -> fluidColorOneId;
                case 1 -> fluidColorTwoId;
                case 2 -> fluidColorThreeId;
                default -> net.minecraft.world.item.DyeColor.WHITE.getId();
            };
        }

        private int getRedstoneColorId(int index) {
            return switch (index) {
                case 0 -> redstoneColorOneId;
                case 1 -> redstoneColorTwoId;
                case 2 -> redstoneColorThreeId;
                default -> net.minecraft.world.item.DyeColor.WHITE.getId();
            };
        }
    }

    private static int sanitizeModeOrdinal(int ordinal) {
        return Math.max(0, Math.min(PylonMode.values().length - 1, ordinal));
    }

    private static int readOverviewColorId(CompoundTag entryTag, int channel, int index) {
        String colorTag = getOverviewColorTagName(channel, index);
        if (entryTag.contains(colorTag)) {
            return net.minecraft.world.item.DyeColor.byId(entryTag.getInt(colorTag)).getId();
        }
        String legacyColorTag = getLegacyOverviewColorTagName(index);
        if (entryTag.contains(legacyColorTag)) {
            return net.minecraft.world.item.DyeColor.byId(entryTag.getInt(legacyColorTag)).getId();
        }
        return net.minecraft.world.item.DyeColor.WHITE.getId();
    }

    private static String getOverviewColorTagName(int channel, int index) {
        return "channel_" + (channel + 1) + "_color_" + (index + 1);
    }

    private static String getLegacyOverviewColorTagName(int index) {
        return "color_" + (index + 1);
    }

    private abstract static class RemoteMenuProvider implements net.minecraft.world.MenuProvider {
        private final Component displayName;

        private RemoteMenuProvider(Component displayName) {
            this.displayName = displayName;
        }

        @Override
        public Component getDisplayName() {
            return displayName;
        }
    }
}

