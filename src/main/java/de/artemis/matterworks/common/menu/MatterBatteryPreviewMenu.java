package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MatterBatteryPreviewMenu extends AbstractBaseMenu implements NamedBlockMenu {
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
        addPlayerInventorySlots(inventory, 8, 84);
    }

    private void addPlayerHotbar(Inventory inventory) {
        addPlayerHotbarSlots(inventory, 8, 142);
    }

    private static MatterBatteryCoreBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        return MenuHelper.resolveBlockEntity(inventory, pos, MatterBatteryCoreBlockEntity.class, "Matter Battery Core");
    }
}
