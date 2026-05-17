package de.artemis.matterworks.common.capability;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.energy.PowerBankEnergyStorage;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.item.MatterPowerBankItem;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ModCapabilities {
    private ModCapabilities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.EnergyStorage.ITEM,
                (stack, context) -> new PowerBankEnergyStorage(stack, MatterPowerBankItem.CAPACITY, MatterPowerBankItem.MAX_TRANSFER),
                ModItems.MATTER_POWER_BANK.get()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MATTER_RECYCLER.get(),
                (blockEntity, side) -> blockEntity.getAutomationHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.MATTER_RECYCLER.get(),
                (blockEntity, side) -> blockEntity.getFluidAutomationHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.MATTER_RECYCLER.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MATTER_STABILIZER.get(),
                (blockEntity, side) -> blockEntity.getAutomationHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.MATTER_STABILIZER.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MATTER_ANALYZER.get(),
                (blockEntity, side) -> blockEntity.getAutomationHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.MATTER_STABILIZER.get(),
                (blockEntity, side) -> blockEntity.getFluidAutomationHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.MATTER_ANALYZER.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MATTER_CONSTRUCTOR.get(),
                (blockEntity, side) -> blockEntity.getAutomationHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.MATTER_CONSTRUCTOR.get(),
                (blockEntity, side) -> blockEntity.getFluidAutomationHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.MATTER_CONSTRUCTOR.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MATTER_GENERATOR.get(),
                (blockEntity, side) -> blockEntity.getAutomationHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.MATTER_GENERATOR.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MATTER_ENERGY_CELL.get(),
                (blockEntity, side) -> blockEntity.getItemHandler()
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.MATTER_ENERGY_CELL.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MATTER_FLUID_TANK.get(),
                (blockEntity, side) -> blockEntity.getItemHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.MATTER_FLUID_TANK.get(),
                (blockEntity, side) -> blockEntity.getFluidStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MATTER_STORAGE_BARREL.get(),
                (blockEntity, side) -> blockEntity.getItemHandler()
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.CREATIVE_SOURCE.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CREATIVE_SOURCE.get(),
                (blockEntity, side) -> blockEntity.getItemHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.CREATIVE_SOURCE.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.CREATIVE_SINK.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CREATIVE_SINK.get(),
                (blockEntity, side) -> blockEntity.getItemHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.CREATIVE_SINK.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MATTER_SEPARATOR.get(),
                (blockEntity, side) -> blockEntity.getAutomationHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.MATTER_SEPARATOR.get(),
                (blockEntity, side) -> blockEntity.getFluidAutomationHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.MATTER_SEPARATOR.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.POWER_CRYSTAL_CHARGER.get(),
                (blockEntity, side) -> blockEntity.getAutomationHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.POWER_CRYSTAL_CHARGER.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage(side)
        );
    }
}
