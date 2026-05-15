package de.artemis.matterworks.common.datagen;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public final class ModDatagenEntries {
    public static final List<GeneratedBlockPair> CORE_PAIRS = List.of();
    public static final List<GeneratedPillarPair> PILLAR_PAIRS = List.of();
    public static final List<GeneratedFanPair> FAN_PAIRS = List.of();
    public static final List<WoodFamily> WOOD_FAMILIES = List.of();
    public static final List<DeferredBlock<? extends DoorBlock>> DOORS = List.of();
    public static final List<DeferredBlock<? extends TrapDoorBlock>> TRAPDOORS = List.of();

    public static final List<GeneratedBlockPair> ALL_PAIRS = Stream.concat(
            CORE_PAIRS.stream(),
            WOOD_FAMILIES.stream().flatMap(WoodFamily::pairs)
    ).toList();

    public static final List<Object> GENERATED_PAIRS = Stream.concat(
            ALL_PAIRS.stream().map(pair -> (Object) pair),
            PILLAR_PAIRS.stream().map(pair -> (Object) pair)
    ).toList();

    private ModDatagenEntries() {
    }

    public record GeneratedBlockPair(DeferredBlock<? extends Block> base, DeferredBlock<? extends Block> glowing, String texturePath) {
        public String baseModelName() {
            return this.base.getId().getPath();
        }

        public String glowingModelName() {
            return this.glowing.getId().getPath();
        }

        public String fusionTexturePath() {
            return this.texturePath + "-fusion";
        }
    }

    public record WoodFamily(@Nullable Block planks, GeneratedBlockPair floorPair, GeneratedBlockPair tilePair) {
        public Stream<GeneratedBlockPair> pairs() {
            return Stream.of(this.floorPair, this.tilePair);
        }
    }

    public record GeneratedPillarPair(
            DeferredBlock<? extends Block> base,
            DeferredBlock<? extends Block> glowing,
            String sideTexturePath,
            String endTexturePath
    ) {
        public String baseModelName() {
            return this.base.getId().getPath();
        }

        public String glowingModelName() {
            return this.glowing.getId().getPath();
        }
    }

    public record GeneratedFanPair(
            DeferredBlock<? extends Block> base,
            DeferredBlock<? extends Block> glowing,
            String poweredTexturePath
    ) {
        public String baseModelName() {
            return this.base.getId().getPath();
        }

        public String glowingModelName() {
            return this.glowing.getId().getPath();
        }
    }
}
