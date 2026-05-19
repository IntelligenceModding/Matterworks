package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;

public class MatterCapacitorCellBlock extends AbstractBatteryMultiblockBlock {
    public static final MapCodec<MatterCapacitorCellBlock> CODEC = simpleCodec(MatterCapacitorCellBlock::new);

    public MatterCapacitorCellBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MatterCapacitorCellBlock> codec() {
        return CODEC;
    }
}
