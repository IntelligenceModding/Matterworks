package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MatterBatteryPreviewMenu extends AbstractContainerMenu implements NamedBlockMenu {
    private final MatterBatteryCoreBlockEntity blockEntity;
    private final ContainerData data;

    public MatterBatteryPreviewMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()), new SimpleContainerData(MatterBatteryCoreBlockEntity.DATA_COUNT));
    }

    public MatterBatteryPreviewMenu(int containerId, Inventory playerInventory, MatterBatteryCoreBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MATTER_BATTERY_PREVIEW.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    public MatterBatteryCoreBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
    }

    @Override
    public String getBlockDisplayName() {
        return blockEntity.getDisplayName().getString();
    }

    public boolean isFormed() {
        return data.get(MatterBatteryCoreBlockEntity.DATA_FORMED) > 0;
    }

    public int getCellCount() {
        return data.get(MatterBatteryCoreBlockEntity.DATA_CELLS);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.isMenuStillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            this.addSlot(new Slot(inventory, slot, 8 + slot * 18, 142));
        }
    }

    private static MatterBatteryCoreBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof MatterBatteryCoreBlockEntity blockEntity) {
            return blockEntity;
        }
        throw new IllegalStateException("Missing Matter Battery Core block entity at " + pos);
    }
}
