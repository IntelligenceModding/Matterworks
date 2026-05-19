package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.PowerCrystalChargerBlockEntity;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class PowerCrystalChargerMenu extends AbstractContainerMenu implements NamedBlockMenu {
    private static final int PLAYER_INVENTORY_START = PowerCrystalChargerBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 9;

    private final PowerCrystalChargerBlockEntity blockEntity;
    private final ContainerData data;

    public PowerCrystalChargerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()), new SimpleContainerData(PowerCrystalChargerBlockEntity.DATA_COUNT));
    }

    public PowerCrystalChargerMenu(int containerId, Inventory playerInventory, PowerCrystalChargerBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.POWER_CRYSTAL_CHARGER.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addMachineSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    public int getProgress() {
        return data.get(PowerCrystalChargerBlockEntity.DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return data.get(PowerCrystalChargerBlockEntity.DATA_MAX_PROGRESS);
    }

    public int getScaledProgress(int width) {
        int progress = getProgress();
        int maxProgress = getMaxProgress();
        if (progress <= 0 || maxProgress <= 0) {
            return 0;
        }
        return Math.max(1, progress * width / maxProgress);
    }

    public int getEnergyStored() {
        return data.get(PowerCrystalChargerBlockEntity.DATA_ENERGY);
    }

    public int getEnergyCapacity() {
        return data.get(PowerCrystalChargerBlockEntity.DATA_ENERGY_CAPACITY);
    }

    public int getScaledEnergyAmount(int width) {
        int stored = getEnergyStored();
        int capacity = getEnergyCapacity();
        if (stored <= 0 || capacity <= 0) {
            return 0;
        }
        return Math.max(1, stored * width / capacity);
    }

    public int getProgressBarColor() {
        return 0xFF000000 | data.get(PowerCrystalChargerBlockEntity.DATA_PROGRESS_COLOR);
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
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.POWER_CRYSTAL_CHARGER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copiedStack = sourceStack.copy();

        if (index < PowerCrystalChargerBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (PowerCrystalEffects.isPowerCrystal(sourceStack)) {
            boolean moved = false;
            if (PowerCrystalEffects.hasChargerEffect(sourceStack)) {
                moved = moveItemStackTo(sourceStack, PowerCrystalChargerBlockEntity.SLOT_BOOST, PowerCrystalChargerBlockEntity.SLOT_BOOST + 1, false);
            }
            if (!moved
                    && !moveItemStackTo(sourceStack, PowerCrystalChargerBlockEntity.SLOT_CHARGE_START, PowerCrystalChargerBlockEntity.SLOT_CHARGE_START + PowerCrystalChargerBlockEntity.CHARGE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (EnergyItemHelper.canProvideEnergy(sourceStack)) {
            if (!moveItemStackTo(sourceStack, PowerCrystalChargerBlockEntity.SLOT_POWER_INPUT, PowerCrystalChargerBlockEntity.SLOT_POWER_INPUT + 1, false)
                    && !moveItemStackTo(sourceStack, PowerCrystalChargerBlockEntity.SLOT_CHARGE_START, PowerCrystalChargerBlockEntity.SLOT_CHARGE_START + PowerCrystalChargerBlockEntity.CHARGE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (EnergyItemHelper.canReceiveEnergy(sourceStack)) {
            if (!moveItemStackTo(sourceStack, PowerCrystalChargerBlockEntity.SLOT_CHARGE_START, PowerCrystalChargerBlockEntity.SLOT_CHARGE_START + PowerCrystalChargerBlockEntity.CHARGE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_HOTBAR_START) {
            if (!moveItemStackTo(sourceStack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START, false)) {
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

    private void addMachineSlots() {
        for (int column = 0; column < PowerCrystalChargerBlockEntity.CHARGE_SLOT_COUNT; column++) {
            int slot = PowerCrystalChargerBlockEntity.SLOT_CHARGE_START + column;
            addSlot(new ChargeTargetSlot(blockEntity.getItemHandler(), slot, 8 + column * 18, 18));
        }
        addSlot(new BoostCrystalSlot(blockEntity.getItemHandler(), PowerCrystalChargerBlockEntity.SLOT_BOOST, 8, 39));
        addSlot(new EnergySourceSlot(blockEntity.getItemHandler(), PowerCrystalChargerBlockEntity.SLOT_POWER_INPUT, 152, 39));
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 71 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(inventory, slot, 8 + slot * 18, 129));
        }
    }

    private static PowerCrystalChargerBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof PowerCrystalChargerBlockEntity chargerBlockEntity) {
            return chargerBlockEntity;
        }
        throw new IllegalStateException("Missing Charger block entity at " + pos);
    }

    private static final class ChargeTargetSlot extends SlotItemHandler {
        private ChargeTargetSlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int slot, int x, int y) {
            super(itemHandler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return PowerCrystalEffects.isPowerCrystal(stack) || EnergyItemHelper.canReceiveEnergy(stack);
        }
    }

    private static final class BoostCrystalSlot extends SlotItemHandler {
        private BoostCrystalSlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int slot, int x, int y) {
            super(itemHandler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return PowerCrystalEffects.hasChargerEffect(stack);
        }
    }

    private static final class EnergySourceSlot extends SlotItemHandler {
        private EnergySourceSlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int slot, int x, int y) {
            super(itemHandler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return EnergyItemHelper.canProvideEnergy(stack);
        }
    }
}
