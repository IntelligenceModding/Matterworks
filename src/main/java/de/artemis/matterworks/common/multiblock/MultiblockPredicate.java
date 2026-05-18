package de.artemis.matterworks.common.multiblock;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

@FunctionalInterface
public interface MultiblockPredicate {
    boolean matches(MultiblockMatchContext context);

    static MultiblockPredicate any() {
        return context -> true;
    }

    static MultiblockPredicate air() {
        return context -> context.state().isAir();
    }

    static MultiblockPredicate block(Block block) {
        return context -> context.state().is(block);
    }

    static MultiblockPredicate blockTag(TagKey<Block> tag) {
        return context -> context.state().is(tag);
    }

    default MultiblockPredicate and(MultiblockPredicate other) {
        return context -> matches(context) && other.matches(context);
    }
}
