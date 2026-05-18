package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.MatterNetworkControllerBlockEntity;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class MatterNetworkControllerMenu extends AbstractContainerMenu {
    private final MatterNetworkControllerBlockEntity blockEntity;
    private final boolean remoteAccess;

    public MatterNetworkControllerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()), extraData.readableBytes() > 0 && extraData.readBoolean());
    }

    public MatterNetworkControllerMenu(int containerId, Inventory playerInventory, MatterNetworkControllerBlockEntity blockEntity, boolean remoteAccess) {
        super(ModMenuTypes.MATTER_NETWORK_CONTROLLER.get(), containerId);
        this.blockEntity = blockEntity;
        this.remoteAccess = remoteAccess;
    }

    public MatterNetworkControllerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.isControllerMenuStillValid(player, remoteAccess);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static MatterNetworkControllerBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof MatterNetworkControllerBlockEntity controllerBlockEntity) {
            return controllerBlockEntity;
        }
        throw new IllegalStateException("Missing Matter Network Controller block entity at " + pos);
    }
}
