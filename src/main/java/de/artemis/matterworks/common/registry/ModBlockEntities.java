package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterAnalyzerBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterConstructorBlockEntity;
import de.artemis.matterworks.common.blockentity.EnergyCellBlockEntity;
import de.artemis.matterworks.common.blockentity.FluidTankBlockEntity;
import de.artemis.matterworks.common.blockentity.CombustionGeneratorBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterNetworkControllerBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterNetworkMonitorBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterRecyclerBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterSeparatorBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterStabilizerBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterStorageBarrelBlockEntity;
import de.artemis.matterworks.common.blockentity.CreativeSinkBlockEntity;
import de.artemis.matterworks.common.blockentity.CreativeSourceBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterBatteryPortBlockEntity;
import de.artemis.matterworks.common.blockentity.PowerCrystalChargerBlockEntity;
import de.artemis.matterworks.common.blockentity.PowerCrystalOreBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Matterworks.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterRecyclerBlockEntity>> MATTER_RECYCLER =
            BLOCK_ENTITY_TYPES.register("matter_recycler",
                    () -> BlockEntityType.Builder.of(MatterRecyclerBlockEntity::new, ModBlocks.MATTER_RECYCLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterStabilizerBlockEntity>> MATTER_STABILIZER =
            BLOCK_ENTITY_TYPES.register("matter_stabilizer",
                    () -> BlockEntityType.Builder.of(MatterStabilizerBlockEntity::new, ModBlocks.MATTER_STABILIZER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterAnalyzerBlockEntity>> MATTER_ANALYZER =
            BLOCK_ENTITY_TYPES.register("matter_analyzer",
                    () -> BlockEntityType.Builder.of(MatterAnalyzerBlockEntity::new, ModBlocks.MATTER_ANALYZER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterConstructorBlockEntity>> MATTER_CONSTRUCTOR =
            BLOCK_ENTITY_TYPES.register("matter_constructor",
                    () -> BlockEntityType.Builder.of(MatterConstructorBlockEntity::new, ModBlocks.MATTER_CONSTRUCTOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR =
            BLOCK_ENTITY_TYPES.register("combustion_generator",
                    () -> BlockEntityType.Builder.of(CombustionGeneratorBlockEntity::new, ModBlocks.COMBUSTION_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCellBlockEntity>> MATTER_ENERGY_CELL =
            BLOCK_ENTITY_TYPES.register("matter_energy_cell",
                    () -> BlockEntityType.Builder.of(EnergyCellBlockEntity::new, ModBlocks.MATTER_ENERGY_CELL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterBatteryCoreBlockEntity>> MATTER_BATTERY_CORE =
            BLOCK_ENTITY_TYPES.register("matter_battery_core",
                    () -> BlockEntityType.Builder.of(MatterBatteryCoreBlockEntity::new, ModBlocks.MATTER_BATTERY_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterBatteryPortBlockEntity>> MULTIBLOCK_PORT =
            BLOCK_ENTITY_TYPES.register("multiblock_port",
                    () -> BlockEntityType.Builder.of(MatterBatteryPortBlockEntity::new, ModBlocks.MULTIBLOCK_PORT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>> FLUID_TANK =
            BLOCK_ENTITY_TYPES.register("fluid_tank",
                    () -> BlockEntityType.Builder.of(FluidTankBlockEntity::new, ModBlocks.FLUID_TANK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterStorageBarrelBlockEntity>> MATTER_STORAGE_BARREL =
            BLOCK_ENTITY_TYPES.register("matter_storage_barrel",
                    () -> BlockEntityType.Builder.of(MatterStorageBarrelBlockEntity::new, ModBlocks.MATTER_STORAGE_BARREL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterPylonBlockEntity>> MATTER_PYLON =
            BLOCK_ENTITY_TYPES.register("matter_pylon",
                    () -> BlockEntityType.Builder.of(MatterPylonBlockEntity::new, ModBlocks.MATTER_PYLON.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterNetworkControllerBlockEntity>> MATTER_NETWORK_CONTROLLER =
            BLOCK_ENTITY_TYPES.register("matter_network_controller",
                    () -> BlockEntityType.Builder.of(MatterNetworkControllerBlockEntity::new, ModBlocks.MATTER_NETWORK_CONTROLLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterNetworkMonitorBlockEntity>> MATTER_NETWORK_MONITOR =
            BLOCK_ENTITY_TYPES.register("matter_network_monitor",
                    () -> BlockEntityType.Builder.of(MatterNetworkMonitorBlockEntity::new, ModBlocks.MATTER_NETWORK_MONITOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeSourceBlockEntity>> CREATIVE_SOURCE =
            BLOCK_ENTITY_TYPES.register("creative_source",
                    () -> BlockEntityType.Builder.of(CreativeSourceBlockEntity::new, ModBlocks.CREATIVE_SOURCE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeSinkBlockEntity>> CREATIVE_SINK =
            BLOCK_ENTITY_TYPES.register("creative_sink",
                    () -> BlockEntityType.Builder.of(CreativeSinkBlockEntity::new, ModBlocks.CREATIVE_SINK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterSeparatorBlockEntity>> MATTER_SEPARATOR =
            BLOCK_ENTITY_TYPES.register("matter_separator",
                    () -> BlockEntityType.Builder.of(MatterSeparatorBlockEntity::new, ModBlocks.MATTER_SEPARATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerCrystalChargerBlockEntity>> POWER_CRYSTAL_CHARGER =
            BLOCK_ENTITY_TYPES.register("power_crystal_charger",
                    () -> BlockEntityType.Builder.of(PowerCrystalChargerBlockEntity::new, ModBlocks.POWER_CRYSTAL_CHARGER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerCrystalOreBlockEntity>> POWER_CRYSTAL_ORE =
            BLOCK_ENTITY_TYPES.register("power_crystal_ore",
                    () -> BlockEntityType.Builder.of(PowerCrystalOreBlockEntity::new, ModBlocks.POWER_CRYSTAL_ORE.get(), ModBlocks.POWER_CRYSTAL_REVEAL.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}

