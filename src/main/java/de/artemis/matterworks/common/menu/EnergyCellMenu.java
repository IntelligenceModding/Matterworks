package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.EnergyCellBlockEntity;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
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
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.items.SlotItemHandler;

public class EnergyCellMenu extends AbstractContainerMenu implements NamedBlockMenu, SideConfigMenuAccess {
    private static final int MACHINE_SLOT_COUNT = EnergyCellBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 9;

    private final EnergyCellBlockEntity blockEntity;
    private final ContainerData data;
    private final boolean remoteAccess;

    public EnergyCellMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                resolveBlockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(EnergyCellBlockEntity.DATA_COUNT),
                extraData.readableBytes() > 0 && extraData.readBoolean()
        );
    }

    public EnergyCellMenu(int containerId, Inventory playerInventory, EnergyCellBlockEntity blockEntity, ContainerData data) {
        this(containerId, playerInventory, blockEntity, data, false);
    }

    public EnergyCellMenu(int containerId, Inventory playerInventory, EnergyCellBlockEntity blockEntity, ContainerData data, boolean remoteAccess) {
        super(ModMenuTypes.MATTER_ENERGY_CELL.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;
        this.remoteAccess = remoteAccess;

        this.addSlot(new CrystalSlot(
                blockEntity.getItemHandler(),
                EnergyCellBlockEntity.SLOT_CRYSTAL,
                8,
                108
        ));
        for (int slot = 0; slot < EnergyCellBlockEntity.CHARGE_SLOT_COUNT; slot++) {
            this.addSlot(new SlotItemHandler(
                    blockEntity.getItemHandler(),
                    EnergyCellBlockEntity.CHARGE_SLOT_START + slot,
                    35 + slot * 18,
                    108
            ));
        }
        this.addSlot(new PowerBankSlot(
                blockEntity.getItemHandler(),
                EnergyCellBlockEntity.SLOT_POWER_BANK,
                152,
                108
        ));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
    }

    public EnergyCellBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public boolean isRemoteAccess() {
        return remoteAccess;
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
    public net.minecraft.core.Direction getSideConfigFrontFacing() {
        return SideConfigOrientation.resolveFrontFacing(blockEntity.getBlockState());
    }

    @Override
    public ItemStack getPrimaryTabIcon() {
        return blockEntity.getBlockState().getBlock().asItem().getDefaultInstance();
    }

    @Override
    public boolean hasNetworkTab() {
        return true;
    }

    public int getEnergyStored() {
        return data.get(EnergyCellBlockEntity.DATA_ENERGY);
    }

    public int getEnergyCapacity() {
        return data.get(EnergyCellBlockEntity.DATA_ENERGY_CAPACITY);
    }

    public int getScaledEnergyAmount(int height) {
        int energyStored = getEnergyStored();
        int energyCapacity = getEnergyCapacity();
        if (energyStored <= 0 || energyCapacity <= 0) {
            return 0;
        }
        return Math.max(1, energyStored * height / energyCapacity);
    }

    public EnergyCellBlockEntity.TransferHistorySample getHistorySample(int index) {
        return blockEntity.getHistorySample(index);
    }

    public int getHistorySize() {
        return blockEntity.getHistorySize();
    }

    public int getHistoryCapacity() {
        return blockEntity.getHistoryCapacity();
    }

    public int getCurrentInputRate() {
        return blockEntity.getCurrentInputRate();
    }

    public int getCurrentOutputRate() {
        return blockEntity.getCurrentOutputRate();
    }

    public int getCurrentTransitRate() {
        return blockEntity.getCurrentTransitRate();
    }

    public int getCurrentTotalTransferRate() {
        return blockEntity.getCurrentTotalTransferRate();
    }

    public int getCurrentNetTransferRate() {
        return getCurrentInputRate() - getCurrentOutputRate();
    }

    public DyeColor getNetworkColor(int index) {
        return blockEntity.getNetworkColor(index);
    }

    @Override
    public boolean stillValid(Player player) {
        return remoteAccess
                ? player.level().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
                : stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.MATTER_ENERGY_CELL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copiedStack = sourceStack.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (de.artemis.matterworks.common.upgrade.PowerCrystalEffects.isPowerCrystal(sourceStack)) {
            if (!this.moveItemStackTo(sourceStack, EnergyCellBlockEntity.SLOT_CRYSTAL, EnergyCellBlockEntity.SLOT_CRYSTAL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isUsableEnergyItem(sourceStack)) {
            boolean moved = false;
            if (EnergyItemHelper.canProvideEnergy(sourceStack) && EnergyItemHelper.isPowerBank(sourceStack)) {
                moved = this.moveItemStackTo(
                        sourceStack,
                        EnergyCellBlockEntity.SLOT_POWER_BANK,
                        EnergyCellBlockEntity.SLOT_POWER_BANK + 1,
                        false
                );
            }
            if (!sourceStack.isEmpty() && EnergyItemHelper.canReceiveEnergy(sourceStack)) {
                moved = this.moveItemStackTo(
                        sourceStack,
                        EnergyCellBlockEntity.CHARGE_SLOT_START,
                        EnergyCellBlockEntity.CHARGE_SLOT_START + EnergyCellBlockEntity.CHARGE_SLOT_COUNT,
                        false
                ) || moved;
            }
            if (!moved && EnergyItemHelper.canProvideEnergy(sourceStack)) {
                moved = this.moveItemStackTo(
                        sourceStack,
                        EnergyCellBlockEntity.SLOT_POWER_BANK,
                        EnergyCellBlockEntity.SLOT_POWER_BANK + 1,
                        false
                );
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
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 140 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            this.addSlot(new Slot(inventory, slot, 8 + slot * 18, 198));
        }
    }

    private static boolean isUsableEnergyItem(ItemStack stack) {
        return EnergyItemHelper.getEnergyStorage(stack) != null
                && !(stack.getItem() instanceof de.artemis.matterworks.common.item.PowerCrystalItem);
    }

    private static final class CrystalSlot extends SlotItemHandler {
        private CrystalSlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int slot, int x, int y) {
            super(itemHandler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return de.artemis.matterworks.common.upgrade.PowerCrystalEffects.isPowerCrystal(stack);
        }
    }

    private static final class PowerBankSlot extends SlotItemHandler {
        private PowerBankSlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int slot, int x, int y) {
            super(itemHandler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return EnergyItemHelper.canProvideEnergy(stack);
        }
    }

    private static EnergyCellBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof EnergyCellBlockEntity energyCellBlockEntity) {
            return energyCellBlockEntity;
        }
        throw new IllegalStateException("Missing Matter Energy Cell block entity at " + pos);
    }
}
