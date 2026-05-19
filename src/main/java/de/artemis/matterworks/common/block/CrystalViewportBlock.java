package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;

public class CrystalViewportBlock extends AbstractBatteryMultiblockBlock {
    public static final MapCodec<CrystalViewportBlock> CODEC = simpleCodec(CrystalViewportBlock::new);

    public CrystalViewportBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CrystalViewportBlock> codec() {
        return CODEC;
    }
}
