package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.MatterEnergyCellBlockEntity;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class MatterEnergyCellMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_START = 2;
    private static final int PLAYER_INVENTORY_END = 29;
    private static final int PLAYER_HOTBAR_START = 29;
    private static final int PLAYER_HOTBAR_END = 38;

    private final MatterEnergyCellBlockEntity blockEntity;
    private final ContainerData data;

    public MatterEnergyCellMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()), new SimpleContainerData(MatterEnergyCellBlockEntity.DATA_COUNT));
    }

    public MatterEnergyCellMenu(int containerId, Inventory playerInventory, MatterEnergyCellBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MATTER_ENERGY_CELL.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), MatterEnergyCellBlockEntity.DISCHARGE_SLOT, 53, 35));
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), MatterEnergyCellBlockEntity.CHARGE_SLOT, 107, 35));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
    }

    public int getEnergyStored() {
        return data.get(MatterEnergyCellBlockEntity.DATA_ENERGY);
    }

    public int getEnergyCapacity() {
        return data.get(MatterEnergyCellBlockEntity.DATA_ENERGY_CAPACITY);
    }

    public int getScaledEnergyAmount(int height) {
        int energyStored = getEnergyStored();
        int energyCapacity = getEnergyCapacity();
        if (energyStored <= 0 || energyCapacity <= 0) {
            return 0;
        }
        return Math.max(1, energyStored * height / energyCapacity);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.MATTER_ENERGY_CELL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copiedStack = sourceStack.copy();

        if (index < PLAYER_INVENTORY_START) {
            if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (EnergyItemHelper.getEnergyStorage(sourceStack) != null) {
            boolean moved = false;
            if (EnergyItemHelper.canProvideEnergy(sourceStack)) {
                moved = this.moveItemStackTo(sourceStack, MatterEnergyCellBlockEntity.DISCHARGE_SLOT, MatterEnergyCellBlockEntity.DISCHARGE_SLOT + 1, false);
            }
            if (!sourceStack.isEmpty() && EnergyItemHelper.canReceiveEnergy(sourceStack)) {
                moved = this.moveItemStackTo(sourceStack, MatterEnergyCellBlockEntity.CHARGE_SLOT, MatterEnergyCellBlockEntity.CHARGE_SLOT + 1, false) || moved;
            }
            if (!moved) {
                if (index < PLAYER_HOTBAR_START) {
                    if (!this.moveItemStackTo(sourceStack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (index < PLAYER_HOTBAR_START) {
            if (!this.moveItemStackTo(sourceStack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START, false)) {
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

    private static MatterEnergyCellBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof MatterEnergyCellBlockEntity energyCellBlockEntity) {
            return energyCellBlockEntity;
        }
        throw new IllegalStateException("Missing Matter Energy Cell block entity at " + pos);
    }
}
