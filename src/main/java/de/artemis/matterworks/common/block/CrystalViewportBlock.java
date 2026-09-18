package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CrystalViewportBlock extends AbstractBatteryMultiblockBlock {
    public static final MapCodec<CrystalViewportBlock> CODEC = simpleCodec(CrystalViewportBlock::new);

    public CrystalViewportBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CrystalViewportBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        return adjacentBlockState.is(ModBlocks.MULTIBLOCK_GLASS.get()) || super.skipRendering(state, adjacentBlockState, side);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}
