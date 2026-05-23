package de.artemis.matterworks.common.datagen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, Matterworks.MOD_ID, null);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        var pickaxeTag = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        pickaxeTag.add(ModBlocks.POWER_CRYSTAL_ORE.get());
        pickaxeTag.add(ModBlocks.MATTER_SEPARATOR.get());
        pickaxeTag.add(ModBlocks.GRAVITIC_CONDENSER.get());
        pickaxeTag.add(ModBlocks.POWER_CRYSTAL_CHARGER.get());
        pickaxeTag.add(ModBlocks.MATTER_ENERGY_CELL.get());
        pickaxeTag.add(ModBlocks.MATTER_BATTERY_CORE.get());
        pickaxeTag.add(ModBlocks.MULTIBLOCK_FRAME.get());
        pickaxeTag.add(ModBlocks.MULTIBLOCK_CASING.get());
        pickaxeTag.add(ModBlocks.MULTIBLOCK_PORT.get());
        pickaxeTag.add(ModBlocks.MULTIBLOCK_GLASS.get());
        pickaxeTag.add(ModBlocks.MATTER_CAPACITOR_CELL.get());
        pickaxeTag.add(ModBlocks.MATTER_INDUCTION_RELAY.get());
        pickaxeTag.add(ModBlocks.MATTER_PYLON.get());
        pickaxeTag.add(ModBlocks.MATTER_NETWORK_CONTROLLER.get());
        pickaxeTag.add(ModBlocks.CREATIVE_SOURCE.get());
        pickaxeTag.add(ModBlocks.CREATIVE_SINK.get());
        ModDatagenEntries.CORE_PAIRS.forEach(pair -> {
            pickaxeTag.add(pair.base().get());
            pickaxeTag.add(pair.glowing().get());
        });
        ModDatagenEntries.PILLAR_PAIRS.forEach(pair -> {
            pickaxeTag.add(pair.base().get());
            pickaxeTag.add(pair.glowing().get());
        });
        ModDatagenEntries.FAN_PAIRS.forEach(pair -> {
            pickaxeTag.add(pair.base().get());
            pickaxeTag.add(pair.glowing().get());
        });
        ModDatagenEntries.DOORS.forEach(door -> pickaxeTag.add(door.get()));
        ModDatagenEntries.TRAPDOORS.forEach(trapdoor -> pickaxeTag.add(trapdoor.get()));

        var doorsTag = tag(BlockTags.DOORS);
        ModDatagenEntries.DOORS.forEach(door -> doorsTag.add(door.get()));

        var trapdoorsTag = tag(BlockTags.TRAPDOORS);
        ModDatagenEntries.TRAPDOORS.forEach(trapdoor -> trapdoorsTag.add(trapdoor.get()));

        var axeTag = tag(BlockTags.MINEABLE_WITH_AXE);
        ModDatagenEntries.WOOD_FAMILIES.forEach(family -> family.pairs().forEach(pair -> {
            axeTag.add(pair.base().get());
            axeTag.add(pair.glowing().get());
        }));
    }
}
