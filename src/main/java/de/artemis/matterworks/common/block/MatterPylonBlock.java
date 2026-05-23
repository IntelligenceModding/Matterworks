package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.item.NetworkDataCardItem;
import de.artemis.matterworks.common.network.SetMatterNetworkTrackingPayload;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class MatterPylonBlock extends BaseEntityBlock {
    public static final MapCodec<MatterPylonBlock> CODEC = simpleCodec(MatterPylonBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MatterPylonBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override
    public MapCodec<MatterPylonBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction direction) {
        if (direction != state.getValue(FACING)) {
            return 0;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof MatterPylonBlockEntity pylonBlockEntity ? pylonBlockEntity.getRedstoneOutputSignal() : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (NetworkDataCardItem.isHeldBy(player)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MatterPylonBlockEntity pylonBlockEntity) {
                if (player.isShiftKeyDown()) {
                    pylonBlockEntity.handleLinkUse(player);
                } else {
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        PacketDistributor.sendToPlayer(serverPlayer, new SetMatterNetworkTrackingPayload(false, pos, ""));
                    }
                    pylonBlockEntity.openMatterNetworkMenu(player);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatterPylonBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.MATTER_PYLON.get(), MatterPylonBlockEntity::tick);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MatterPylonBlockEntity pylonBlockEntity) {
                pylonBlockEntity.releaseChunkLoadingTickets();
                pylonBlockEntity.unlinkAll();
                Containers.dropContents(level, pos, pylonBlockEntity.createDropInventory());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
