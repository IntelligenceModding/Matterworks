package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;

public class MatterBatteryCasingBlock extends AbstractBatteryMultiblockBlock {
    public static final MapCodec<MatterBatteryCasingBlock> CODEC = simpleCodec(MatterBatteryCasingBlock::new);

    public MatterBatteryCasingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MatterBatteryCasingBlock> codec() {
        return CODEC;
    }
}
