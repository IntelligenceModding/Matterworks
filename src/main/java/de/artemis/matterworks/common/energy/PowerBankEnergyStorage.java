package de.artemis.matterworks.common.energy;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class PowerBankEnergyStorage implements IEnergyStorage {
    private final ItemStack stack;
    private final int capacity;
    private final int maxTransfer;

    public PowerBankEnergyStorage(ItemStack stack, int capacity, int maxTransfer) {
        this.stack = stack;
        this.capacity = capacity;
        this.maxTransfer = maxTransfer;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (maxReceive <= 0) {
            return 0;
        }

        int energyStored = PowerBankData.getEnergyStored(stack, capacity);
        int received = Math.min(Math.min(maxTransfer, maxReceive), capacity - energyStored);
        if (received > 0 && !simulate) {
            PowerBankData.setEnergyStored(stack, energyStored + received, capacity);
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (maxExtract <= 0) {
            return 0;
        }

        int energyStored = PowerBankData.getEnergyStored(stack, capacity);
        int extracted = Math.min(Math.min(maxTransfer, maxExtract), energyStored);
        if (extracted > 0 && !simulate) {
            PowerBankData.setEnergyStored(stack, energyStored - extracted, capacity);
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return PowerBankData.getEnergyStored(stack, capacity);
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
