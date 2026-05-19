package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.menu.PowerCrystalChargerMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.upgrade.PowerCrystalData;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PowerCrystalChargerBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements MenuProvider, CustomNamedBlockEntity {
    public static final int CHARGE_SLOT_COUNT = 9;
    public static final int SLOT_CHARGE_START = 0;
    public static final int SLOT_BOOST = 9;
    public static final int SLOT_POWER_INPUT = 10;
    public static final int SLOT_COUNT = 11;
    public static final int TICKS_PER_PERCENT = PowerCrystalEffects.CHARGER_BASE_PROCESS_TIME;
    public static final int ENERGY_PER_TICK = PowerCrystalEffects.CHARGER_BASE_ENERGY_PER_TICK;
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_ENERGY = 2;
    public static final int DATA_ENERGY_CAPACITY = 3;
    public static final int DATA_PROGRESS_COLOR = 4;
    public static final int DATA_COUNT = 5;
    private static final int DEFAULT_PROGRESS_COLOR = 0xB67CFF;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_BOOST -> PowerCrystalEffects.hasChargerEffect(stack);
                case SLOT_POWER_INPUT -> EnergyItemHelper.canProvideEnergy(stack);
                default -> isChargeableTarget(stack);
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private final ChargerEnergyStorage energyStorage = new ChargerEnergyStorage(PowerCrystalEffects.CHARGER_BASE_ENERGY_CAPACITY);
    private final IEnergyStorage externalEnergyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return energyStorage.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energyStorage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyStorage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX_PROGRESS -> getProcessTime();
                case DATA_ENERGY -> energyStorage.getEnergyStored();
                case DATA_ENERGY_CAPACITY -> energyStorage.getMaxEnergyStored();
                case DATA_PROGRESS_COLOR -> getProgressColor();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == DATA_PROGRESS) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private String customName = "";
    private int progress;
    private int activeChargeSlot = -1;
    private int nextChargeSlot;

    public PowerCrystalChargerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.POWER_CRYSTAL_CHARGER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PowerCrystalChargerBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public IEnergyStorage getEnergyStorage(@Nullable net.minecraft.core.Direction side) {
        return externalEnergyStorage;
    }

    public ContainerData getData() {
        return data;
    }

    public SimpleContainer createDropInventory() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            inventory.setItem(slot, itemHandler.getStackInSlot(slot).copy());
        }
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return customName.isEmpty() ? getDefaultName() : Component.literal(customName);
    }

    @Override
    public String getCustomNameText() {
        return customName;
    }

    @Override
    public void setCustomNameText(String customName) {
        String normalized = normalizeCustomName(customName);
        if (Objects.equals(this.customName, normalized)) {
            return;
        }
        this.customName = normalized;
        setChanged();
        syncVisualState();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PowerCrystalChargerMenu(containerId, playerInventory, this, data);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (!customName.isEmpty()) {
            tag.putString("custom_name", customName);
        }
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putInt("progress", progress);
        tag.putInt("active_charge_slot", activeChargeSlot);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putInt("progress", progress);
        tag.putInt("active_charge_slot", activeChargeSlot);
        tag.putInt("next_charge_slot", nextChargeSlot);
        if (!customName.isEmpty()) {
            tag.putString("custom_name", customName);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            CompoundTag inventoryTag = tag.getCompound("inventory");
            if (inventoryTag.contains("Size") && inventoryTag.getInt("Size") < SLOT_COUNT) {
                migrateLegacyInventory(inventoryTag, registries);
            } else {
                itemHandler.deserializeNBT(registries, inventoryTag);
            }
        }
        energyStorage.setStoredEnergy(tag.getInt("energy"));
        progress = Math.max(0, tag.getInt("progress"));
        activeChargeSlot = tag.getInt("active_charge_slot");
        nextChargeSlot = Math.floorMod(tag.getInt("next_charge_slot"), CHARGE_SLOT_COUNT);
        customName = normalizeCustomName(tag.getString("custom_name"));
        syncEnergyCapacity();
    }

    private void serverTick() {
        syncEnergyCapacity();
        transferEnergyFromPowerSlot();
        int energyPerTarget = getEnergyPerTick();
        boolean itemActivity = chargeEnergyItems(energyPerTarget);
        boolean crystalActivity = tickCrystalProgress(energyPerTarget);
        if (itemActivity || crystalActivity) {
            if (!crystalActivity) {
                progress = (progress + 1) % Math.max(1, getProcessTime());
                setChanged();
            }
            return;
        }
        resetProgress();
    }

    private void resetProgress() {
        if (progress != 0 || activeChargeSlot != -1) {
            progress = 0;
            activeChargeSlot = -1;
            setChanged();
        }
    }

    private boolean isChargeableTarget(ItemStack stack) {
        return canChargeCrystal(stack) || canChargeEnergyItem(stack);
    }

    private boolean canChargeCrystal(ItemStack stack) {
        return PowerCrystalEffects.isPowerCrystal(stack) && PowerCrystalData.getChargePercent(stack) < PowerCrystalData.MAX_CHARGE;
    }

    private boolean canChargeEnergyItem(ItemStack stack) {
        return EnergyItemHelper.canReceiveEnergy(stack);
    }

    private void transferEnergyFromPowerSlot() {
        ItemStack sourceStack = itemHandler.getStackInSlot(SLOT_POWER_INPUT);
        IEnergyStorage sourceEnergy = EnergyItemHelper.getEnergyStorage(sourceStack);
        if (sourceEnergy == null) {
            return;
        }

        int missingEnergy = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        if (missingEnergy <= 0) {
            return;
        }

        int moved = EnergyItemHelper.transferEnergy(sourceEnergy, energyStorage, missingEnergy);
        if (moved > 0) {
            setChanged();
        }
    }

    private void syncEnergyCapacity() {
        int desiredCapacity = PowerCrystalEffects.getChargerEnergyCapacity(itemHandler.getStackInSlot(SLOT_BOOST));
        if (energyStorage.getMaxEnergyStored() != desiredCapacity) {
            energyStorage.setCapacity(desiredCapacity);
            setChanged();
        }
    }

    private void consumeBoostCharge() {
        ItemStack boostStack = itemHandler.getStackInSlot(SLOT_BOOST);
        if (PowerCrystalEffects.isActive(boostStack)) {
            PowerCrystalData.drainCharge(boostStack, PowerCrystalEffects.CRYSTAL_CHARGE_COST);
            setChanged();
        }
    }

    private boolean chargeEnergyItems(int energyPerTarget) {
        boolean anyActivity = false;
        for (int slot = SLOT_CHARGE_START; slot < SLOT_CHARGE_START + CHARGE_SLOT_COUNT; slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            IEnergyStorage targetEnergy = EnergyItemHelper.getEnergyStorage(stack);
            if (targetEnergy == null) {
                continue;
            }

            int offered = Math.min(energyPerTarget, energyStorage.getEnergyStored());
            if (offered <= 0) {
                break;
            }

            int accepted = targetEnergy.receiveEnergy(offered, true);
            if (accepted <= 0) {
                continue;
            }

            int extracted = energyStorage.extractEnergy(accepted, false);
            if (extracted <= 0) {
                break;
            }

            int received = targetEnergy.receiveEnergy(extracted, false);
            if (received <= 0) {
                energyStorage.receiveEnergy(extracted, false);
                continue;
            }

            if (received < extracted) {
                energyStorage.receiveEnergy(extracted - received, false);
            }
            anyActivity = true;
            setChanged();
        }
        return anyActivity;
    }

    private boolean tickCrystalProgress(int energyPerTarget) {
        int crystalTargets = countChargeableCrystals();
        if (crystalTargets <= 0) {
            return false;
        }

        int requiredEnergy = energyPerTarget * crystalTargets;
        if (requiredEnergy <= 0 || energyStorage.extractEnergy(requiredEnergy, true) < requiredEnergy) {
            return false;
        }

        energyStorage.extractEnergy(requiredEnergy, false);
        progress++;
        activeChargeSlot = SLOT_BOOST;
        setChanged();

        if (progress >= getProcessTime()) {
            for (int slot = SLOT_CHARGE_START; slot < SLOT_CHARGE_START + CHARGE_SLOT_COUNT; slot++) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (canChargeCrystal(stack)) {
                    PowerCrystalData.setChargePercent(stack, Math.min(PowerCrystalData.MAX_CHARGE, PowerCrystalData.getChargePercent(stack) + 1));
                }
            }
            consumeBoostCharge(crystalTargets);
            progress = 0;
            activeChargeSlot = -1;
            setChanged();
        }
        return true;
    }

    private int countChargeableCrystals() {
        int count = 0;
        for (int slot = SLOT_CHARGE_START; slot < SLOT_CHARGE_START + CHARGE_SLOT_COUNT; slot++) {
            if (canChargeCrystal(itemHandler.getStackInSlot(slot))) {
                count++;
            }
        }
        return count;
    }

    private void consumeBoostCharge(int amount) {
        ItemStack boostStack = itemHandler.getStackInSlot(SLOT_BOOST);
        if (PowerCrystalEffects.isActive(boostStack)) {
            PowerCrystalData.drainCharge(boostStack, PowerCrystalEffects.CRYSTAL_CHARGE_COST * Math.max(1, amount));
            setChanged();
        }
    }

    private int getProcessTime() {
        return PowerCrystalEffects.getChargerProcessTime(itemHandler.getStackInSlot(SLOT_BOOST));
    }

    private int getEnergyPerTick() {
        return PowerCrystalEffects.getChargerEnergyPerTick(itemHandler.getStackInSlot(SLOT_BOOST));
    }

    private int getProgressColor() {
        ItemStack boostStack = itemHandler.getStackInSlot(SLOT_BOOST);
        return PowerCrystalEffects.isActive(boostStack) ? PowerCrystalEffects.getBarColor(boostStack) : DEFAULT_PROGRESS_COLOR;
    }

    private void migrateLegacyInventory(CompoundTag inventoryTag, HolderLookup.Provider registries) {
        ItemStackHandler legacyHandler = new ItemStackHandler(7);
        legacyHandler.deserializeNBT(registries, inventoryTag);

        for (int legacySlot = 0; legacySlot < legacyHandler.getSlots(); legacySlot++) {
            ItemStack stack = legacyHandler.getStackInSlot(legacySlot);
            if (stack.isEmpty()) {
                continue;
            }

            if (legacySlot == AbstractMatterMachineBlockEntity.CRYSTAL_SLOT
                    && itemHandler.getStackInSlot(SLOT_BOOST).isEmpty()
                    && PowerCrystalEffects.isPowerCrystal(stack)) {
                itemHandler.setStackInSlot(SLOT_BOOST, stack.copy());
                continue;
            }

            if (legacySlot == AbstractMatterMachineBlockEntity.ENERGY_ITEM_INPUT_SLOT
                    && itemHandler.getStackInSlot(SLOT_POWER_INPUT).isEmpty()
                    && EnergyItemHelper.canProvideEnergy(stack)) {
                itemHandler.setStackInSlot(SLOT_POWER_INPUT, stack.copy());
                continue;
            }

            insertIntoFirstChargeSlot(stack);
        }
    }

    private void insertIntoFirstChargeSlot(ItemStack stack) {
        for (int slot = SLOT_CHARGE_START; slot < SLOT_CHARGE_START + CHARGE_SLOT_COUNT; slot++) {
            if (itemHandler.getStackInSlot(slot).isEmpty() && isChargeableTarget(stack)) {
                itemHandler.setStackInSlot(slot, stack.copy());
                return;
            }
        }
    }

    private Component getDefaultName() {
        return Component.translatable(ModBlocks.POWER_CRYSTAL_CHARGER.get().getDescriptionId());
    }

    private void syncVisualState() {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static String normalizeCustomName(String customName) {
        String normalized = customName.strip();
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private static final class ChargerEnergyStorage extends EnergyStorage {
        private ChargerEnergyStorage(int capacity) {
            super(capacity, capacity, capacity);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return super.receiveEnergy(maxReceive, simulate);
        }

        private void setStoredEnergy(int storedEnergy) {
            this.energy = Math.max(0, Math.min(storedEnergy, capacity));
        }

        private void setCapacity(int capacity) {
            this.capacity = Math.max(0, capacity);
            if (energy > this.capacity) {
                energy = this.capacity;
            }
        }
    }
}
