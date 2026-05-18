package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.menu.MatterEnergyCellMenu;
import de.artemis.matterworks.common.menu.MatterFluidTankMenu;
import de.artemis.matterworks.common.menu.MatterNetworkControllerMenu;
import de.artemis.matterworks.common.menu.MatterNetworkMonitorMenu;
import de.artemis.matterworks.common.menu.MatterPylonMenu;
import de.artemis.matterworks.common.menu.MatterStorageBarrelMenu;
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

    public void handleAction(Player player, BlockPos targetPos, int action) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!collectConnectedNodePositions().contains(targetPos)) {
            return;
        }
        if (!(serverLevel.getBlockEntity(targetPos) instanceof MatterPylonBlockEntity target)) {
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
        writeOverviewTag(tag);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeOverviewTag(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readOverviewTag(tag);
    }

    private void refreshOverview() {
        if (level == null) {
            return;
        }

        Set<BlockPos> members = collectConnectedNodePositions();
        List<ControllerEntry> entries = new ArrayList<>(members.size());
        for (BlockPos pos : members) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MatterPylonBlockEntity node) {
                entries.add(new ControllerEntry(
                        pos.immutable(),
                        node.getDisplayName().getString(),
                        node.getNetworkColor(0).getId(),
                        node.getNetworkColor(1).getId(),
                        node.getNetworkColor(2).getId(),
                        hasRecentTransfers(node),
                        node.getLinkedNodePositions().size(),
                        node.getTransferDisplayAmount(CHANNEL_ENERGY),
                        node.getTransferDisplayRole(CHANNEL_ENERGY).ordinal(),
                        node.getTransferDisplayAmount(CHANNEL_ITEMS),
                        node.getTransferDisplayRole(CHANNEL_ITEMS).ordinal(),
                        node.getTransferDisplayAmount(CHANNEL_FLUIDS),
                        node.getTransferDisplayRole(CHANNEL_FLUIDS).ordinal(),
                        node.getTransferDisplayAmount(CHANNEL_REDSTONE),
                        node.getTransferDisplayRole(CHANNEL_REDSTONE).ordinal()
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
        if (target instanceof MatterEnergyCellBlockEntity energyCell) {
            return new RemoteMenuProvider(energyCell.getDisplayName()) {
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new MatterEnergyCellMenu(containerId, inventory, energyCell, energyCell.getData(), true);
                }
            };
        }
        if (target instanceof MatterFluidTankBlockEntity fluidTank) {
            return new RemoteMenuProvider(fluidTank.getDisplayName()) {
                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new MatterFluidTankMenu(containerId, inventory, fluidTank, fluidTank.getData(), true);
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

    private void writeOverviewTag(CompoundTag tag) {
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
            entryTag.putBoolean("active", entry.active());
            entryTag.putInt("link_count", entry.linkCount());
            entryTag.putInt("energy_amount", entry.energyAmount());
            entryTag.putInt("energy_role", entry.energyRole());
            entryTag.putInt("item_amount", entry.itemAmount());
            entryTag.putInt("item_role", entry.itemRole());
            entryTag.putInt("fluid_amount", entry.fluidAmount());
            entryTag.putInt("fluid_role", entry.fluidRole());
            entryTag.putInt("redstone_amount", entry.redstoneAmount());
            entryTag.putInt("redstone_role", entry.redstoneRole());
            entries.add(entryTag);
        }
        tag.put("controller_overview", entries);
    }

    private void readOverviewTag(CompoundTag tag) {
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
                        entryTag.getBoolean("active"),
                        entryTag.getInt("link_count"),
                        entryTag.getInt("energy_amount"),
                        entryTag.getInt("energy_role"),
                        entryTag.getInt("item_amount"),
                        entryTag.getInt("item_role"),
                        entryTag.getInt("fluid_amount"),
                        entryTag.getInt("fluid_role"),
                        entryTag.getInt("redstone_amount"),
                        entryTag.getInt("redstone_role")
                ));
            }
        }
        overviewEntries = List.copyOf(entries);
    }

    public record ControllerEntry(
            BlockPos pos,
            String displayName,
            int colorOneId,
            int colorTwoId,
            int colorThreeId,
            boolean active,
            int linkCount,
            int energyAmount,
            int energyRole,
            int itemAmount,
            int itemRole,
            int fluidAmount,
            int fluidRole,
            int redstoneAmount,
            int redstoneRole
    ) {
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
