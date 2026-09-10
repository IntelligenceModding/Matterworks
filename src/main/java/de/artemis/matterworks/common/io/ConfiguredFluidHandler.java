package de.artemis.matterworks.common.io;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.function.Function;
import java.util.function.Supplier;

public final class ConfiguredFluidHandler implements IFluidHandler {
    private final Supplier<SideAccessMode> modeSupplier;
    private final Supplier<IFluidHandler> inputSupplier;
    private final Function<SideAccessMode, IFluidHandler> outputSupplier;

    public ConfiguredFluidHandler(Supplier<SideAccessMode> modeSupplier, Supplier<IFluidHandler> delegateSupplier) {
        this(modeSupplier, delegateSupplier, ignored -> delegateSupplier.get());
    }

    public ConfiguredFluidHandler(Supplier<SideAccessMode> modeSupplier, Supplier<IFluidHandler> inputSupplier, Function<SideAccessMode, IFluidHandler> outputSupplier) {
        this.modeSupplier = modeSupplier;
        this.inputSupplier = inputSupplier;
        this.outputSupplier = outputSupplier;
    }

    @Override
    public int getTanks() {
        return getVisibleDelegate().getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return getVisibleDelegate().getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return getVisibleDelegate().getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return getMode().allowsInput() && getInputDelegate().isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return getMode().allowsInput() ? getInputDelegate().fill(resource, action) : 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        SideAccessMode mode = getMode();
        return mode.allowsOutput() ? getOutputDelegate(mode).drain(resource, action) : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        SideAccessMode mode = getMode();
        return mode.allowsOutput() ? getOutputDelegate(mode).drain(maxDrain, action) : FluidStack.EMPTY;
    }

    private SideAccessMode getMode() {
        return modeSupplier.get();
    }

    private IFluidHandler getVisibleDelegate() {
        SideAccessMode mode = getMode();
        if (mode == SideAccessMode.DISABLED) {
            return EmptyFluidHandler.INSTANCE;
        }
        if (mode.allowsOutput() && !mode.allowsInput()) {
            return getOutputDelegate(mode);
        }
        return getInputDelegate();
    }

    private IFluidHandler getInputDelegate() {
        return inputSupplier.get();
    }

    private IFluidHandler getOutputDelegate(SideAccessMode mode) {
        return outputSupplier.apply(mode);
    }

    private enum EmptyFluidHandler implements IFluidHandler {
        INSTANCE;

        @Override
        public int getTanks() {
            return 0;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
