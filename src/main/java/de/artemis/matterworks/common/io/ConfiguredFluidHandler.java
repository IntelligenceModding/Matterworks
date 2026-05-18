package de.artemis.matterworks.common.io;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.function.Supplier;

public final class ConfiguredFluidHandler implements IFluidHandler {
    private final Supplier<SideAccessMode> modeSupplier;
    private final Supplier<IFluidHandler> delegateSupplier;

    public ConfiguredFluidHandler(Supplier<SideAccessMode> modeSupplier, Supplier<IFluidHandler> delegateSupplier) {
        this.modeSupplier = modeSupplier;
        this.delegateSupplier = delegateSupplier;
    }

    @Override
    public int getTanks() {
        return getDelegate().getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return getDelegate().getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return getDelegate().getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return getMode().allowsInput() && getDelegate().isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return getMode().allowsInput() ? getDelegate().fill(resource, action) : 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return getMode().allowsOutput() ? getDelegate().drain(resource, action) : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return getMode().allowsOutput() ? getDelegate().drain(maxDrain, action) : FluidStack.EMPTY;
    }

    private SideAccessMode getMode() {
        return modeSupplier.get();
    }

    private IFluidHandler getDelegate() {
        return delegateSupplier.get();
    }
}
