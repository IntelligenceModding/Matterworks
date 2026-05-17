package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;
import de.artemis.matterworks.common.blockentity.MatterFluidTankBlockEntity;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class MatterFluidTankBlock extends BaseEntityBlock {
    public static final MapCodec<MatterFluidTankBlock> CODEC = simpleCodec(MatterFluidTankBlock::new);

    public MatterFluidTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MatterFluidTankBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MatterFluidTankBlockEntity fluidTankBlockEntity) {
                if (player.isShiftKeyDown()) {
                    fluidTankBlockEntity.handleLinkUse(player);
                } else {
                    player.openMenu((MenuProvider) fluidTankBlockEntity, pos);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatterFluidTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.MATTER_FLUID_TANK.get(), MatterFluidTankBlockEntity::tick);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MatterFluidTankBlockEntity fluidTankBlockEntity) {
                fluidTankBlockEntity.releaseChunkLoadingTickets();
                fluidTankBlockEntity.unlinkAll();
                Containers.dropContents(level, pos, fluidTankBlockEntity.createDropInventory());
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MatterFluidTankBlockEntity fluidTankBlockEntity) {
            int fluidAmount = fluidTankBlockEntity.getFluidAmount();
            int capacity = fluidTankBlockEntity.getFluidCapacity();
            if (fluidAmount <= 0 || capacity <= 0) {
                return 0;
            }
            return Mth.floor((double) fluidAmount * 14.0D / capacity) + 1;
        }
        return 0;
    }
}
