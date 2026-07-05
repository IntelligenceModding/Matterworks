package de.artemis.matterworks.common.multiblock;

import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class MatterBatteryMultiblockDefinition implements MultiblockDefinition {
    public static final String ID = "matter_battery_matrix";
    public static final MatterBatteryMultiblockDefinition INSTANCE = new MatterBatteryMultiblockDefinition();
    public static final String DESC_FRAME = "multiblock frame";
    public static final String DESC_CASING = "multiblock casing";
    public static final String DESC_CONTROLLER = "battery core";
    public static final String DESC_CELL = "matter capacitor cell";

    private final MultiblockPattern pattern = createPattern();

    private MatterBatteryMultiblockDefinition() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public MultiblockPattern getPattern() {
        return pattern;
    }

    @Override
    public boolean canAssemble(MultiblockMatch match) {
        return true;
    }

    private static MultiblockPattern createPattern() {
        return MultiblockPattern.builder(new BlockPos(1, 1, 1))
                .aisle(
                        "FFFFF",
                        "FSSSF",
                        "FSSSF",
                        "FSSSF",
                        "FFCFF"
                )
                .aisle(
                        "FSSSF",
                        "FCIIF",
                        "FIIIF",
                        "FIIIF",
                        "FSSSF"
                )
                .aisle(
                        "FSSSF",
                        "FIIIF",
                        "FIIIF",
                        "FIIIF",
                        "FSSSF"
                )
                .aisle(
                        "FSSSF",
                        "FIIIF",
                        "FIIIF",
                        "FIIIF",
                        "FSSSF"
                )
                .aisle(
                        "FFFFF",
                        "FSSSF",
                        "FSSSF",
                        "FSSSF",
                        "FFFFF"
                )
                .where('F', MultiblockRequirement.frame(MatterBatteryMultiblockDefinition::matchesFrame, DESC_FRAME))
                .where('S', MultiblockRequirement.casing(MatterBatteryMultiblockDefinition::matchesShellFace, DESC_CASING))
                .where('C', MultiblockRequirement.controller(MultiblockPredicate.block(ModBlocks.MATTER_BATTERY_CORE.get()), DESC_CONTROLLER))
                .where('I', MultiblockRequirement.internal(MatterBatteryMultiblockDefinition::matchesInternal, DESC_CELL))
                .build();
    }

    public static boolean matchesFrame(MultiblockMatchContext context) {
        return matchesFrameState(context.state());
    }

    public static boolean matchesFrameState(BlockState state) {
        return state.is(ModBlocks.MULTIBLOCK_FRAME.get())
                || state.is(ModBlocks.MULTIBLOCK_PORT.get());
    }

    public static boolean matchesShellFace(MultiblockMatchContext context) {
        return matchesShellFaceState(context.state());
    }

    public static boolean matchesShellFaceState(BlockState state) {
        return state.is(ModBlocks.MULTIBLOCK_CASING.get())
                || state.is(ModBlocks.MULTIBLOCK_PORT.get())
                || state.is(ModBlocks.MULTIBLOCK_GLASS.get());
    }

    public static boolean matchesInternal(MultiblockMatchContext context) {
        return matchesInternalState(context.state());
    }

    public static boolean matchesInternalState(BlockState state) {
        return state.is(ModBlocks.MATTER_CAPACITOR_CELL.get());
    }
}
