package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.AbstractMatterMachineBlockEntity;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.menu.slot.BucketInputSlot;
import de.artemis.matterworks.common.menu.slot.CrystalSlot;
import de.artemis.matterworks.common.menu.slot.EnergyInputSlot;
import de.artemis.matterworks.common.menu.slot.InputSlot;
import de.artemis.matterworks.common.menu.slot.OutputOnlySlot;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public abstract class AbstractMatterMachineMenu extends AbstractContainerMenu {
    protected final AbstractMatterMachineBlockEntity blockEntity;
    protected final ContainerData data;
    protected final int machineSlotCount;
    protected final int playerInventoryStart;
    protected final int playerInventoryEnd;
    protected final int playerHotbarStart;
    protected final int playerHotbarEnd;

    protected AbstractMatterMachineMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, AbstractMatterMachineBlockEntity blockEntity, ContainerData data) {
        super(menuType, containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addMachineSlots();
        this.machineSlotCount = this.slots.size();
        this.playerInventoryStart = this.machineSlotCount;
        this.playerInventoryEnd = this.playerInventoryStart + 27;
        this.playerHotbarStart = this.playerInventoryEnd;
        this.playerHotbarEnd = this.playerHotbarStart + 9;
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    protected abstract void addMachineSlots();

    protected void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
    }

    protected void addPlayerHotbar(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            this.addSlot(new Slot(inventory, slot, 8 + slot * 18, 142));
        }
    }

    protected SlotItemHandler createMachineSlot(int slot, int x, int y) {
        return new InputSlot(blockEntity.getItemHandler(), slot, x, y);
    }

    protected SlotItemHandler createEnergyInputSlot(int slot, int x, int y) {
        return new EnergyInputSlot(blockEntity.getItemHandler(), slot, x, y);
    }

    protected SlotItemHandler createCrystalSlot(int slot, int x, int y) {
        return new CrystalSlot(blockEntity.getItemHandler(), slot, x, y);
    }

    protected SlotItemHandler createBucketInputSlot(int slot, int x, int y) {
        return new BucketInputSlot(blockEntity.getItemHandler(), slot, x, y);
    }

    protected SlotItemHandler createOutputOnlySlot(int slot, int x, int y) {
        return new OutputOnlySlot(blockEntity.getItemHandler(), slot, x, y);
    }

    public boolean isProcessing() {
        return data.get(AbstractMatterMachineBlockEntity.DATA_PROGRESS) > 0;
    }

    public int getProgress() {
        return data.get(AbstractMatterMachineBlockEntity.DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return data.get(AbstractMatterMachineBlockEntity.DATA_MAX_PROGRESS);
    }

    public int getScaledProgress(int width) {
        int progress = getProgress();
        int maxProgress = getMaxProgress();
        if (progress <= 0 || maxProgress <= 0) {
            return 0;
        }
        return progress * width / maxProgress;
    }

    public int getFluidAmount() {
        return data.get(AbstractMatterMachineBlockEntity.DATA_FLUID_AMOUNT);
    }

    public int getFluidCapacity() {
        return data.get(AbstractMatterMachineBlockEntity.DATA_FLUID_CAPACITY);
    }

    public int getScaledFluidAmount(int height) {
        int fluidAmount = getFluidAmount();
        int fluidCapacity = getFluidCapacity();
        if (fluidAmount <= 0 || fluidCapacity <= 0) {
            return 0;
        }
        return Math.max(1, fluidAmount * height / fluidCapacity);
    }

    public int getEnergyStored() {
        return data.get(AbstractMatterMachineBlockEntity.DATA_ENERGY);
    }

    public int getEnergyCapacity() {
        return data.get(AbstractMatterMachineBlockEntity.DATA_ENERGY_CAPACITY);
    }

    public int getScaledEnergyAmount(int height) {
        int energyStored = getEnergyStored();
        int energyCapacity = getEnergyCapacity();
        if (energyStored <= 0 || energyCapacity <= 0) {
            return 0;
        }
        return Math.max(1, energyStored * height / energyCapacity);
    }

    public int getProgressBarColor() {
        return 0xFF000000 | data.get(AbstractMatterMachineBlockEntity.DATA_PROGRESS_COLOR);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copiedStack = ItemStack.EMPTY;
        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        copiedStack = sourceStack.copy();

        if (index == AbstractMatterMachineBlockEntity.ENERGY_ITEM_OUTPUT_SLOT
                || index == AbstractMatterMachineBlockEntity.INVALID_OUTPUT_SLOT
                || index == AbstractMatterMachineBlockEntity.FLUID_BUCKET_OUTPUT_SLOT) {
            if (!this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
            sourceSlot.onQuickCraft(sourceStack, copiedStack);
        } else if (index >= playerInventoryStart) {
            if (PowerCrystalEffects.isPowerCrystal(sourceStack)) {
                if (!this.moveItemStackTo(sourceStack, AbstractMatterMachineBlockEntity.CRYSTAL_SLOT, AbstractMatterMachineBlockEntity.CRYSTAL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (EnergyItemHelper.canProvideEnergy(sourceStack)) {
                if (!this.moveItemStackTo(sourceStack, AbstractMatterMachineBlockEntity.ENERGY_ITEM_INPUT_SLOT, AbstractMatterMachineBlockEntity.ENERGY_ITEM_INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(sourceStack, AbstractMatterMachineBlockEntity.ENERGY_ITEM_INPUT_SLOT, AbstractMatterMachineBlockEntity.ENERGY_ITEM_INPUT_SLOT + 1, false)
                    && !this.moveItemStackTo(sourceStack, AbstractMatterMachineBlockEntity.INPUT_SLOT, AbstractMatterMachineBlockEntity.INPUT_SLOT + 1, false)
                    && !this.moveItemStackTo(sourceStack, AbstractMatterMachineBlockEntity.FLUID_BUCKET_INPUT_SLOT, AbstractMatterMachineBlockEntity.FLUID_BUCKET_INPUT_SLOT + 1, false)) {
                if (index < playerHotbarStart) {
                    if (!this.moveItemStackTo(sourceStack, playerHotbarStart, playerHotbarEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (!this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarEnd, false)) {
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
}
