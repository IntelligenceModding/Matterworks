package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.AbstractMatterMachineBlockEntity;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.fluid.FluidItemHelper;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.menu.slot.BucketInputSlot;
import de.artemis.matterworks.common.menu.slot.CrystalSlot;
import de.artemis.matterworks.common.menu.slot.EnergyInputSlot;
import de.artemis.matterworks.common.menu.slot.InputSlot;
import de.artemis.matterworks.common.menu.slot.OutputOnlySlot;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.List;

public abstract class AbstractMatterMachineMenu extends AbstractBaseMenu implements NamedBlockMenu, SideConfigMenuAccess {
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
        addPlayerInventorySlots(inventory, 8, 84);
    }

    protected void addPlayerHotbar(Inventory inventory) {
        addPlayerHotbarSlots(inventory, 8, 142);
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

    @Override
    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
    }

    @Override
    public String getBlockDisplayName() {
        return blockEntity.getDisplayName().getString();
    }

    @Override
    public boolean supportsSideConfigType(SideConfigType type) {
        return blockEntity.supportsSideConfigType(type);
    }

    @Override
    public boolean supportsSideConfigInput(SideConfigType type) {
        return blockEntity.supportsSideConfigInput(type);
    }

    @Override
    public boolean supportsSideConfigOutput(SideConfigType type) {
        return blockEntity.supportsSideConfigOutput(type);
    }

    @Override
    public SideAccessMode getSideAccessMode(SideConfigType type, net.minecraft.core.Direction side) {
        return blockEntity.getSideAccessMode(type, side);
    }

    @Override
    public List<SideAccessMode> getAllowedSideAccessModes(SideConfigType type) {
        return blockEntity.getAllowedSideAccessModes(type);
    }

    @Override
    public String getSideAccessModeLabel(SideConfigType type, net.minecraft.core.Direction side, SideAccessMode mode) {
        return blockEntity.getSideAccessModeLabel(type, side, mode);
    }

    @Override
    public String getSideAccessModeShortLabel(SideConfigType type, net.minecraft.core.Direction side, SideAccessMode mode) {
        return blockEntity.getSideAccessModeShortLabel(type, side, mode);
    }

    @Override
    public net.minecraft.core.Direction getSideConfigFrontFacing() {
        return SideConfigOrientation.resolveFrontFacing(blockEntity.getBlockState());
    }

    @Override
    public ItemStack getPrimaryTabIcon() {
        return blockEntity.getBlockState().getBlock().asItem().getDefaultInstance();
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

    public FluidStack getFluidStack() {
        return blockEntity.getFluidTank().getFluid().copy();
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

        int energyOutputMenuSlot = findMenuSlotIndexForContainerSlot(AbstractMatterMachineBlockEntity.ENERGY_ITEM_OUTPUT_SLOT);
        int invalidOutputMenuSlot = findMenuSlotIndexForContainerSlot(AbstractMatterMachineBlockEntity.INVALID_OUTPUT_SLOT);
        int fluidOutputMenuSlot = findMenuSlotIndexForContainerSlot(AbstractMatterMachineBlockEntity.FLUID_BUCKET_OUTPUT_SLOT);

        if (index == energyOutputMenuSlot
                || index == invalidOutputMenuSlot
                || index == fluidOutputMenuSlot) {
            if (!this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
            sourceSlot.onQuickCraft(sourceStack, copiedStack);
        } else if (index >= playerInventoryStart) {
            if (PowerCrystalEffects.isPowerCrystal(sourceStack)) {
                if (!moveToMachineContainerSlot(sourceStack, AbstractMatterMachineBlockEntity.CRYSTAL_SLOT)) {
                    return ItemStack.EMPTY;
                }
            } else if (EnergyItemHelper.canProvideEnergy(sourceStack)) {
                if (!moveToMachineContainerSlot(sourceStack, AbstractMatterMachineBlockEntity.ENERGY_ITEM_INPUT_SLOT)) {
                    return ItemStack.EMPTY;
                }
            } else if (FluidItemHelper.isFluidItem(sourceStack)) {
                if (!moveToMachineContainerSlot(sourceStack, AbstractMatterMachineBlockEntity.FLUID_BUCKET_INPUT_SLOT)) {
                    if (!moveToMachineContainerSlot(sourceStack, AbstractMatterMachineBlockEntity.INPUT_SLOT)
                            && !moveWithinPlayerInventory(index, sourceStack)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!moveToMachineContainerSlot(sourceStack, AbstractMatterMachineBlockEntity.INPUT_SLOT)) {
                if (!moveWithinPlayerInventory(index, sourceStack)) {
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

    protected boolean moveWithinPlayerInventory(int index, ItemStack sourceStack) {
        if (index < playerHotbarStart) {
            return this.moveItemStackTo(sourceStack, playerHotbarStart, playerHotbarEnd, false);
        }
        return this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarStart, false);
    }

    protected boolean moveToMachineContainerSlot(ItemStack sourceStack, int containerSlot) {
        return moveToContainerSlot(sourceStack, machineSlotCount, containerSlot);
    }

    protected boolean moveToMachineContainerSlotRange(ItemStack sourceStack, int containerSlotStart, int containerSlotCount) {
        return moveToContainerSlotRange(sourceStack, machineSlotCount, containerSlotStart, containerSlotCount);
    }

    protected int findMenuSlotIndexForContainerSlot(int containerSlot) {
        return findSlotIndexForContainerSlot(machineSlotCount, containerSlot);
    }
}
