package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;
import de.artemis.matterworks.common.blockentity.PowerCrystalOreBlockEntity;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public class PowerCrystalOreBlock extends BaseEntityBlock {
    public static final MapCodec<PowerCrystalOreBlock> CODEC = simpleCodec(PowerCrystalOreBlock::new);

    public PowerCrystalOreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<PowerCrystalOreBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerCrystalOreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.POWER_CRYSTAL_ORE.get(), PowerCrystalOreBlockEntity::tick);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (player.isCreative()) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }

        if (!level.isClientSide()) {
            level.setBlock(pos, ModBlocks.POWER_CRYSTAL_REVEAL.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof PowerCrystalOreBlockEntity blockEntity) {
                if (!blockEntity.startReveal(player)) {
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        return false;
    }
}
