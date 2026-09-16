package de.artemis.matterworks.client.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public record MachineOperationJeiRecipe(
        ItemStack machine,
        List<ItemStack> itemInputs,
        List<FluidStack> fluidInputs,
        List<ItemStack> itemOutputs,
        List<FluidStack> fluidOutputs,
        List<Component> notes
) {
    public MachineOperationJeiRecipe {
        machine = machine.copy();
        itemInputs = copyItemStacks(itemInputs);
        fluidInputs = copyFluidStacks(fluidInputs);
        itemOutputs = copyItemStacks(itemOutputs);
        fluidOutputs = copyFluidStacks(fluidOutputs);
        notes = List.copyOf(notes);
    }

    private static List<ItemStack> copyItemStacks(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::copy).toList();
    }

    private static List<FluidStack> copyFluidStacks(List<FluidStack> stacks) {
        return stacks.stream().map(FluidStack::copy).toList();
    }
}
