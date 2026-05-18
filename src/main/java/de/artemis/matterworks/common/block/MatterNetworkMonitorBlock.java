package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;
import de.artemis.matterworks.common.blockentity.MatterNetworkMonitorBlockEntity;
import de.artemis.matterworks.common.network.SetMatterNetworkTrackingPayload;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class MatterNetworkMonitorBlock extends MatterPylonBlock {
    public static final MapCodec<MatterPylonBlock> CODEC = simpleCodec(MatterNetworkMonitorBlock::new);

    public MatterNetworkMonitorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MatterPylonBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MatterNetworkMonitorBlockEntity monitorBlockEntity) {
                if (player.isShiftKeyDown()) {
                    monitorBlockEntity.handleLinkUse(player);
                } else {
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        PacketDistributor.sendToPlayer(serverPlayer, new SetMatterNetworkTrackingPayload(false, pos, ""));
                    }
                    player.openMenu((MenuProvider) monitorBlockEntity, pos);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatterNetworkMonitorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.MATTER_NETWORK_MONITOR.get(), MatterNetworkMonitorBlockEntity::tick);
    }
}
