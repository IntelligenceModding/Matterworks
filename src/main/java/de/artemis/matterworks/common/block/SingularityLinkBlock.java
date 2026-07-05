package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.blockentity.SingularityLinkBlockEntity;
import de.artemis.matterworks.common.item.NetworkDataCardItem;
import de.artemis.matterworks.common.network.SetMatterNetworkTrackingPayload;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class SingularityLinkBlock extends MatterPylonBlock {
    public static final MapCodec<SingularityLinkBlock> CODEC = simpleCodec(SingularityLinkBlock::new);

    public SingularityLinkBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public MapCodec<MatterPylonBlock> codec() {
        return (MapCodec) CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (NetworkDataCardItem.isHeldBy(player)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SingularityLinkBlockEntity linkBlockEntity) {
                if (player.isShiftKeyDown()) {
                    linkBlockEntity.handleLinkUse(player);
                } else {
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        PacketDistributor.sendToPlayer(serverPlayer, new SetMatterNetworkTrackingPayload(false, pos, ""));
                    }
                    linkBlockEntity.openMatterNetworkMenu(player);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SingularityLinkBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.SINGULARITY_LINK.get(), SingularityLinkBlockEntity::tick);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SingularityLinkBlockEntity linkBlockEntity) {
                linkBlockEntity.onBridgeRemoved();
                linkBlockEntity.releaseChunkLoadingTickets();
                linkBlockEntity.unlinkAll();
                Containers.dropContents(level, pos, linkBlockEntity.createDropInventory());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
