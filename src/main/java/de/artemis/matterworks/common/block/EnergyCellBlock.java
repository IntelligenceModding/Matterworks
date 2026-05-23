package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;
import de.artemis.matterworks.common.blockentity.EnergyCellBlockEntity;
import de.artemis.matterworks.common.item.NetworkDataCardItem;
import de.artemis.matterworks.common.network.SetMatterNetworkTrackingPayload;
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
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class EnergyCellBlock extends HorizontalFacingMachineBlock {
    public static final MapCodec<EnergyCellBlock> CODEC = simpleCodec(EnergyCellBlock::new);

    public EnergyCellBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<EnergyCellBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (NetworkDataCardItem.isHeldBy(player)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EnergyCellBlockEntity energyCellBlockEntity) {
                if (player.isShiftKeyDown()) {
                    energyCellBlockEntity.handleLinkUse(player);
                } else {
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        PacketDistributor.sendToPlayer(serverPlayer, new SetMatterNetworkTrackingPayload(false, pos, ""));
                    }
                    player.openMenu((MenuProvider) energyCellBlockEntity, pos);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCellBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.MATTER_ENERGY_CELL.get(), EnergyCellBlockEntity::tick);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EnergyCellBlockEntity energyCellBlockEntity) {
                energyCellBlockEntity.releaseChunkLoadingTickets();
                energyCellBlockEntity.unlinkAll();
                Containers.dropContents(level, pos, energyCellBlockEntity.createDropInventory());
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
        if (blockEntity instanceof EnergyCellBlockEntity energyCellBlockEntity) {
            int energyStored = energyCellBlockEntity.getEnergyStored();
            int capacity = energyCellBlockEntity.getEnergyCapacity();
            if (energyStored <= 0 || capacity <= 0) {
                return 0;
            }
            return Mth.floor((double) energyStored * 14.0D / capacity) + 1;
        }
        return 0;
    }
}
