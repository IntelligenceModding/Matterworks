package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class MatterCapacitorCellBlock extends AbstractBatteryMultiblockBlock {
    public static final MapCodec<MatterCapacitorCellBlock> CODEC = simpleCodec(MatterCapacitorCellBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public MatterCapacitorCellBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    public MapCodec<MatterCapacitorCellBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }
}
