package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.MatterStorageBarrelBlockEntity;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MatterStorageBarrelMenu extends AbstractContainerMenu {
    public static final int ROWS = 6;
    public static final int SLOT_COUNT = ROWS * 9;
    private static final int PLAYER_INVENTORY_START = SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 9;

    private final MatterStorageBarrelBlockEntity blockEntity;

    public MatterStorageBarrelMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()));
    }

    public MatterStorageBarrelMenu(int containerId, Inventory playerInventory, MatterStorageBarrelBlockEntity blockEntity) {
        super(ModMenuTypes.MATTER_STORAGE_BARREL.get(), containerId);
        this.blockEntity = blockEntity;

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(blockEntity, column + row * 9, 8 + column * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 140 + row * 18));
            }
        }

        for (int slot = 0; slot < 9; slot++) {
            this.addSlot(new Slot(playerInventory, slot, 8 + slot * 18, 198));
        }
    }

    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copiedStack = sourceStack.copy();

        if (index < SLOT_COUNT) {
            if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(sourceStack, 0, SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        if (sourceStack.getCount() == copiedStack.getCount()) {
            return ItemStack.EMPTY;
        }

        sourceSlot.onTake(player, sourceStack);
        return copiedStack;
    }

    private static MatterStorageBarrelBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof MatterStorageBarrelBlockEntity storageBarrelBlockEntity) {
            return storageBarrelBlockEntity;
        }
        throw new IllegalStateException("Missing Matter Storage Barrel block entity at " + pos);
    }
}
