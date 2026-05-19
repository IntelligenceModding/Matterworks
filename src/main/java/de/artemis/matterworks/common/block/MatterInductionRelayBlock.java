package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;

public class MatterInductionRelayBlock extends AbstractBatteryMultiblockBlock {
    public static final MapCodec<MatterInductionRelayBlock> CODEC = simpleCodec(MatterInductionRelayBlock::new);

    public MatterInductionRelayBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MatterInductionRelayBlock> codec() {
        return CODEC;
    }
}
