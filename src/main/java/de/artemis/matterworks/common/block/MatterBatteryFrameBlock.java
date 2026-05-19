package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;

public class MatterBatteryFrameBlock extends AbstractBatteryMultiblockBlock {
    public static final MapCodec<MatterBatteryFrameBlock> CODEC = simpleCodec(MatterBatteryFrameBlock::new);

    public MatterBatteryFrameBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MatterBatteryFrameBlock> codec() {
        return CODEC;
    }
}
