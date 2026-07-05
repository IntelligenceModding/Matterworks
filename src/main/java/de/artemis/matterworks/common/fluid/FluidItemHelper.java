package de.artemis.matterworks.common.fluid;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

public final class FluidItemHelper {
    private FluidItemHelper() {
    }

    public static @Nullable IFluidHandlerItem getFluidHandler(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.FluidHandler.ITEM);
    }

    public static boolean isFluidItem(ItemStack stack) {
        return getFluidHandler(stack) != null;
    }

    public static boolean isFilledFluidContainer(ItemStack stack) {
        return canProvideFluid(stack);
    }

    public static boolean isEmptyFluidContainer(ItemStack stack) {
        IFluidHandlerItem handler = getFluidHandler(stack);
        if (handler == null) {
            return false;
        }
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            if (!handler.getFluidInTank(tank).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean canProvideFluid(ItemStack stack) {
        IFluidHandlerItem handler = getFluidHandler(stack);
        if (handler == null) {
            return false;
        }
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            if (!handler.getFluidInTank(tank).isEmpty()) {
                return true;
            }
        }
        return !handler.drain(1, IFluidHandler.FluidAction.SIMULATE).isEmpty();
    }

    public static boolean canReceiveFluid(ItemStack stack, FluidStack sample) {
        IFluidHandlerItem handler = getFluidHandler(stack);
        return handler != null && !sample.isEmpty() && handler.fill(sample.copyWithAmount(1), IFluidHandler.FluidAction.SIMULATE) > 0;
    }

    public static int transferFluid(IFluidHandler source, IFluidHandler target, FluidStack sample, int maxTransfer) {
        if (sample.isEmpty() || maxTransfer <= 0) {
            return 0;
        }

        FluidStack requested = sample.copyWithAmount(Math.min(sample.getAmount(), maxTransfer));
        FluidStack simulatedDrain = source.drain(requested, IFluidHandler.FluidAction.SIMULATE);
        if (simulatedDrain.isEmpty()) {
            return 0;
        }

        int accepted = target.fill(simulatedDrain, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return 0;
        }

        FluidStack extracted = source.drain(simulatedDrain.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
        if (extracted.isEmpty()) {
            return 0;
        }

        int inserted = target.fill(extracted, IFluidHandler.FluidAction.EXECUTE);
        if (inserted < extracted.getAmount()) {
            FluidStack remainder = extracted.copyWithAmount(extracted.getAmount() - inserted);
            source.fill(remainder, IFluidHandler.FluidAction.EXECUTE);
        }
        return inserted;
    }
}
