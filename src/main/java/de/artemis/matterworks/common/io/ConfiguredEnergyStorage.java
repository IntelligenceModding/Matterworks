package de.artemis.matterworks.common.io;

import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.function.Supplier;

public final class ConfiguredEnergyStorage implements IEnergyStorage {
    private final Supplier<SideAccessMode> modeSupplier;
    private final Supplier<IEnergyStorage> delegateSupplier;

    public ConfiguredEnergyStorage(Supplier<SideAccessMode> modeSupplier, Supplier<IEnergyStorage> delegateSupplier) {
        this.modeSupplier = modeSupplier;
        this.delegateSupplier = delegateSupplier;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return getMode().allowsInput() ? getDelegate().receiveEnergy(maxReceive, simulate) : 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return getMode().allowsOutput() ? getDelegate().extractEnergy(maxExtract, simulate) : 0;
    }

    @Override
    public int getEnergyStored() {
        return getDelegate().getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return getDelegate().getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return getMode().allowsOutput() && getDelegate().canExtract();
    }

    @Override
    public boolean canReceive() {
        return getMode().allowsInput() && getDelegate().canReceive();
    }

    private SideAccessMode getMode() {
        return modeSupplier.get();
    }

    private IEnergyStorage getDelegate() {
        return delegateSupplier.get();
    }
}
