package de.artemis.matterworks.common.io;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class SingleTankFluidHandler implements IFluidHandler {
    private final FluidTank tank;
    private final boolean allowFill;
    private final boolean allowDrain;

    public SingleTankFluidHandler(FluidTank tank, boolean allowFill, boolean allowDrain) {
        this.tank = tank;
        this.allowFill = allowFill;
        this.allowDrain = allowDrain;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == 0 ? this.tank.getFluidInTank(0) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? this.tank.getTankCapacity(0) : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return allowFill && tank == 0 && this.tank.isFluidValid(0, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return allowFill ? tank.fill(resource, action) : 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return allowDrain ? tank.drain(resource, action) : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return allowDrain ? tank.drain(maxDrain, action) : FluidStack.EMPTY;
    }
}
