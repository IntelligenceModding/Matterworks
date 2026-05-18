package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.MatterNetworkMonitorBlockEntity;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class MatterNetworkMonitorMenu extends AbstractContainerMenu implements NamedBlockMenu {
    private final MatterNetworkMonitorBlockEntity blockEntity;
    private final boolean remoteAccess;

    public MatterNetworkMonitorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()), extraData.readableBytes() > 0 && extraData.readBoolean());
    }

    public MatterNetworkMonitorMenu(int containerId, Inventory playerInventory, MatterNetworkMonitorBlockEntity blockEntity, boolean remoteAccess) {
        super(ModMenuTypes.MATTER_NETWORK_MONITOR.get(), containerId);
        this.blockEntity = blockEntity;
        this.remoteAccess = remoteAccess;
    }

    public MatterNetworkMonitorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public boolean isRemoteAccess() {
        return remoteAccess;
    }

    @Override
    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
    }

    @Override
    public String getBlockDisplayName() {
        return blockEntity.getDisplayName().getString();
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.isMonitorMenuStillValid(player, remoteAccess);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static MatterNetworkMonitorBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof MatterNetworkMonitorBlockEntity monitorBlockEntity) {
            return monitorBlockEntity;
        }
        throw new IllegalStateException("Missing Matter Network Monitor block entity at " + pos);
    }
}
