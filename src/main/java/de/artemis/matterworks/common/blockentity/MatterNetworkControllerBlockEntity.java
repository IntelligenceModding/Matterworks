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
                        node.getNetworkColor(0).getId(),
                        node.getNetworkColor(1).getId(),
                        node.getNetworkColor(2).getId(),
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
            entryTag.putInt("color_1", entry.colorOneId());
            entryTag.putInt("color_2", entry.colorTwoId());
            entryTag.putInt("color_3", entry.colorThreeId());
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
                        entryTag.contains("color_1") ? entryTag.getInt("color_1") : net.minecraft.world.item.DyeColor.WHITE.getId(),
                        entryTag.contains("color_2") ? entryTag.getInt("color_2") : net.minecraft.world.item.DyeColor.WHITE.getId(),
                        entryTag.contains("color_3") ? entryTag.getInt("color_3") : net.minecraft.world.item.DyeColor.WHITE.getId(),
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
            int colorOneId,
            int colorTwoId,
            int colorThreeId,
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
    }

    private static int sanitizeModeOrdinal(int ordinal) {
        return Math.max(0, Math.min(PylonMode.values().length - 1, ordinal));
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

