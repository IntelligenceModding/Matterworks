package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterAnalyzerBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterConstructorBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterGeneratorBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterRecyclerBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterSeparatorBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterStabilizerBlockEntity;
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterGeneratorBlockEntity>> MATTER_GENERATOR =
            BLOCK_ENTITY_TYPES.register("matter_generator",
                    () -> BlockEntityType.Builder.of(MatterGeneratorBlockEntity::new, ModBlocks.MATTER_GENERATOR.get()).build(null));

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
