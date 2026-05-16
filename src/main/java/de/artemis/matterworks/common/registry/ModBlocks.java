package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.block.MatterAnalyzerBlock;
import de.artemis.matterworks.common.block.MatterConstructorBlock;
import de.artemis.matterworks.common.block.MatterGeneratorBlock;
import de.artemis.matterworks.common.block.MatterRecyclerBlock;
import de.artemis.matterworks.common.block.MatterSeparatorBlock;
import de.artemis.matterworks.common.block.MatterSludgeBlock;
import de.artemis.matterworks.common.block.MatterStabilizerBlock;
import de.artemis.matterworks.common.block.HardenedSludgeBlock;
import de.artemis.matterworks.common.block.PowerCrystalChargerBlock;
import de.artemis.matterworks.common.block.PowerCrystalOreBlock;
import de.artemis.matterworks.common.block.PowerCrystalRevealBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Matterworks.MOD_ID);

    private static <T extends Block> DeferredBlock<T> register(
            String name,
            Function<BlockBehaviour.Properties, T> blockFactory,
            UnaryOperator<BlockBehaviour.Properties> properties
    ) {
        DeferredBlock<T> block = BLOCKS.registerBlock(name, blockFactory, properties.apply(BlockBehaviour.Properties.of()));
        ModItems.ITEMS.registerSimpleBlockItem(block, new Item.Properties());
        return block;
    }

    private static boolean never(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, EntityType<?> entityType) {
        return false;
    }

    private static boolean never(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return false;
    }

    public static final DeferredBlock<MatterRecyclerBlock> MATTER_RECYCLER = register(
            "matter_recycler",
            MatterRecyclerBlock::new,
            properties -> properties.strength(3.5F).sound(SoundType.METAL)
    );

    public static final DeferredBlock<MatterStabilizerBlock> MATTER_STABILIZER = register(
            "matter_stabilizer",
            MatterStabilizerBlock::new,
            properties -> properties.strength(3.5F).sound(SoundType.METAL)
    );

    public static final DeferredBlock<LiquidBlock> RAW_MATTER_BLOCK = BLOCKS.register("raw_matter",
            () -> new LiquidBlock(ModFluids.RAW_MATTER.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .liquid()
                    .sound(SoundType.EMPTY)
                    .noLootTable()));

    public static final DeferredBlock<LiquidBlock> REFINED_MATTER_BLOCK = BLOCKS.register("refined_matter",
            () -> new LiquidBlock(ModFluids.REFINED_MATTER.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIAMOND)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .liquid()
                    .sound(SoundType.EMPTY)
                    .noLootTable()));

    public static final DeferredBlock<LiquidBlock> UNSTABLE_MATTER_BLOCK = BLOCKS.register("unstable_matter",
            () -> new LiquidBlock(ModFluids.UNSTABLE_MATTER.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .liquid()
                    .sound(SoundType.EMPTY)
                    .noLootTable()));

    public static final DeferredBlock<MatterSludgeBlock> MATTER_SLUDGE_BLOCK = BLOCKS.register("matter_sludge",
            () -> new MatterSludgeBlock(ModFluids.MATTER_SLUDGE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .liquid()
                    .lightLevel(state -> 2)
                    .sound(SoundType.EMPTY)
                    .noLootTable()));

    public static final DeferredBlock<HardenedSludgeBlock> HARDENED_SLUDGE = register(
            "hardened_sludge",
            HardenedSludgeBlock::new,
            properties -> properties.mapColor(MapColor.COLOR_BROWN).strength(0.8F).sound(SoundType.MUD).noOcclusion()
    );

    public static final DeferredBlock<MatterAnalyzerBlock> MATTER_ANALYZER = register(
            "matter_analyzer",
            MatterAnalyzerBlock::new,
            properties -> properties.strength(3.5F).sound(SoundType.GLASS)
    );

    public static final DeferredBlock<MatterConstructorBlock> MATTER_CONSTRUCTOR = register(
            "matter_constructor",
            MatterConstructorBlock::new,
            properties -> properties.strength(3.5F).sound(SoundType.METAL)
    );

    public static final DeferredBlock<MatterGeneratorBlock> MATTER_GENERATOR = register(
            "matter_generator",
            MatterGeneratorBlock::new,
            properties -> properties.strength(3.5F).sound(SoundType.METAL)
    );

    public static final DeferredBlock<MatterSeparatorBlock> MATTER_SEPARATOR = register(
            "matter_separator",
            MatterSeparatorBlock::new,
            properties -> properties.strength(3.5F).sound(SoundType.METAL)
    );

    public static final DeferredBlock<PowerCrystalChargerBlock> POWER_CRYSTAL_CHARGER = register(
            "power_crystal_charger",
            PowerCrystalChargerBlock::new,
            properties -> properties.strength(3.5F).sound(SoundType.METAL)
    );

    public static final DeferredBlock<PowerCrystalOreBlock> POWER_CRYSTAL_ORE = register(
            "power_crystal_ore",
            PowerCrystalOreBlock::new,
            properties -> properties.mapColor(MapColor.STONE).strength(4.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noLootTable()
    );

    public static final DeferredBlock<PowerCrystalRevealBlock> POWER_CRYSTAL_REVEAL = BLOCKS.registerBlock(
            "power_crystal_reveal",
            PowerCrystalRevealBlock::new,
            BlockBehaviour.Properties.of()
                    .replaceable()
                    .noCollission()
                    .noOcclusion()
                    .lightLevel(state -> 4)
                    .instabreak()
                    .sound(SoundType.EMPTY)
                    .noLootTable()
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
